package me.rerere.rikkahub.wear.pages.provider

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import me.rerere.ai.provider.ProviderSetting
import me.rerere.rikkahub.R
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

/** Provider 类型的显示名；ProviderEdit.kt 里的 typeLabel() 是非 Composable 的纯函数，这里做本地化映射。 */
@Composable
internal fun wearProviderTypeLabel(provider: ProviderSetting): String = when (provider) {
    is ProviderSetting.OpenAI -> stringResource(R.string.wear_svc_provider_openai_compatible)
    is ProviderSetting.Google -> stringResource(R.string.wear_svc_provider_google_gemini)
    is ProviderSetting.Claude -> stringResource(R.string.wear_svc_provider_anthropic_claude)
    is ProviderSetting.ChatGPT -> stringResource(R.string.wear_svc_provider_chatgpt_account)
    is ProviderSetting.Grok -> stringResource(R.string.wear_svc_provider_grok_account)
    else -> provider::class.simpleName ?: stringResource(R.string.wear_svc_provider_generic)
}

@Composable
fun WearProvidersPage(
    onBack: () -> Unit,
    onOpenProvider: (Uuid) -> Unit,
) {
    val settings = LocalSettings.current
    val store: SettingsStore = koinInject()
    val scope = rememberCoroutineScope()
    val toaster = LocalToaster.current
    val onLabel = stringResource(R.string.wear_svc_on)
    val offLabel = stringResource(R.string.wear_svc_off)
    val openAiAdded = stringResource(R.string.wear_svc_provider_added, "OpenAI")
    val googleAdded = stringResource(R.string.wear_svc_provider_added, "Google")
    val claudeAdded = stringResource(R.string.wear_svc_provider_added, "Claude")

    WearScreen(title = stringResource(R.string.wear_svc_providers_title), onBack = onBack) {
        settings.providers.forEach { provider ->
            val typeLabel = wearProviderTypeLabel(provider)
            navItem(
                title = provider.name.ifBlank { typeLabel },
                subtitle = stringResource(
                    R.string.wear_svc_providers_subtitle,
                    typeLabel,
                    provider.models.size,
                    if (provider.enabled) onLabel else offLabel,
                ),
                icon = WearIcons.Server,
                onClick = { onOpenProvider(provider.id) },
            )
        }
        section(stringResource(R.string.wear_svc_add_provider))
        buttonItem(label = stringResource(R.string.wear_svc_provider_openai_compatible), tonal = true) {
            scope.launch {
                store.update { s ->
                    s.copy(providers = s.providers + ProviderSetting.OpenAI(name = "OpenAI"))
                }
                toaster.show(openAiAdded, ToastType.Success)
            }
        }
        buttonItem(label = stringResource(R.string.wear_svc_provider_google_gemini), tonal = true) {
            scope.launch {
                store.update { s ->
                    s.copy(providers = s.providers + ProviderSetting.Google(name = "Google"))
                }
                toaster.show(googleAdded, ToastType.Success)
            }
        }
        buttonItem(label = stringResource(R.string.wear_svc_provider_anthropic_claude), tonal = true) {
            scope.launch {
                store.update { s ->
                    s.copy(providers = s.providers + ProviderSetting.Claude(name = "Claude"))
                }
                toaster.show(claudeAdded, ToastType.Success)
            }
        }
    }
}
