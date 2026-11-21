package me.rerere.rikkahub.wear.pages.assistant

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.datastore.findModelById
import me.rerere.rikkahub.ui.context.LocalSettings
import me.rerere.rikkahub.wear.WearIcons
import me.rerere.rikkahub.wear.components.WearScreen
import me.rerere.rikkahub.wear.components.infoItem
import me.rerere.rikkahub.wear.components.navItem
import me.rerere.rikkahub.wear.components.radioItem
import me.rerere.rikkahub.wear.components.section
import me.rerere.rikkahub.wear.components.textItem
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.uuid.Uuid

@Composable
fun WearAssistantsPage(
    onBack: () -> Unit,
    onOpenAssistant: (Uuid) -> Unit,
) {
    val settings = LocalSettings.current
    val store: SettingsStore = koinInject()
    val scope = rememberCoroutineScope()
    val currentAssistant = settings.assistants.find { it.id == settings.assistantId }
        ?: settings.assistants.firstOrNull()

    WearScreen(title = "Assistants", onBack = onBack) {
        textItem("Each assistant has its own prompt, model and tools. Selecting one changes the active chat context.")
        if (currentAssistant != null) {
            buttonItemForAssistant(currentAssistant.id, currentAssistant.name, onOpenAssistant)
        }
        section("Switch assistant")
        settings.assistants.forEach { assistant ->
            val model = settings.findModelById(assistant.chatModelId ?: settings.chatModelId)
            radioItem(
                title = assistant.name.ifBlank { "Assistant" },
                subtitle = model?.displayName?.ifBlank { model.modelId } ?: "No model",
                selected = assistant.id == settings.assistantId,
                onSelect = {
                    scope.launch { store.selectAssistant(assistant.id) }
                },
            )
        }
        infoItem("Total", "${settings.assistants.size}")
    }
}

private fun me.rerere.rikkahub.wear.components.WearListScope.buttonItemForAssistant(
    id: Uuid,
    name: String,
    onOpen: (Uuid) -> Unit,
) {
    navItem(
        title = "Configure ${name.ifBlank { "assistant" }}",
        subtitle = "Prompt, model, memory, tools",
        icon = WearIcons.Settings,
        onClick = { onOpen(id) },
    )
}
