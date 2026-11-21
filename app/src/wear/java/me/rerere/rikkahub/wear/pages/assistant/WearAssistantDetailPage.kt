package me.rerere.rikkahub.wear.pages.assistant

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
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

    if (assistant == null) {
        WearScreen(title = "Assistant", onBack = onBack) {
            textItem("This assistant no longer exists.")
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
    val modelLabel = when {
        assistant.chatModelId == null && globalModel != null ->
            "${globalModel.displayName.ifBlank { globalModel.modelId }} (global)"

        activeModel != null -> activeModel.displayName.ifBlank { activeModel.modelId }
        else -> "Not configured"
    }
    val memoryLabel = when {
        !assistant.enableMemory -> "Disabled"
        assistant.useGlobalMemory -> "Enabled · global memory"
        else -> "Enabled · per-assistant memory"
    }

    WearScreen(title = assistant.name.ifBlank { "Assistant" }, onBack = onBack) {
        toggleItem(
            title = "Use for new chats",
            subtitle = "Selects this assistant as the current one",
            checked = assistant.id == settings.assistantId,
            enabled = assistant.id != settings.assistantId,
            onCheckedChange = { active ->
                if (active) scope.launch { store.selectAssistant(assistant.id) }
            },
        )

        section("Configuration")
        navItem(
            title = "Name",
            subtitle = assistant.name.ifBlank { "Unnamed assistant" },
            icon = WearIcons.Settings,
            onClick = { editName = true },
        )
        navItem(
            title = "Chat model",
            subtitle = modelLabel,
            icon = WearIcons.Cpu,
            onClick = { showModelPicker = true },
        )
        navItem(
            title = "System prompt",
            subtitle = assistant.systemPrompt
                .replace('\n', ' ')
                .trim()
                .ifBlank { "Empty" }
                .take(60),
            icon = WearIcons.Messages,
            onClick = { onNavigate(WearDestination.AssistantPrompt(id)) },
        )

        section("Capabilities")
        navItem(
            title = "Memory",
            subtitle = memoryLabel,
            icon = WearIcons.Brain,
            onClick = { onNavigate(WearDestination.AssistantMemory(id)) },
        )
        navItem(
            title = "MCP servers",
            subtitle = "${assistant.mcpServers.size} assigned",
            icon = WearIcons.Mcp,
            onClick = { onNavigate(WearDestination.AssistantMcp(id)) },
        )
        navItem(
            title = "Local tools",
            subtitle = "${assistant.localTools.size} enabled",
            icon = WearIcons.Robot,
            onClick = { onNavigate(WearDestination.AssistantTools(id)) },
        )
        navItem(
            title = "Prompt injections",
            subtitle = "${assistant.modeInjectionIds.size} modes · ${assistant.lorebookIds.size} lorebooks",
            icon = WearIcons.Ai,
            onClick = { onNavigate(WearDestination.AssistantInjections(id)) },
        )

        if (assistant.systemPrompt.isNotBlank()) {
            section("System prompt preview")
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
            title = "Assistant name",
            initialValue = assistant.name,
            label = "Name",
            placeholder = "Assistant",
            singleLine = true,
            onConfirm = { value ->
                editName = false
                scope.launch {
                    vm.update(assistant.copy(name = value.trim()))
                    toaster.show("Name saved", ToastType.Success)
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
        WearOptionDialog(
            visible = true,
            title = "Chat model",
            options = listOf("Use global default") + allModels.map { (provider, model) ->
                "${model.displayName.ifBlank { model.modelId }} · $provider"
            },
            selectedIndex = selectedIndex.coerceAtLeast(0),
            onSelect = { index ->
                showModelPicker = false
                if (index == 0) {
                    scope.launch {
                        vm.update(assistant.copy(chatModelId = null))
                        toaster.show("Using global default model", ToastType.Success)
                    }
                } else {
                    val model = allModels.getOrNull(index - 1)?.second
                    if (model != null) {
                        scope.launch {
                            vm.update(assistant.copy(chatModelId = model.id))
                            toaster.show("Model set to ${model.displayName.ifBlank { model.modelId }}", ToastType.Success)
                        }
                    }
                }
            },
            onDismiss = { showModelPicker = false },
        )
    }
}
