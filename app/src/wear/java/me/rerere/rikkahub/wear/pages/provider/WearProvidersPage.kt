package me.rerere.rikkahub.wear.pages.provider

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import me.rerere.ai.provider.ProviderSetting
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.ui.context.LocalSettings
import me.rerere.rikkahub.ui.context.LocalToaster
import me.rerere.rikkahub.wear.WearIcons
import me.rerere.rikkahub.wear.components.WearScreen
import me.rerere.rikkahub.wear.components.buttonItem
import me.rerere.rikkahub.wear.components.navItem
import me.rerere.rikkahub.wear.components.section
import kotlinx.coroutines.launch
import me.rerere.ui.toast.ToastType
import org.koin.compose.koinInject
import kotlin.uuid.Uuid

@Composable
fun WearProvidersPage(
    onBack: () -> Unit,
    onOpenProvider: (Uuid) -> Unit,
) {
    val settings = LocalSettings.current
    val store: SettingsStore = koinInject()
    val scope = rememberCoroutineScope()
    val toaster = LocalToaster.current

    WearScreen(title = "Providers", onBack = onBack) {
        settings.providers.forEach { provider ->
            navItem(
                title = provider.name.ifBlank { provider.typeLabel() },
                subtitle = "${provider.typeLabel()} · ${provider.models.size} models · ${if (provider.enabled) "on" else "off"}",
                icon = WearIcons.Server,
                onClick = { onOpenProvider(provider.id) },
            )
        }
        section("Add provider")
        buttonItem(label = "OpenAI compatible", tonal = true) {
            scope.launch {
                store.update { s ->
                    s.copy(providers = s.providers + ProviderSetting.OpenAI(name = "OpenAI"))
                }
                toaster.show("OpenAI provider added", ToastType.Success)
            }
        }
        buttonItem(label = "Google Gemini", tonal = true) {
            scope.launch {
                store.update { s ->
                    s.copy(providers = s.providers + ProviderSetting.Google(name = "Google"))
                }
                toaster.show("Google provider added", ToastType.Success)
            }
        }
        buttonItem(label = "Anthropic Claude", tonal = true) {
            scope.launch {
                store.update { s ->
                    s.copy(providers = s.providers + ProviderSetting.Claude(name = "Claude"))
                }
                toaster.show("Claude provider added", ToastType.Success)
            }
        }
    }
}
