package me.rerere.rikkahub.wear.pages.assistant

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import me.rerere.rikkahub.R
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

    // WearScreen 的 content 是普通 DSL builder（非 @Composable），字符串要先在外部取好。
    val systemPromptLabel = stringResource(R.string.wear_asst_system_prompt)
    val missingAssistant = stringResource(R.string.wear_asst_assistant_missing)
    val promptPlaceholder = stringResource(R.string.wear_asst_system_prompt_placeholder)
    val savePromptLabel = stringResource(R.string.wear_asst_save_prompt)
    val systemPromptSavedMessage = stringResource(R.string.wear_asst_system_prompt_saved)
    val discardChangesLabel = stringResource(R.string.wear_asst_discard_changes)
    val charactersLabel = stringResource(R.string.wear_asst_characters)
    val behaviorLabel = stringResource(R.string.wear_asst_behavior)
    val perConversationPromptLabel = stringResource(R.string.wear_asst_per_conversation_prompt)
    val perConversationPromptDesc =
        stringResource(R.string.wear_asst_per_conversation_prompt_desc)
    val streamOutputLabel = stringResource(R.string.wear_asst_stream_output)
    val streamOutputDesc = stringResource(R.string.wear_asst_stream_output_desc)
    val messageTemplateLabel = stringResource(R.string.wear_asst_message_template)
    val templateLabel = stringResource(R.string.wear_asst_template)
    val templateVariables = stringResource(R.string.wear_asst_template_variables)
    val templateMissingMessage = stringResource(R.string.wear_asst_template_missing_message)
    val templateSavedMessage = stringResource(R.string.wear_asst_template_saved)

    if (assistant == null) {
        WearScreen(title = systemPromptLabel, onBack = onBack) {
            textItem(missingAssistant)
        }
        return
    }

    // 以存储里的提示词为基准生成草稿：外部改动会重置草稿，本页保存后两边一致
    var draft by remember(assistant.systemPrompt) { mutableStateOf(assistant.systemPrompt) }
    val dirty = draft != assistant.systemPrompt

    WearScreen(title = systemPromptLabel, onBack = onBack) {
        item(key = "system-prompt-editor") {
            WearTextField(
                value = draft,
                onValueChange = { draft = it },
                label = systemPromptLabel,
                placeholder = promptPlaceholder,
                singleLine = false,
                minLines = 6,
            )
        }
        buttonItem(
            label = savePromptLabel,
            icon = WearIcons.Check,
            enabled = dirty,
            onClick = {
                val value = draft
                scope.launch {
                    vm.update(assistant.copy(systemPrompt = value))
                    toaster.show(systemPromptSavedMessage, ToastType.Success)
                }
            },
        )
        if (dirty) {
            buttonItem(label = discardChangesLabel, tonal = true) {
                draft = assistant.systemPrompt
            }
        }
        infoItem(charactersLabel, draft.length.toString())

        section(behaviorLabel)
        toggleItem(
            title = perConversationPromptLabel,
            subtitle = perConversationPromptDesc,
            checked = assistant.allowConversationSystemPrompt,
            onCheckedChange = { checked ->
                scope.launch { vm.update(assistant.copy(allowConversationSystemPrompt = checked)) }
            },
        )
        toggleItem(
            title = streamOutputLabel,
            subtitle = streamOutputDesc,
            checked = assistant.streamOutput,
            onCheckedChange = { checked ->
                scope.launch { vm.update(assistant.copy(streamOutput = checked)) }
            },
        )

        section(messageTemplateLabel)
        navItem(
            title = templateLabel,
            subtitle = assistant.messageTemplate.replace('\n', ' ').take(60),
            icon = WearIcons.Settings,
            onClick = { editTemplate = true },
        )
        textItem(
            templateVariables,
            small = true,
            muted = true,
        )
        if ("{{ message }}" !in assistant.messageTemplate) {
            textItem(
                templateMissingMessage,
                small = true,
            )
        }
    }

    if (editTemplate) {
        WearInputDialog(
            visible = true,
            title = messageTemplateLabel,
            initialValue = assistant.messageTemplate,
            label = templateLabel,
            confirmLabel = stringResource(R.string.wear_common_ok),
            singleLine = false,
            onConfirm = { value ->
                editTemplate = false
                if (value.isNotBlank()) {
                    scope.launch {
                        vm.update(assistant.copy(messageTemplate = value))
                        toaster.show(templateSavedMessage, ToastType.Success)
                    }
                }
            },
            onDismiss = { editTemplate = false },
        )
    }
}
