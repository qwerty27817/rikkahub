package me.rerere.rikkahub.wear.pages.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.ui.context.LocalSettings
import me.rerere.rikkahub.ui.context.LocalToaster
import me.rerere.rikkahub.wear.components.WearScreen
import me.rerere.rikkahub.wear.components.radioItem
import me.rerere.rikkahub.wear.components.section
import me.rerere.rikkahub.wear.components.textItem
import kotlinx.coroutines.launch
import me.rerere.ui.toast.ToastType
import org.koin.compose.koinInject

@Composable
fun WearModelsPage(onBack: () -> Unit) {
    val settings = LocalSettings.current
    val store: SettingsStore = koinInject()
    val scope = rememberCoroutineScope()
    val toaster = LocalToaster.current
    val currentModelId = settings.findCurrentModelId()

    WearScreen(title = "Model", onBack = onBack) {
        if (settings.providers.none { it.models.isNotEmpty() }) {
            textItem("No models configured. Add a provider first.")
        }
        settings.providers.filter { it.enabled }.forEach { provider ->
            if (provider.models.isNotEmpty()) {
                section(provider.name)
                provider.models.forEach { model ->
                    radioItem(
                        title = model.displayName.ifBlank { model.modelId },
                        subtitle = model.modelId,
                        selected = model.id == currentModelId,
                        onSelect = {
                            scope.launch {
                                store.update { s ->
                                    s.copy(
                                        chatModelId = model.id,
                                        assistants = s.assistants.map { assistant ->
                                            if (assistant.id == s.assistantId) {
                                                assistant.copy(chatModelId = model.id)
                                            } else {
                                                assistant
                                            }
                                        },
                                    )
                                }
                                toaster.show("Model set to ${model.displayName.ifBlank { model.modelId }}", ToastType.Success)
                            }
                        },
                    )
                }
            }
        }
    }
}

private fun me.rerere.rikkahub.data.datastore.Settings.findCurrentModelId(): kotlin.uuid.Uuid {
    val assistant = getCurrentAssistantOrNull()
    return assistant?.chatModelId ?: chatModelId
}

private fun me.rerere.rikkahub.data.datastore.Settings.getCurrentAssistantOrNull() =
    assistants.find { it.id == assistantId }
