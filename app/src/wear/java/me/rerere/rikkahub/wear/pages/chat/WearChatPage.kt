package me.rerere.rikkahub.wear.pages.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScrollIndicator
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import kotlin.uuid.Uuid
import kotlinx.coroutines.launch
import me.rerere.ai.core.MessageRole
import me.rerere.ai.ui.UIMessage
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.model.getAssistantOf
import me.rerere.rikkahub.data.model.getChatModelOf
import me.rerere.rikkahub.ui.context.LocalASRState
import me.rerere.rikkahub.ui.context.LocalSettings
import me.rerere.rikkahub.ui.context.LocalTTSState
import me.rerere.rikkahub.ui.context.LocalToaster
import me.rerere.rikkahub.ui.pages.chat.ChatVM
import me.rerere.rikkahub.wear.WearIcons
import me.rerere.rikkahub.wear.components.WearList
import me.rerere.rikkahub.wear.components.WearListScope
import me.rerere.rikkahub.wear.components.WearOptionDialog
import me.rerere.rikkahub.wear.components.WearScreen
import me.rerere.rikkahub.wear.components.WearTextField
import me.rerere.rikkahub.wear.components.buttonItem
import me.rerere.rikkahub.wear.components.rememberWearListState
import me.rerere.rikkahub.wear.components.section
import me.rerere.rikkahub.wear.shape.LocalScreenShapeState
import me.rerere.rikkahub.wear.shape.ScreenShape
import me.rerere.ui.toast.ToastType
import org.koin.androidx.compose.koinViewModel

@Composable
fun WearChatPage(
    conversationId: Uuid,
    onBack: () -> Unit,
    onForked: (Uuid) -> Unit,
    onNewChat: () -> Unit,
) {
    val vm: ChatVM = koinViewModel(
        parameters = { org.koin.core.parameter.parametersOf(conversationId.toString()) },
    )
    val conversation by vm.conversation.collectAsStateWithLifecycle()
    val loadingJob by vm.conversationJob.collectAsStateWithLifecycle()
    val errors by vm.errors.collectAsStateWithLifecycle()
    val processingStatus by vm.processingStatus.collectAsStateWithLifecycle()
    val settings = LocalSettings.current
    val assistant = remember(settings, conversation) { settings.getAssistantOf(conversation) }
    val chatModel = remember(settings, conversation) { settings.getChatModelOf(conversation) }
    val shapeState = LocalScreenShapeState.current
    val scope = rememberCoroutineScope()
    val toaster = LocalToaster.current
    val tts = LocalTTSState.current
    val asrState = LocalASRState.current
    val asr by asrState.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val defaultChatTitle = stringResource(R.string.wear_chat_title)
    val noModelText = stringResource(R.string.wear_chat_no_model)
    val listeningText = stringResource(R.string.wear_chat_listening)
    val generationFailedText = stringResource(R.string.wear_chat_generation_failed)
    val selectModelFirstText = stringResource(R.string.wear_chat_select_model_first)
    val copiedText = stringResource(R.string.wear_chat_copied)

    var showInput by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    var listening by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var modelPickerOpen by remember { mutableStateOf(false) }
    var actionMessage by remember { mutableStateOf<UIMessage?>(null) }

    val nodes = conversation.messageNodes
    val listState = rememberWearListState()
    val lastText = nodes.lastOrNull()?.currentMessage?.toText().orEmpty()

    LaunchedEffect(nodes.size, lastText.length, loadingJob != null) {
        val itemCount = nodes.size + if (loadingJob != null) 1 else 0
        if (itemCount > 0) {
            runCatching { listState.scrollToBottom(itemCount - 1) }
        }
    }
    LaunchedEffect(errors.size) {
        errors.lastOrNull()?.let { error ->
            toaster.show(
                message = error.title ?: error.error.message ?: generationFailedText,
                type = ToastType.Error,
            )
        }
    }

    fun send(text: String) {
        if (text.isBlank()) return
        if (chatModel == null) {
            toaster.show(selectModelFirstText, ToastType.Error)
            showInput = false
            modelPickerOpen = true
            return
        }
        vm.inputState.setMessageText(text)
        val editingMessageId = vm.inputState.editingMessage
        if (editingMessageId != null) {
            vm.handleMessageEdit(parts = vm.inputState.getContents(), messageId = editingMessageId)
        } else {
            vm.handleMessageSend(vm.inputState.getContents())
        }
        vm.inputState.clearInput()
        inputText = ""
        showInput = false
    }

    if (showInput) {
        WearChatInputPage(
            initialText = inputText,
            onTextChange = { inputText = it },
            onBack = { showInput = false },
            onSend = { send(it) },
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ChatTopBar(
            title = conversation.title.ifBlank { assistant.name.ifBlank { defaultChatTitle } },
            subtitle = chatModel?.displayName?.ifBlank { chatModel.modelId } ?: noModelText,
            isRound = shapeState.shape == ScreenShape.ROUND,
            listening = listening,
            onBack = onBack,
            onVoice = {
                if (listening) {
                    asrState.stop()
                    listening = false
                    showInput = true
                } else {
                    inputText = ""
                    listening = true
                    asrState.start { transcript -> inputText = transcript }
                }
            },
            onMenu = { menuOpen = true },
        )
        Box(modifier = Modifier.weight(1f)) {
            if (shapeState.shape == ScreenShape.ROUND) {
                ScreenScaffold(
                    scrollState = listState.scalingState,
                    edgeButton = {},
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                ) { padding ->
                    ChatMessageList(
                        nodes = nodes,
                        loading = loadingJob != null,
                        streamingIndex = if (loadingJob != null) nodes.lastIndex else -1,
                        processingStatus = processingStatus,
                        listState = listState,
                        contentPadding = padding,
                        onAction = { actionMessage = it },
                        onBranch = { nodeId, index -> vm.selectMessageNode(nodeId, index) },
                        onToolApproval = { id, approved -> vm.handleToolApproval(id, approved) },
                    )
                }
            } else {
                ChatMessageList(
                    nodes = nodes,
                    loading = loadingJob != null,
                    streamingIndex = if (loadingJob != null) nodes.lastIndex else -1,
                    processingStatus = processingStatus,
                    listState = listState,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    onAction = { actionMessage = it },
                    onBranch = { nodeId, index -> vm.selectMessageNode(nodeId, index) },
                    onToolApproval = { id, approved -> vm.handleToolApproval(id, approved) },
                )
                ScrollIndicator(
                    state = listState.lazyState,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 2.dp),
                )
            }
        }
        ChatBottomBar(
            loading = loadingJob != null,
            listening = listening,
            isRound = shapeState.shape == ScreenShape.ROUND,
            onReply = { showInput = true },
            onStop = {
                if (listening) {
                    asrState.stop()
                    listening = false
                } else {
                    vm.stopGeneration()
                }
            },
            onVoice = {
                if (listening) {
                    asrState.stop()
                    listening = false
                    showInput = true
                } else {
                    inputText = ""
                    listening = true
                    asrState.start { transcript -> inputText = transcript }
                }
            },
            onRegenerate = {
                nodes.lastOrNull { node ->
                    node.currentMessage.role == MessageRole.ASSISTANT
                }?.currentMessage?.let(vm::regenerateAtMessage)
            },
        )
        if (listening) {
            Text(
                text = asr.transcript.ifBlank { listeningText },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
            )
        }
    }

    if (menuOpen) {
        WearOptionDialog(
            visible = true,
            title = conversation.title.ifBlank { defaultChatTitle },
            options = listOf(
                stringResource(R.string.wear_chat_choose_model),
                stringResource(R.string.wear_chat_new_chat),
                stringResource(R.string.wear_chat_regenerate),
                stringResource(R.string.wear_chat_delete_conversation),
            ),
            onSelect = { index ->
                menuOpen = false
                when (index) {
                    0 -> modelPickerOpen = true
                    1 -> onNewChat()
                    2 -> nodes.lastOrNull { node ->
                        node.currentMessage.role == MessageRole.ASSISTANT
                    }?.currentMessage?.let(vm::regenerateAtMessage)
                    else -> {
                        scope.launch {
                            vm.deleteConversation(conversation).join()
                            onBack()
                        }
                    }
                }
            },
            onDismiss = { menuOpen = false },
        )
    }

    if (modelPickerOpen) {
        val allModels = settings.providers.filter { it.enabled }.flatMap { provider ->
            provider.models.map { provider.name to it }
        }
        WearOptionDialog(
            visible = true,
            title = stringResource(R.string.wear_chat_choose_model),
            options = allModels.map { (provider, model) ->
                "${model.displayName.ifBlank { model.modelId }} · $provider"
            },
            selectedIndex = allModels.indexOfFirst { it.second.id == chatModel?.id },
            onSelect = { index ->
                modelPickerOpen = false
                allModels.getOrNull(index)?.second?.let(vm::setChatModel)
            },
            onDismiss = { modelPickerOpen = false },
        )
    }

    actionMessage?.let { message ->
        WearOptionDialog(
            visible = true,
            title = stringResource(R.string.wear_chat_message),
            options = listOf(
                stringResource(R.string.wear_chat_copy),
                stringResource(R.string.wear_chat_speak),
                stringResource(R.string.wear_chat_regenerate),
                stringResource(R.string.wear_chat_fork),
                stringResource(R.string.wear_chat_edit),
                stringResource(R.string.wear_chat_delete),
            ),
            onSelect = { index ->
                actionMessage = null
                when (index) {
                    0 -> {
                        copyToClipboard(context, message.toText())
                        toaster.show(copiedText, ToastType.Success)
                    }
                    1 -> tts.speak(message.toText())
                    2 -> vm.regenerateAtMessage(message)
                    3 -> scope.launch { onForked(vm.forkMessage(message).id) }
                    4 -> {
                        vm.inputState.editingMessage = message.id
                        vm.inputState.setMessageText(message.toText())
                        inputText = message.toText()
                        showInput = true
                    }
                    else -> vm.deleteMessage(message)
                }
            },
            onDismiss = { actionMessage = null },
        )
    }
}

@Composable
private fun ChatMessageList(
    nodes: List<me.rerere.rikkahub.data.model.MessageNode>,
    loading: Boolean,
    streamingIndex: Int,
    processingStatus: String?,
    listState: me.rerere.rikkahub.wear.components.WearListState,
    contentPadding: PaddingValues,
    onAction: (UIMessage) -> Unit,
    onBranch: (nodeId: Uuid, index: Int) -> Unit,
    onToolApproval: (toolCallId: String, approved: Boolean) -> Unit,
) {
    WearList(state = listState, contentPadding = contentPadding) {
        if (nodes.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = stringResource(R.string.wear_chat_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(nodes.size, key = { nodes[it].id }) { index ->
            WearChatMessage(
                node = nodes[index],
                streaming = loading && index == streamingIndex,
                onAction = onAction,
                onBranch = { selectIndex -> onBranch(nodes[index].id, selectIndex) },
                onToolApproval = onToolApproval,
            )
        }
        if (loading) {
            item(key = "status") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = processingStatus ?: stringResource(R.string.wear_chat_generating),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatTopBar(
    title: String,
    subtitle: String,
    isRound: Boolean,
    listening: Boolean,
    onBack: () -> Unit,
    onVoice: () -> Unit,
    onMenu: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isRound) 22.dp else 2.dp,
                end = if (isRound) 22.dp else 2.dp,
                top = if (isRound) 10.dp else 4.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            modifier = if (isRound) Modifier.size(38.dp) else Modifier,
        ) {
            Icon(
                imageVector = WearIcons.Back,
                contentDescription = stringResource(R.string.wear_chat_back),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = if (isRound) Modifier.size(18.dp) else Modifier,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (isRound) {
            // 圆屏底部放不下三个按钮：把语音入口挪到标题栏，底部只留主操作
            IconButton(
                onClick = onVoice,
                modifier = Modifier.size(38.dp),
            ) {
                Icon(
                    imageVector = if (listening) WearIcons.Check else WearIcons.Mic,
                    contentDescription = if (listening) {
                        stringResource(R.string.wear_chat_finish_voice_input)
                    } else {
                        stringResource(R.string.wear_chat_voice_input)
                    },
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        IconButton(
            onClick = onMenu,
            modifier = if (isRound) Modifier.size(38.dp) else Modifier,
        ) {
            Icon(
                imageVector = WearIcons.More,
                contentDescription = stringResource(R.string.wear_chat_menu),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = if (isRound) Modifier.size(18.dp) else Modifier,
            )
        }
    }
}

@Composable
private fun ChatBottomBar(
    loading: Boolean,
    listening: Boolean,
    isRound: Boolean,
    onReply: () -> Unit,
    onStop: () -> Unit,
    onVoice: () -> Unit,
    onRegenerate: () -> Unit,
) {
    if (isRound) {
        // 圆屏底部弦宽有限：只保留一个居中的主按钮（回复/停止），语音在顶部，重生成在菜单
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Button(
                onClick = if (loading) onStop else onReply,
                modifier = Modifier
                    .width(120.dp)
                    .height(40.dp),
                contentPadding = PaddingValues(0.dp),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (loading) {
                            stringResource(R.string.wear_chat_stop)
                        } else {
                            stringResource(R.string.wear_chat_reply)
                        },
                    )
                }
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = onReply,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 6.dp),
            ) {
                Text(stringResource(R.string.wear_chat_reply))
            }
            FilledIconButton(onClick = onVoice, modifier = Modifier.size(46.dp)) {
                Icon(
                    imageVector = if (listening) WearIcons.Check else WearIcons.Mic,
                    contentDescription = if (listening) {
                        stringResource(R.string.wear_chat_finish_voice_input)
                    } else {
                        stringResource(R.string.wear_chat_voice_input)
                    },
                )
            }
            FilledIconButton(
                onClick = if (loading) onStop else onRegenerate,
                modifier = Modifier.size(46.dp),
            ) {
                Icon(
                    imageVector = if (loading) WearIcons.Stop else WearIcons.Retry,
                    contentDescription = if (loading) {
                        stringResource(R.string.wear_chat_stop)
                    } else {
                        stringResource(R.string.wear_chat_regenerate)
                    },
                )
            }
        }
    }
}

@Composable
private fun WearChatInputPage(
    initialText: String,
    onTextChange: (String) -> Unit,
    onBack: () -> Unit,
    onSend: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initialText) }
    val settings = LocalSettings.current
    // WearScreen 的 content 是普通 DSL builder（非 @Composable），字符串需在进入 DSL 前取好
    val inputTitle = stringResource(R.string.wear_chat_message)
    val inputPlaceholder = stringResource(R.string.wear_chat_input_placeholder)
    val sendLabel = stringResource(R.string.wear_chat_send)
    val quickMessagesLabel = stringResource(R.string.wear_chat_quick_messages)

    WearScreen(title = inputTitle, onBack = onBack) {
        item(key = "input") {
            WearTextField(
                value = text,
                onValueChange = {
                    text = it
                    onTextChange(it)
                },
                singleLine = false,
                minLines = 3,
                autoFocus = true,
                placeholder = inputPlaceholder,
            )
        }
        buttonItem(label = sendLabel, icon = WearIcons.Play) { onSend(text) }
        if (settings.quickMessages.isNotEmpty()) {
            section(quickMessagesLabel)
            settings.quickMessages.take(6).forEach { quickMessage ->
                buttonItem(
                    label = quickMessage.title.ifBlank { quickMessage.content.take(20) },
                    tonal = true,
                ) {
                    text = quickMessage.content
                    onTextChange(quickMessage.content)
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    manager.setPrimaryClip(ClipData.newPlainText("message", text))
}
