package me.rerere.rikkahub.wear.pages.provider

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import me.rerere.ai.provider.Model
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
        WearScreen(title = "Provider", onBack = onBack) {
            textItem("This provider no longer exists.")
        }
        return
    }

    WearScreen(title = provider.name.ifBlank { provider.typeLabel() }, onBack = onBack) {
        infoItem("Type", provider.typeLabel())
        infoItem("Models", "${provider.models.size}")
        toggleItem(
            title = "Enabled",
            checked = provider.enabled,
            onCheckedChange = { enabled ->
                scope.launch { store.updateProvider(providerId) { it.withEnabledValue(enabled) } }
            },
        )
        navItem("Name", provider.name, WearIcons.Settings) { editField = EditField.NAME }

        val apiKey = provider.apiKeyOrNull()
        if (apiKey != null) {
            navItem(
                title = "API key",
                subtitle = if (apiKey.isBlank()) "Not set" else "••••••${apiKey.takeLast(4)}",
                icon = WearIcons.Cpu,
            ) { editField = EditField.API_KEY }
        }
        val baseUrl = provider.baseUrlOrNull()
        if (baseUrl != null) {
            navItem("Base URL", baseUrl, WearIcons.Web) { editField = EditField.BASE_URL }
        }

        if (provider.models.isNotEmpty()) {
            section("Models")
            provider.models.take(10).forEach { model ->
                textItem(
                    text = model.displayName.ifBlank { model.modelId },
                    maxLines = 1,
                    small = true,
                )
            }
            if (provider.models.size > 10) {
                textItem("+${provider.models.size - 10} more")
            }
        }
        buttonItem(label = "Add model", icon = WearIcons.Add, tonal = true) {
            addModel = true
        }

        if (!provider.builtIn && settings.providers.size > 1) {
            section("Danger")
            buttonItem(label = "Delete provider", icon = WearIcons.Delete, tonal = true) {
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
        EditField.NAME -> "Provider name"
        EditField.API_KEY -> "API key"
        EditField.BASE_URL -> "Base URL"
        null -> ""
    }
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
                    toaster.show("$dialogTitle saved", ToastType.Success)
                }
            },
            onDismiss = { editField = null },
        )
    }

    if (addModel) {
        WearInputDialog(
            visible = true,
            title = "Add model",
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
                        toaster.show("Model added", ToastType.Success)
                    }
                }
            },
            onDismiss = { addModel = false },
        )
    }

    WearConfirmDialog(
        visible = confirmDelete,
        title = "Delete provider?",
        message = "Models from this provider will be removed from the model list.",
        confirmLabel = "Delete",
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
