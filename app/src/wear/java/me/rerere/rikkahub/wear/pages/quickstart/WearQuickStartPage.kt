package me.rerere.rikkahub.wear.pages.quickstart

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.ui.context.LocalSettings
import me.rerere.rikkahub.ui.context.LocalToaster
import me.rerere.rikkahub.wear.WearIcons
import me.rerere.rikkahub.wear.components.WearInputDialog
import me.rerere.rikkahub.wear.components.WearScreen
import me.rerere.rikkahub.wear.components.navItem
import me.rerere.rikkahub.wear.components.radioItem
import me.rerere.rikkahub.wear.components.section
import me.rerere.rikkahub.wear.components.textItem
import me.rerere.rikkahub.wear.pages.provider.apiKeyOrNull
import me.rerere.rikkahub.wear.pages.provider.typeLabel
import me.rerere.rikkahub.wear.pages.provider.updateProvider
import me.rerere.rikkahub.wear.pages.provider.withApiKeyValue
import kotlinx.coroutines.launch
import me.rerere.ui.toast.ToastType
import org.koin.compose.koinInject
import kotlin.uuid.Uuid

/**
 * 手表端引导：填一个 Provider 的 API Key，然后选模型。
 * 账号登录类 Provider（ChatGPT / Grok）需要浏览器 OAuth，手表上提示去手机端完成。
 */
@Composable
fun WearQuickStartPage(
    onBack: () -> Unit,
) {
    val settings = LocalSettings.current
    val store: SettingsStore = koinInject()
    val scope = rememberCoroutineScope()
    val toaster = LocalToaster.current
    var editingProviderId by remember { mutableStateOf<Uuid?>(null) }
    var editingValue by remember { mutableStateOf("") }

    WearScreen(title = "Quick start", onBack = onBack) {
        textItem("Add an API key for one provider, then choose a chat model. Everything else can be configured later in Settings.")
        settings.providers.forEach { provider ->
            val apiKey = provider.apiKeyOrNull()
            when {
                apiKey == null -> navItem(
                    title = provider.name.ifBlank { provider.typeLabel() },
                    subtitle = "Account login — sign in on the phone app first",
                    icon = WearIcons.Assistant,
                    onClick = {},
                )
                else -> navItem(
                    title = provider.name.ifBlank { provider.typeLabel() },
                    subtitle = if (apiKey.isBlank()) "API key not set" else "API key set · ${provider.models.size} models",
                    icon = WearIcons.Cpu,
                    onClick = {
                        editingProviderId = provider.id
                        editingValue = apiKey
                    },
                )
            }
        }

        val models = settings.providers.filter { it.enabled }.flatMap { it.models }
        if (models.isNotEmpty()) {
            section("Choose model")
            settings.providers.filter { it.enabled }.forEach { provider ->
                provider.models.forEach { model ->
                    radioItem(
                        title = model.displayName.ifBlank { model.modelId },
                        subtitle = provider.name,
                        selected = model.id == settings.chatModelId,
                        onSelect = {
                            scope.launch {
                                store.update { s ->
                                    s.copy(
                                        chatModelId = model.id,
                                        assistants = s.assistants.map { a ->
                                            if (a.id == s.assistantId) a.copy(chatModelId = model.id) else a
                                        },
                                    )
                                }
                                toaster.show("Model selected", ToastType.Success)
                            }
                        },
                    )
                }
            }
        } else {
            textItem("No models yet — set an API key above first.")
        }
    }

    val editingProvider = settings.providers.find { it.id == editingProviderId }
    if (editingProvider != null) {
        WearInputDialog(
            visible = true,
            title = "${editingProvider.name} API key",
            initialValue = editingValue,
            singleLine = true,
            placeholder = "sk-...",
            onConfirm = { value ->
                val id = editingProvider.id
                editingProviderId = null
                scope.launch {
                    store.updateProvider(id) { it.withApiKeyValue(value.trim()) }
                    toaster.show("API key saved", ToastType.Success)
                }
            },
            onDismiss = { editingProviderId = null },
        )
    }
}
