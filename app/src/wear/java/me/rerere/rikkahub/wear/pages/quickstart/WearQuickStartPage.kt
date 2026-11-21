package me.rerere.rikkahub.wear.pages.quickstart

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import me.rerere.rikkahub.R
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

    // WearScreen 的 content 是普通 DSL builder（非 @Composable），字符串要先在外部取好；
    // 每张服务商卡片的动态副标题用带占位符的模板在 DSL 内格式化。
    val title = stringResource(R.string.wear_set_quick_start)
    val intro = stringResource(R.string.wear_set_quickstart_intro)
    val accountLogin = stringResource(R.string.wear_set_account_login)
    val apiKeyNotSet = stringResource(R.string.wear_set_api_key_not_set)
    val apiKeySetFormat = stringResource(R.string.wear_set_api_key_set_models)
    val chooseModelSection = stringResource(R.string.wear_set_choose_model)
    val noModelsYet = stringResource(R.string.wear_set_no_models_yet)
    val modelSelected = stringResource(R.string.wear_set_model_selected)
    val apiKeySaved = stringResource(R.string.wear_set_api_key_saved)
    val apiKeyPlaceholder = stringResource(R.string.wear_set_api_key_placeholder)

    WearScreen(title = title, onBack = onBack) {
        textItem(intro)
        settings.providers.forEach { provider ->
            val apiKey = provider.apiKeyOrNull()
            when {
                apiKey == null -> navItem(
                    title = provider.name.ifBlank { provider.typeLabel() },
                    subtitle = accountLogin,
                    icon = WearIcons.Assistant,
                    onClick = {},
                )
                else -> navItem(
                    title = provider.name.ifBlank { provider.typeLabel() },
                    subtitle = if (apiKey.isBlank()) {
                        apiKeyNotSet
                    } else {
                        apiKeySetFormat.format(provider.models.size)
                    },
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
            section(chooseModelSection)
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
                                toaster.show(modelSelected, ToastType.Success)
                            }
                        },
                    )
                }
            }
        } else {
            textItem(noModelsYet)
        }
    }

    val editingProvider = settings.providers.find { it.id == editingProviderId }
    if (editingProvider != null) {
        WearInputDialog(
            visible = true,
            title = stringResource(R.string.wear_set_api_key_dialog_title, editingProvider.name),
            initialValue = editingValue,
            singleLine = true,
            placeholder = apiKeyPlaceholder,
            confirmLabel = stringResource(R.string.wear_common_ok),
            onConfirm = { value ->
                val id = editingProvider.id
                editingProviderId = null
                scope.launch {
                    store.updateProvider(id) { it.withApiKeyValue(value.trim()) }
                    toaster.show(apiKeySaved, ToastType.Success)
                }
            },
            onDismiss = { editingProviderId = null },
        )
    }
}
