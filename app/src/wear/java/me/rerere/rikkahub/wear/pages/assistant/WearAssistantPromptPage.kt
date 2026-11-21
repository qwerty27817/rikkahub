package me.rerere.rikkahub.wear.pages.assistant

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import me.rerere.rikkahub.ui.context.LocalSettings
import me.rerere.rikkahub.ui.context.LocalToaster
import me.rerere.rikkahub.ui.pages.assistant.detail.AssistantDetailVM
import me.rerere.rikkahub.wear.WearIcons
import me.rerere.rikkahub.wear.components.WearInputDialog
import me.rerere.rikkahub.wear.components.WearScreen
import me.rerere.rikkahub.wear.components.WearTextField
import me.rerere.rikkahub.wear.components.buttonItem
import me.rerere.rikkahub.wear.components.infoItem
import me.rerere.rikkahub.wear.components.navItem
import me.rerere.rikkahub.wear.components.section
import me.rerere.rikkahub.wear.components.textItem
import me.rerere.rikkahub.wear.components.toggleItem
import me.rerere.ui.toast.ToastType
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * 助手的系统提示词与消息模板。
 *
 * 手机版还有 presetMessages / regex 编辑器，手表上以文本编辑为主，其余项只读展示。
 */
@Composable
fun WearAssistantPromptPage(
    assistantId: kotlin.uuid.Uuid,
    onBack: () -> Unit,
) {
    val settings = LocalSettings.current
    val toaster = LocalToaster.current
    val scope = rememberCoroutineScope()
    val vm: AssistantDetailVM = koinViewModel(parameters = { parametersOf(assistantId.toString()) })
    val assistant = settings.assistants.find { it.id == assistantId }
    var editTemplate by remember { mutableStateOf(false) }

    if (assistant == null) {
        WearScreen(title = "System prompt", onBack = onBack) {
            textItem("This assistant no longer exists.")
        }
        return
    }

    // 以存储里的提示词为基准生成草稿：外部改动会重置草稿，本页保存后两边一致
    var draft by remember(assistant.systemPrompt) { mutableStateOf(assistant.systemPrompt) }
    val dirty = draft != assistant.systemPrompt

    WearScreen(title = "System prompt", onBack = onBack) {
        item(key = "system-prompt-editor") {
            WearTextField(
                value = draft,
                onValueChange = { draft = it },
                label = "System prompt",
                placeholder = "You are a helpful assistant.",
                singleLine = false,
                minLines = 6,
            )
        }
        buttonItem(
            label = "Save prompt",
            icon = WearIcons.Check,
            enabled = dirty,
            onClick = {
                val value = draft
                scope.launch {
                    vm.update(assistant.copy(systemPrompt = value))
                    toaster.show("System prompt saved", ToastType.Success)
                }
            },
        )
        if (dirty) {
            buttonItem(label = "Discard changes", tonal = true) {
                draft = assistant.systemPrompt
            }
        }
        infoItem("Characters", draft.length.toString())

        section("Behavior")
        toggleItem(
            title = "Per-conversation prompt",
            subtitle = "Let a conversation override the system prompt",
            checked = assistant.allowConversationSystemPrompt,
            onCheckedChange = { checked ->
                scope.launch { vm.update(assistant.copy(allowConversationSystemPrompt = checked)) }
            },
        )
        toggleItem(
            title = "Stream output",
            subtitle = "Show the reply while it is generated",
            checked = assistant.streamOutput,
            onCheckedChange = { checked ->
                scope.launch { vm.update(assistant.copy(streamOutput = checked)) }
            },
        )

        section("Message template")
        navItem(
            title = "Template",
            subtitle = assistant.messageTemplate.replace('\n', ' ').take(60),
            icon = WearIcons.Settings,
            onClick = { editTemplate = true },
        )
        textItem(
            "Variables: {{ role }}, {{ message }}, {{ time }}, {{ date }}.",
            small = true,
            muted = true,
        )
        if ("{{ message }}" !in assistant.messageTemplate) {
            textItem(
                "The template must contain {{ message }}.",
                small = true,
            )
        }
    }

    if (editTemplate) {
        WearInputDialog(
            visible = true,
            title = "Message template",
            initialValue = assistant.messageTemplate,
            label = "Template",
            singleLine = false,
            onConfirm = { value ->
                editTemplate = false
                if (value.isNotBlank()) {
                    scope.launch {
                        vm.update(assistant.copy(messageTemplate = value))
                        toaster.show("Template saved", ToastType.Success)
                    }
                }
            },
            onDismiss = { editTemplate = false },
        )
    }
}
