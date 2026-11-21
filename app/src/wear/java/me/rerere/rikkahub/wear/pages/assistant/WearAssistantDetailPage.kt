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
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.datastore.findModelById
import me.rerere.rikkahub.ui.context.LocalSettings
import me.rerere.rikkahub.ui.context.LocalToaster
import me.rerere.rikkahub.ui.pages.assistant.detail.AssistantDetailVM
import me.rerere.rikkahub.wear.WearDestination
import me.rerere.rikkahub.wear.WearIcons
import me.rerere.rikkahub.wear.components.WearInputDialog
import me.rerere.rikkahub.wear.components.WearOptionDialog
import me.rerere.rikkahub.wear.components.WearScreen
import me.rerere.rikkahub.wear.components.navItem
import me.rerere.rikkahub.wear.components.section
import me.rerere.rikkahub.wear.components.textItem
import me.rerere.rikkahub.wear.components.toggleItem
import me.rerere.ui.toast.ToastType
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

/**
 * 单个助手的配置入口：名字、模型、提示词、记忆、MCP、本地工具与注入。
 *
 * 手机版把基础信息拆在 AssistantBasicPage，手表没有单独目的地，这里直接放最常用的几项。
 */
@Composable
fun WearAssistantDetailPage(
    assistantId: kotlin.uuid.Uuid,
    onBack: () -> Unit,
    onNavigate: (me.rerere.rikkahub.wear.WearDestination) -> Unit,
) {
    val settings = LocalSettings.current
    val store: SettingsStore = koinInject()
    val toaster = LocalToaster.current
    val scope = rememberCoroutineScope()
    // 手机上这一页由 AssistantDetailVM 承载更新；手表复用同一份 update 逻辑
    val vm: AssistantDetailVM = koinViewModel(parameters = { parametersOf(assistantId.toString()) })
    val assistant = settings.assistants.find { it.id == assistantId }
    val id = assistantId.toString()

    var editName by remember { mutableStateOf(false) }
    var showModelPicker by remember { mutableStateOf(false) }

    // WearScreen 的 content 是普通 DSL builder（非 @Composable），字符串要先在外部取好。
    val assistantFallback = stringResource(R.string.wear_asst_assistant)
    val missingAssistant = stringResource(R.string.wear_asst_assistant_missing)

    if (assistant == null) {
        WearScreen(title = assistantFallback, onBack = onBack) {
            textItem(missingAssistant)
        }
        return
    }

    val activeModel = settings.findModelById(assistant.chatModelId ?: settings.chatModelId)
    val allModels = remember(settings.providers) {
        settings.providers
            .filter { provider -> provider.enabled }
            .flatMap { provider -> provider.models.map { model -> provider.name to model } }
    }
    val globalModel = settings.findModelById(settings.chatModelId)

    val notConfiguredLabel = stringResource(R.string.wear_asst_not_configured)
    val memoryDisabledLabel = stringResource(R.string.wear_asst_disabled)
    val memoryGlobalLabel = stringResource(R.string.wear_asst_memory_enabled_global)
    val memoryAssistantLabel = stringResource(R.string.wear_asst_memory_enabled_assistant)
    val useForNewChatsLabel = stringResource(R.string.wear_asst_use_for_new_chats)
    val useForNewChatsDesc = stringResource(R.string.wear_asst_use_for_new_chats_desc)
    val configurationLabel = stringResource(R.string.wear_asst_configuration)
    val nameLabel = stringResource(R.string.wear_asst_name)
    val unnamedAssistantLabel = stringResource(R.string.wear_asst_unnamed_assistant)
    val chatModelLabel = stringResource(R.string.wear_asst_chat_model)
    val systemPromptLabel = stringResource(R.string.wear_asst_system_prompt)
    val emptyLabel = stringResource(R.string.wear_asst_empty)
    val capabilitiesLabel = stringResource(R.string.wear_asst_capabilities)
    val memoryLabel = stringResource(R.string.wear_asst_memory)
    val mcpServersLabel = stringResource(R.string.wear_asst_mcp_servers)
    val mcpAssignedLabel = stringResource(R.string.wear_asst_assigned_count, assistant.mcpServers.size)
    val localToolsLabel = stringResource(R.string.wear_asst_local_tools)
    val localToolsEnabledLabel =
        stringResource(R.string.wear_asst_enabled_count, assistant.localTools.size)
    val promptInjectionsLabel = stringResource(R.string.wear_asst_prompt_injections)
    val injectionsSummaryLabel = stringResource(
        R.string.wear_asst_injections_summary,
        assistant.modeInjectionIds.size,
        assistant.lorebookIds.size,
    )
    val systemPromptPreviewLabel = stringResource(R.string.wear_asst_system_prompt_preview)
    val nameSavedMessage = stringResource(R.string.wear_asst_name_saved)
    val usingGlobalDefaultMessage = stringResource(R.string.wear_asst_using_global_default)

    val modelLabel = when {
        assistant.chatModelId == null && globalModel != null ->
            stringResource(
                R.string.wear_asst_model_global,
                globalModel.displayName.ifBlank { globalModel.modelId },
            )

        activeModel != null -> activeModel.displayName.ifBlank { activeModel.modelId }
        else -> notConfiguredLabel
    }
    val memoryStateLabel = when {
        !assistant.enableMemory -> memoryDisabledLabel
        assistant.useGlobalMemory -> memoryGlobalLabel
        else -> memoryAssistantLabel
    }

    WearScreen(title = assistant.name.ifBlank { assistantFallback }, onBack = onBack) {
        toggleItem(
            title = useForNewChatsLabel,
            subtitle = useForNewChatsDesc,
            checked = assistant.id == settings.assistantId,
            enabled = assistant.id != settings.assistantId,
            onCheckedChange = { active ->
                if (active) scope.launch { store.selectAssistant(assistant.id) }
            },
        )

        section(configurationLabel)
        navItem(
            title = nameLabel,
            subtitle = assistant.name.ifBlank { unnamedAssistantLabel },
            icon = WearIcons.Settings,
            onClick = { editName = true },
        )
        navItem(
            title = chatModelLabel,
            subtitle = modelLabel,
            icon = WearIcons.Cpu,
            onClick = { showModelPicker = true },
        )
        navItem(
            title = systemPromptLabel,
            subtitle = assistant.systemPrompt
                .replace('\n', ' ')
                .trim()
                .ifBlank { emptyLabel }
                .take(60),
            icon = WearIcons.Messages,
            onClick = { onNavigate(WearDestination.AssistantPrompt(id)) },
        )

        section(capabilitiesLabel)
        navItem(
            title = memoryLabel,
            subtitle = memoryStateLabel,
            icon = WearIcons.Brain,
            onClick = { onNavigate(WearDestination.AssistantMemory(id)) },
        )
        navItem(
            title = mcpServersLabel,
            subtitle = mcpAssignedLabel,
            icon = WearIcons.Mcp,
            onClick = { onNavigate(WearDestination.AssistantMcp(id)) },
        )
        navItem(
            title = localToolsLabel,
            subtitle = localToolsEnabledLabel,
            icon = WearIcons.Robot,
            onClick = { onNavigate(WearDestination.AssistantTools(id)) },
        )
        navItem(
            title = promptInjectionsLabel,
            subtitle = injectionsSummaryLabel,
            icon = WearIcons.Ai,
            onClick = { onNavigate(WearDestination.AssistantInjections(id)) },
        )

        if (assistant.systemPrompt.isNotBlank()) {
            section(systemPromptPreviewLabel)
            textItem(
                text = assistant.systemPrompt,
                maxLines = 6,
                small = true,
                muted = true,
            )
        }
    }

    if (editName) {
        WearInputDialog(
            visible = true,
            title = stringResource(R.string.wear_asst_assistant_name),
            initialValue = assistant.name,
            label = nameLabel,
            placeholder = assistantFallback,
            confirmLabel = stringResource(R.string.wear_common_ok),
            singleLine = true,
            onConfirm = { value ->
                editName = false
                scope.launch {
                    vm.update(assistant.copy(name = value.trim()))
                    toaster.show(nameSavedMessage, ToastType.Success)
                }
            },
            onDismiss = { editName = false },
        )
    }

    if (showModelPicker) {
        val selectedIndex = if (assistant.chatModelId == null) {
            0
        } else {
            allModels.indexOfFirst { (_, model) -> model.id == assistant.chatModelId } + 1
        }
        val useGlobalDefaultLabel = stringResource(R.string.wear_asst_use_global_default)
        // 回调不是 @Composable，先把每个模型的提示文本取好
        val modelSetMessages = mutableListOf<String>()
        for ((_, model) in allModels) {
            modelSetMessages += stringResource(
                R.string.wear_asst_model_set_to,
                model.displayName.ifBlank { model.modelId },
            )
        }
        WearOptionDialog(
            visible = true,
            title = chatModelLabel,
            options = listOf(useGlobalDefaultLabel) + allModels.map { (provider, model) ->
                "${model.displayName.ifBlank { model.modelId }} · $provider"
            },
            selectedIndex = selectedIndex.coerceAtLeast(0),
            onSelect = { index ->
                showModelPicker = false
                if (index == 0) {
                    scope.launch {
                        vm.update(assistant.copy(chatModelId = null))
                        toaster.show(usingGlobalDefaultMessage, ToastType.Success)
                    }
                } else {
                    val model = allModels.getOrNull(index - 1)?.second
                    if (model != null) {
                        scope.launch {
                            vm.update(assistant.copy(chatModelId = model.id))
                            toaster.show(
                                modelSetMessages.getOrElse(index - 1) { "" },
                                ToastType.Success,
                            )
                        }
                    }
                }
            },
            onDismiss = { showModelPicker = false },
        )
    }
}
