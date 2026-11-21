package me.rerere.rikkahub.wear.pages.provider

import me.rerere.ai.provider.ProviderSetting
import me.rerere.rikkahub.data.datastore.SettingsStore
import kotlin.uuid.Uuid

/** Provider 编辑没有通用 setter，按类型 copy。未知类型原样返回。 */
fun ProviderSetting.withApiKeyValue(value: String): ProviderSetting = when (this) {
    is ProviderSetting.OpenAI -> copy(apiKey = value)
    is ProviderSetting.Google -> copy(apiKey = value)
    is ProviderSetting.Claude -> copy(apiKey = value)
    else -> this
}

fun ProviderSetting.withBaseUrlValue(value: String): ProviderSetting = when (this) {
    is ProviderSetting.OpenAI -> copy(baseUrl = value)
    is ProviderSetting.Google -> copy(baseUrl = value)
    is ProviderSetting.Claude -> copy(baseUrl = value)
    else -> this
}

fun ProviderSetting.apiKeyOrNull(): String? = (this as? ProviderSetting.ApiKey)?.apiKey

fun ProviderSetting.baseUrlOrNull(): String? = (this as? ProviderSetting.ApiKey)?.baseUrl

fun ProviderSetting.withEnabledValue(value: Boolean): ProviderSetting = copyProvider(enabled = value)

fun ProviderSetting.withNameValue(value: String): ProviderSetting = copyProvider(name = value)

fun ProviderSetting.typeLabel(): String = when (this) {
    is ProviderSetting.OpenAI -> "OpenAI compatible"
    is ProviderSetting.Google -> "Google Gemini"
    is ProviderSetting.Claude -> "Anthropic Claude"
    is ProviderSetting.ChatGPT -> "ChatGPT account"
    is ProviderSetting.Grok -> "Grok account"
    else -> this::class.simpleName ?: "Provider"
}

suspend fun SettingsStore.updateProvider(
    providerId: Uuid,
    transform: (ProviderSetting) -> ProviderSetting,
) {
    update { settings ->
        settings.copy(
            providers = settings.providers.map { provider ->
                if (provider.id == providerId) transform(provider) else provider
            }
        )
    }
}
