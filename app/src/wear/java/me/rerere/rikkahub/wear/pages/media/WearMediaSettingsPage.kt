package me.rerere.rikkahub.wear.pages.media

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.launch
import me.rerere.ai.provider.Model
import me.rerere.ai.provider.ModelType
import me.rerere.ai.provider.ProviderSetting
import me.rerere.mediagen.provider.MediaGenerationProviderSetting
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.ui.context.LocalSettings
import me.rerere.rikkahub.ui.context.LocalToaster
import me.rerere.rikkahub.wear.WearIcons
import me.rerere.rikkahub.wear.components.WearConfirmDialog
import me.rerere.rikkahub.wear.components.WearInputDialog
import me.rerere.rikkahub.wear.components.WearOptionDialog
import me.rerere.rikkahub.wear.components.WearScreen
import me.rerere.rikkahub.wear.components.buttonItem
import me.rerere.rikkahub.wear.components.infoItem
import me.rerere.rikkahub.wear.components.navItem
import me.rerere.rikkahub.wear.components.section
import me.rerere.rikkahub.wear.components.textItem
import me.rerere.ui.toast.ToastType
import org.koin.compose.koinInject
import kotlin.uuid.Uuid

private enum class MediaField { NAME, API_KEY, BASE_URL, WORKSPACE_ID }

private class MediaFactory(val label: String, val create: () -> MediaGenerationProviderSetting)

private val mediaFactories = listOf(
    MediaFactory("OpenAI") { MediaGenerationProviderSetting.OpenAI() },
    MediaFactory("Aliyun") { MediaGenerationProviderSetting.Aliyun() },
    MediaFactory("Volcengine") { MediaGenerationProviderSetting.Volcengine() },
    MediaFactory("MiniMax") { MediaGenerationProviderSetting.MiniMax() },
    MediaFactory("OpenRouter") { MediaGenerationProviderSetting.OpenRouter() },
)

/** 手表端媒体生成设置：provider 列表 / 基础配置 / 默认图像模型选择。 */
@Composable
fun WearMediaSettingsPage(
    onBack: () -> Unit,
) {
    val settings = LocalSettings.current
    val store: SettingsStore = koinInject()
    val scope = rememberCoroutineScope()
    val toaster = LocalToaster.current

    var editingId by remember { mutableStateOf<Uuid?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var showImageModel by remember { mutableStateOf(false) }

    val providers = settings.mediaGenerationProviders
    val editing = providers.find { it.id == editingId }

    val imageModels: List<Pair<ProviderSetting, Model>> = settings.providers.flatMap { provider ->
        provider.models
            .filter { it.type == ModelType.IMAGE }
            .map { provider to it }
    }

    if (editing != null) {
        MediaProviderDetail(
            provider = editing,
            onBack = { editingId = null },
            onUpdate = { updated ->
                scope.launch {
                    store.update { latest ->
                        latest.copy(
                            mediaGenerationProviders = latest.mediaGenerationProviders.map {
                                if (it.id == updated.id) updated else it
                            }
                        )
                    }
                }
            },
            onDelete = {
                scope.launch {
                    store.update { latest ->
                        latest.copy(
                            mediaGenerationProviders = latest.mediaGenerationProviders.filterNot { it.id == editing.id }
                        )
                    }
                }
                editingId = null
                toaster.show("Media provider deleted", ToastType.Success)
            },
        )
        return
    }

    val titles = numberedTitles(providers.map { it.name.ifBlank { it.typeLabel() } })
    val currentImageModel = imageModels.firstOrNull { it.second.id == settings.imageGenerationModelId }
    val currentImageModelLabel = currentImageModel?.let { (provider, model) ->
        "${provider.name.ifBlank { provider.typeLabel() }}: ${model.displayName.ifBlank { model.modelId }}"
    } ?: "Not selected"
    val imageModelLabels = imageModels.map { (provider, model) ->
        "${provider.name.ifBlank { provider.typeLabel() }}: ${model.displayName.ifBlank { model.modelId }}"
    }

    WearScreen(title = "Media", onBack = onBack) {
        section("Image generation")
        infoItem("Image model", currentImageModelLabel)
        buttonItem(label = "Select image model", icon = WearIcons.Image, tonal = true) {
            showImageModel = true
        }
        if (imageModels.isEmpty()) {
            textItem(
                text = "No image model found. Add an image model to an AI provider first.",
                maxLines = 3,
                small = true,
                muted = true,
            )
        }

        section("Generation providers")
        if (providers.isEmpty()) {
            textItem("No media generation providers configured.")
        }
        providers.forEachIndexed { index, provider ->
            val modelCount = provider.models.size
            navItem(
                title = titles[index],
                subtitle = "${provider.typeLabel()} · $modelCount model(s)",
                icon = WearIcons.Image,
                onClick = { editingId = provider.id },
            )
        }
        buttonItem(label = "Add media provider", icon = WearIcons.Add, tonal = true) {
            showAdd = true
        }
        textItem(
            text = "Per-provider model lists are edited on the phone app.",
            maxLines = 3,
            small = true,
            muted = true,
        )
    }

    if (showImageModel) {
        WearOptionDialog(
            visible = true,
            title = "Image generation model",
            options = imageModelLabels,
            selectedIndex = imageModelLabels.indexOf(currentImageModelLabel),
            onSelect = { index ->
                val model = imageModels.getOrNull(index)?.second
                if (model != null) {
                    showImageModel = false
                    scope.launch {
                        store.update { latest -> latest.copy(imageGenerationModelId = model.id) }
                    }
                    toaster.show("Image model updated", ToastType.Success)
                }
            },
            onDismiss = { showImageModel = false },
        )
    }

    if (showAdd) {
        WearOptionDialog(
            visible = true,
            title = "Add media provider",
            options = mediaFactories.map { it.label },
            onSelect = { index ->
                val factory = mediaFactories.getOrNull(index)
                if (factory != null) {
                    val created = factory.create()
                    showAdd = false
                    scope.launch {
                        store.update { latest ->
                            latest.copy(mediaGenerationProviders = listOf(created) + latest.mediaGenerationProviders)
                        }
                    }
                    editingId = created.id
                }
            },
            onDismiss = { showAdd = false },
        )
    }
}

@Composable
private fun MediaProviderDetail(
    provider: MediaGenerationProviderSetting,
    onBack: () -> Unit,
    onUpdate: (MediaGenerationProviderSetting) -> Unit,
    onDelete: () -> Unit,
) {
    var editField by remember(provider.id) { mutableStateOf<MediaField?>(null) }
    var confirmDelete by remember(provider.id) { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    WearScreen(title = provider.name.ifBlank { provider.typeLabel() }, onBack = onBack) {
        infoItem("Type", provider.typeLabel())
        navItem(
            title = "Name",
            subtitle = provider.name.ifBlank { "Not set" },
            icon = WearIcons.Settings,
            onClick = { editField = MediaField.NAME },
        )
        navItem(
            title = "API key",
            subtitle = if (provider.apiKey.isBlank()) "Not set" else maskSecret(provider.apiKey),
            icon = WearIcons.Cpu,
            onClick = { editField = MediaField.API_KEY },
        )
        navItem(
            title = "Base URL",
            subtitle = provider.baseUrl.ifBlank { "Not set" },
            icon = WearIcons.Web,
            onClick = { editField = MediaField.BASE_URL },
        )
        if (provider is MediaGenerationProviderSetting.Aliyun) {
            navItem(
                title = "Workspace ID",
                subtitle = provider.workspaceId.ifBlank { "Not set" },
                icon = WearIcons.Settings,
                onClick = { editField = MediaField.WORKSPACE_ID },
            )
        }

        section("Models")
        if (provider.models.isEmpty()) {
            textItem("No models configured for this provider.")
        }
        provider.models.forEachIndexed { index, model ->
            infoItem(
                "Model ${index + 1}",
                "${model.kind.name.lowercase()} · ${model.modelId.ifBlank { model.displayName }}",
            )
        }

        buttonItem(label = "Delete provider", icon = WearIcons.Delete, tonal = true) {
            confirmDelete = true
        }
    }

    val field = editField
    if (field != null) {
        val title = when (field) {
            MediaField.NAME -> "Provider name"
            MediaField.API_KEY -> "API key"
            MediaField.BASE_URL -> "Base URL"
            MediaField.WORKSPACE_ID -> "Workspace ID"
        }
        val initialValue = when (field) {
            MediaField.NAME -> provider.name
            MediaField.API_KEY -> provider.apiKey
            MediaField.BASE_URL -> provider.baseUrl
            MediaField.WORKSPACE_ID -> (provider as? MediaGenerationProviderSetting.Aliyun)?.workspaceId.orEmpty()
        }
        WearInputDialog(
            visible = true,
            title = title,
            initialValue = initialValue,
            singleLine = true,
            keyboardType = if (field == MediaField.API_KEY) KeyboardType.Password else KeyboardType.Text,
            onConfirm = { value ->
                editField = null
                val updated = when (field) {
                    MediaField.NAME -> provider.copyProvider(name = value.trim())
                    MediaField.API_KEY -> provider.copyProvider(apiKey = value.trim())
                    MediaField.BASE_URL -> provider.copyProvider(baseUrl = value.trim())
                    MediaField.WORKSPACE_ID -> {
                        val aliyun = provider as? MediaGenerationProviderSetting.Aliyun
                        if (aliyun != null) aliyun.copy(workspaceId = value.trim()) else provider
                    }
                }
                onUpdate(updated)
            },
            onDismiss = { editField = null },
        )
    }

    WearConfirmDialog(
        visible = confirmDelete,
        title = "Delete provider?",
        message = provider.name.ifBlank { "This provider" } + " will be removed.",
        confirmLabel = "Delete",
        danger = true,
        onConfirm = {
            confirmDelete = false
            onDelete()
        },
        onDismiss = { confirmDelete = false },
    )
}

private fun numberedTitles(names: List<String>): List<String> {
    val seen = mutableMapOf<String, Int>()
    return names.map { name ->
        val label = name.ifBlank { "Unnamed provider" }
        val count = (seen[label] ?: 0) + 1
        seen[label] = count
        if (count == 1) label else "$label #$count"
    }
}

private fun maskSecret(value: String): String = when {
    value.isBlank() -> "Not set"
    value.length <= 4 -> "••••"
    else -> "••••${value.takeLast(4)}"
}

private fun MediaGenerationProviderSetting.typeLabel(): String = when (this) {
    is MediaGenerationProviderSetting.OpenAI -> "OpenAI"
    is MediaGenerationProviderSetting.Aliyun -> "Aliyun"
    is MediaGenerationProviderSetting.Volcengine -> "Volcengine"
    is MediaGenerationProviderSetting.MiniMax -> "MiniMax"
    is MediaGenerationProviderSetting.OpenRouter -> "OpenRouter"
}

private fun ProviderSetting.typeLabel(): String = when (this) {
    is ProviderSetting.OpenAI -> "OpenAI"
    is ProviderSetting.Google -> "Gemini"
    is ProviderSetting.Claude -> "Claude"
    is ProviderSetting.ChatGPT -> "ChatGPT"
    is ProviderSetting.Grok -> "Grok"
    else -> "Provider"
}
