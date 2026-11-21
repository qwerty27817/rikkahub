package me.rerere.rikkahub.wear.pages.provider

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import me.rerere.ai.provider.Model
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.ui.context.LocalSettings
import me.rerere.rikkahub.ui.context.LocalToaster
import me.rerere.rikkahub.wear.WearIcons
import me.rerere.rikkahub.wear.components.WearConfirmDialog
import me.rerere.rikkahub.wear.components.WearInputDialog
import me.rerere.rikkahub.wear.components.WearScreen
import me.rerere.rikkahub.wear.components.buttonItem
import me.rerere.rikkahub.wear.components.infoItem
import me.rerere.rikkahub.wear.components.navItem
import me.rerere.rikkahub.wear.components.section
import me.rerere.rikkahub.wear.components.textItem
import me.rerere.rikkahub.wear.components.toggleItem
import kotlinx.coroutines.launch
import me.rerere.ui.toast.ToastType
import org.koin.compose.koinInject
import kotlin.uuid.Uuid

private enum class EditField { NAME, API_KEY, BASE_URL }

@Composable
fun WearProviderDetailPage(
    providerId: Uuid,
    onBack: () -> Unit,
) {
    val settings = LocalSettings.current
    val store: SettingsStore = koinInject()
    val scope = rememberCoroutineScope()
    val toaster = LocalToaster.current
    val provider = settings.providers.find { it.id == providerId }
    var editField by remember { mutableStateOf<EditField?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var addModel by remember { mutableStateOf(false) }

    if (provider == null) {
        WearScreen(title = stringResource(R.string.wear_svc_provider_title), onBack = onBack) {
            textItem(stringResource(R.string.wear_svc_provider_missing))
        }
        return
    }

    val providerLabel = wearProviderTypeLabel(provider)
    val notSetLabel = stringResource(R.string.wear_svc_not_set)
    val modelAdded = stringResource(R.string.wear_svc_model_added)

    WearScreen(title = provider.name.ifBlank { providerLabel }, onBack = onBack) {
        infoItem(stringResource(R.string.wear_svc_type), providerLabel)
        infoItem(stringResource(R.string.wear_svc_models), "${provider.models.size}")
        toggleItem(
            title = stringResource(R.string.wear_svc_enable),
            checked = provider.enabled,
            onCheckedChange = { enabled ->
                scope.launch { store.updateProvider(providerId) { it.withEnabledValue(enabled) } }
            },
        )
        navItem(stringResource(R.string.wear_svc_name), provider.name, WearIcons.Settings) {
            editField = EditField.NAME
        }

        val apiKey = provider.apiKeyOrNull()
        if (apiKey != null) {
            navItem(
                title = stringResource(R.string.wear_svc_api_key),
                subtitle = if (apiKey.isBlank()) notSetLabel else "••••••${apiKey.takeLast(4)}",
                icon = WearIcons.Cpu,
            ) { editField = EditField.API_KEY }
        }
        val baseUrl = provider.baseUrlOrNull()
        if (baseUrl != null) {
            navItem(stringResource(R.string.wear_svc_base_url), baseUrl, WearIcons.Web) {
                editField = EditField.BASE_URL
            }
        }

        if (provider.models.isNotEmpty()) {
            section(stringResource(R.string.wear_svc_models))
            provider.models.take(10).forEach { model ->
                textItem(
                    text = model.displayName.ifBlank { model.modelId },
                    maxLines = 1,
                    small = true,
                )
            }
            if (provider.models.size > 10) {
                textItem(stringResource(R.string.wear_svc_more_models, provider.models.size - 10))
            }
        }
        buttonItem(label = stringResource(R.string.wear_svc_add_model), icon = WearIcons.Add, tonal = true) {
            addModel = true
        }

        if (!provider.builtIn && settings.providers.size > 1) {
            section(stringResource(R.string.wear_svc_danger))
            buttonItem(
                label = stringResource(R.string.wear_svc_delete_provider),
                icon = WearIcons.Delete,
                tonal = true,
            ) {
                confirmDelete = true
            }
        }
    }

    val initialValue = when (editField) {
        EditField.NAME -> provider.name
        EditField.API_KEY -> provider.apiKeyOrNull().orEmpty()
        EditField.BASE_URL -> provider.baseUrlOrNull().orEmpty()
        null -> ""
    }
    val dialogTitle = when (editField) {
        EditField.NAME -> stringResource(R.string.wear_svc_provider_name)
        EditField.API_KEY -> stringResource(R.string.wear_svc_api_key)
        EditField.BASE_URL -> stringResource(R.string.wear_svc_base_url)
        null -> ""
    }
    val savedMessage = stringResource(R.string.wear_svc_saved, dialogTitle)
    if (editField != null) {
        WearInputDialog(
            visible = true,
            title = dialogTitle,
            initialValue = initialValue,
            singleLine = editField != EditField.BASE_URL,
            placeholder = when (editField) {
                EditField.API_KEY -> "sk-..."
                EditField.BASE_URL -> "https://api.openai.com/v1"
                else -> null
            },
            onConfirm = { value ->
                val field = editField
                editField = null
                scope.launch {
                    store.updateProvider(providerId) { p ->
                        when (field) {
                            EditField.NAME -> p.withNameValue(value.trim())
                            EditField.API_KEY -> p.withApiKeyValue(value.trim())
                            EditField.BASE_URL -> p.withBaseUrlValue(value.trim())
                            null -> p
                        }
                    }
                    toaster.show(savedMessage, ToastType.Success)
                }
            },
            onDismiss = { editField = null },
        )
    }

    if (addModel) {
        WearInputDialog(
            visible = true,
            title = stringResource(R.string.wear_svc_add_model),
            initialValue = "",
            singleLine = true,
            placeholder = "gpt-4o-mini",
            onConfirm = { value ->
                addModel = false
                if (value.isNotBlank()) {
                    scope.launch {
                        store.updateProvider(providerId) { p ->
                            p.copyProvider(
                                models = p.models + Model(
                                    modelId = value.trim(),
                                    displayName = value.trim(),
                                )
                            )
                        }
                        toaster.show(modelAdded, ToastType.Success)
                    }
                }
            },
            onDismiss = { addModel = false },
        )
    }

    WearConfirmDialog(
        visible = confirmDelete,
        title = stringResource(R.string.wear_svc_delete_provider_title),
        message = stringResource(R.string.wear_svc_delete_provider_message),
        confirmLabel = stringResource(R.string.wear_common_delete),
        danger = true,
        onConfirm = {
            confirmDelete = false
            scope.launch {
                store.update { s ->
                    s.copy(providers = s.providers.filterNot { it.id == providerId })
                }
                onBack()
            }
        },
        onDismiss = { confirmDelete = false },
    )
}
