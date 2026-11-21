package me.rerere.rikkahub.wear.pages.speech

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import me.rerere.asr.ASRProviderSetting
import me.rerere.rikkahub.data.datastore.DEFAULT_SYSTEM_TTS_ID
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
import me.rerere.rikkahub.wear.components.radioItem
import me.rerere.rikkahub.wear.components.section
import me.rerere.rikkahub.wear.components.textItem
import me.rerere.tts.provider.TTSProviderSetting
import me.rerere.ui.toast.ToastType
import org.koin.compose.koinInject
import kotlin.uuid.Uuid

private enum class TtsField { NAME, API_KEY, BASE_URL }

private enum class AsrField { NAME, API_KEY, ENDPOINT }

private class TtsFactory(val label: String, val create: () -> TTSProviderSetting)

private class AsrFactory(val label: String, val create: () -> ASRProviderSetting)

private val ttsFactories = listOf(
    TtsFactory("OpenAI") { TTSProviderSetting.OpenAI() },
    TtsFactory("Gemini") { TTSProviderSetting.Gemini() },
    TtsFactory("System TTS") { TTSProviderSetting.SystemTTS() },
    TtsFactory("MiniMax") { TTSProviderSetting.MiniMax() },
    TtsFactory("Qwen") { TTSProviderSetting.Qwen() },
    TtsFactory("Groq") { TTSProviderSetting.Groq() },
    TtsFactory("xAI") { TTSProviderSetting.XAI() },
    TtsFactory("MiMo") { TTSProviderSetting.MiMo() },
    TtsFactory("ElevenLabs") { TTSProviderSetting.ElevenLabs() },
    TtsFactory("Step") { TTSProviderSetting.Step() },
    TtsFactory("Fish Audio") { TTSProviderSetting.FishAudio() },
    TtsFactory("Volcengine") { TTSProviderSetting.Volcengine() },
    TtsFactory("DeepSeek") { TTSProviderSetting.DeepSeek() },
)

private val asrFactories = listOf(
    AsrFactory("OpenAI Realtime") { ASRProviderSetting.OpenAIRealtime() },
    AsrFactory("DashScope") { ASRProviderSetting.DashScope() },
    AsrFactory("Volcengine") { ASRProviderSetting.Volcengine() },
    AsrFactory("MiMo") { ASRProviderSetting.MiMo() },
    AsrFactory("Step") { ASRProviderSetting.Step() },
    AsrFactory("DeepSeek") { ASRProviderSetting.DeepSeek() },
)

/** 手表端语音设置：TTS / ASR 两组 provider 的选择与基础配置。 */
@Composable
fun WearSpeechSettingsPage(
    onBack: () -> Unit,
) {
    val settings = LocalSettings.current
    val store: SettingsStore = koinInject()
    val scope = rememberCoroutineScope()
    val toaster = LocalToaster.current

    var editingTtsId by remember { mutableStateOf<Uuid?>(null) }
    var editingAsrId by remember { mutableStateOf<Uuid?>(null) }
    var showAddTts by remember { mutableStateOf(false) }
    var showAddAsr by remember { mutableStateOf(false) }

    val ttsProviders = settings.ttsProviders
    val asrProviders = settings.asrProviders
    val editingTts = ttsProviders.find { it.id == editingTtsId }
    val editingAsr = asrProviders.find { it.id == editingAsrId }

    if (editingTts != null) {
        TtsProviderDetail(
            provider = editingTts,
            selected = settings.selectedTTSProviderId == editingTts.id,
            onBack = { editingTtsId = null },
            onUpdate = { updated ->
                scope.launch {
                    store.update { latest ->
                        latest.copy(
                            ttsProviders = latest.ttsProviders.map {
                                if (it.id == updated.id) updated else it
                            }
                        )
                    }
                }
            },
            onSelect = {
                scope.launch {
                    store.update { latest -> latest.copy(selectedTTSProviderId = editingTts.id) }
                }
                toaster.show("Default TTS provider updated", ToastType.Success)
            },
            onDelete = {
                scope.launch {
                    store.update { latest ->
                        val remaining = latest.ttsProviders.filterNot { it.id == editingTts.id }
                        latest.copy(
                            ttsProviders = remaining,
                            selectedTTSProviderId = if (latest.selectedTTSProviderId == editingTts.id) {
                                DEFAULT_SYSTEM_TTS_ID
                            } else {
                                latest.selectedTTSProviderId
                            },
                        )
                    }
                }
                editingTtsId = null
                toaster.show("TTS provider deleted", ToastType.Success)
            },
        )
        return
    }

    if (editingAsr != null) {
        AsrProviderDetail(
            provider = editingAsr,
            selected = settings.selectedASRProviderId == editingAsr.id,
            onBack = { editingAsrId = null },
            onUpdate = { updated ->
                scope.launch {
                    store.update { latest ->
                        latest.copy(
                            asrProviders = latest.asrProviders.map {
                                if (it.id == updated.id) updated else it
                            }
                        )
                    }
                }
            },
            onSelect = {
                scope.launch {
                    store.update { latest -> latest.copy(selectedASRProviderId = editingAsr.id) }
                }
                toaster.show("Default ASR provider updated", ToastType.Success)
            },
            onDelete = {
                scope.launch {
                    store.update { latest ->
                        val remaining = latest.asrProviders.filterNot { it.id == editingAsr.id }
                        latest.copy(
                            asrProviders = remaining,
                            selectedASRProviderId = if (latest.selectedASRProviderId == editingAsr.id) {
                                remaining.firstOrNull()?.id
                            } else {
                                latest.selectedASRProviderId
                            },
                        )
                    }
                }
                editingAsrId = null
                toaster.show("ASR provider deleted", ToastType.Success)
            },
        )
        return
    }

    // TTS 和 ASR 共用同一个列表，标题必须整体唯一，否则 LazyColumn 的 key 会冲突。
    val allTitles = numberedTitles(
        ttsProviders.map { it.name.ifBlank { it.typeLabel() } } +
            asrProviders.map { it.name.ifBlank { it.typeLabel() } }
    )
    val ttsTitles = allTitles.take(ttsProviders.size)
    val asrTitles = allTitles.drop(ttsProviders.size)
    val currentTts = ttsProviders.find { it.id == settings.selectedTTSProviderId }
    val currentAsr = asrProviders.find { it.id == settings.selectedASRProviderId }
    val currentTtsLabel = currentTts?.let { it.name.ifBlank { it.typeLabel() } } ?: "System TTS"
    val currentAsrLabel = currentAsr?.let { it.name.ifBlank { it.typeLabel() } } ?: "None"

    WearScreen(title = "Speech", onBack = onBack) {
        section("Text to speech")
        infoItem("Current TTS", currentTtsLabel)
        ttsProviders.forEachIndexed { index, provider ->
            navItem(
                title = ttsTitles[index],
                subtitle = provider.typeLabel() + (if (provider.id == settings.selectedTTSProviderId) " · active" else ""),
                icon = WearIcons.Volume,
                trailingText = if (provider.id == settings.selectedTTSProviderId) "Active" else null,
                onClick = { editingTtsId = provider.id },
            )
        }
        buttonItem(label = "Add TTS provider", icon = WearIcons.Add, tonal = true) {
            showAddTts = true
        }

        section("Speech recognition")
        infoItem("Current ASR", currentAsrLabel)
        if (asrProviders.isEmpty()) {
            textItem("No ASR providers configured. Voice input stays disabled.", maxLines = 3, small = true, muted = true)
        }
        asrProviders.forEachIndexed { index, provider ->
            navItem(
                title = asrTitles[index],
                subtitle = provider.typeLabel() + (if (provider.id == settings.selectedASRProviderId) " · active" else ""),
                icon = WearIcons.Mic,
                trailingText = if (provider.id == settings.selectedASRProviderId) "Active" else null,
                onClick = { editingAsrId = provider.id },
            )
        }
        buttonItem(label = "Add ASR provider", icon = WearIcons.Add, tonal = true) {
            showAddAsr = true
        }
        textItem(
            text = "Voice, model and sampling options are edited on the phone app.",
            maxLines = 3,
            small = true,
            muted = true,
        )
    }

    if (showAddTts) {
        WearOptionDialog(
            visible = true,
            title = "Add TTS provider",
            options = ttsFactories.map { it.label },
            onSelect = { index ->
                val factory = ttsFactories.getOrNull(index)
                if (factory != null) {
                    val created = factory.create()
                    showAddTts = false
                    scope.launch {
                        store.update { latest -> latest.copy(ttsProviders = listOf(created) + latest.ttsProviders) }
                    }
                    editingTtsId = created.id
                }
            },
            onDismiss = { showAddTts = false },
        )
    }

    if (showAddAsr) {
        WearOptionDialog(
            visible = true,
            title = "Add ASR provider",
            options = asrFactories.map { it.label },
            onSelect = { index ->
                val factory = asrFactories.getOrNull(index)
                if (factory != null) {
                    val created = factory.create()
                    showAddAsr = false
                    scope.launch {
                        store.update { latest ->
                            latest.copy(
                                asrProviders = listOf(created) + latest.asrProviders,
                                selectedASRProviderId = latest.selectedASRProviderId ?: created.id,
                            )
                        }
                    }
                    editingAsrId = created.id
                }
            },
            onDismiss = { showAddAsr = false },
        )
    }
}

@Composable
private fun TtsProviderDetail(
    provider: TTSProviderSetting,
    selected: Boolean,
    onBack: () -> Unit,
    onUpdate: (TTSProviderSetting) -> Unit,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
) {
    var editField by remember(provider.id) { mutableStateOf<TtsField?>(null) }
    var confirmDelete by remember(provider.id) { mutableStateOf(false) }
    val canDelete = provider.id != DEFAULT_SYSTEM_TTS_ID && provider !is TTSProviderSetting.SystemTTS

    BackHandler(onBack = onBack)

    WearScreen(title = provider.name.ifBlank { provider.typeLabel() }, onBack = onBack) {
        infoItem("Type", provider.typeLabel())
        radioItem(
            title = "Use for text to speech",
            subtitle = if (selected) "Currently active" else "Tap to make active",
            selected = selected,
            onSelect = onSelect,
        )
        navItem(
            title = "Name",
            subtitle = provider.name.ifBlank { "Not set" },
            icon = WearIcons.Settings,
            onClick = { editField = TtsField.NAME },
        )
        val apiKey = provider.apiKeyOrNull()
        if (apiKey != null) {
            navItem(
                title = "API key",
                subtitle = if (apiKey.isBlank()) "Not set" else maskSecret(apiKey),
                icon = WearIcons.Cpu,
                onClick = { editField = TtsField.API_KEY },
            )
        }
        val baseUrl = provider.baseUrlOrNull()
        if (baseUrl != null) {
            navItem(
                title = "Base URL",
                subtitle = baseUrl,
                icon = WearIcons.Web,
                onClick = { editField = TtsField.BASE_URL },
            )
        }
        if (provider is TTSProviderSetting.SystemTTS) {
            textItem("Uses the watch system speech engine.", maxLines = 2, small = true, muted = true)
        }
        if (canDelete) {
            buttonItem(label = "Delete provider", icon = WearIcons.Delete, tonal = true) {
                confirmDelete = true
            }
        }
    }

    val field = editField
    if (field != null) {
        WearInputDialog(
            visible = true,
            title = when (field) {
                TtsField.NAME -> "Provider name"
                TtsField.API_KEY -> "API key"
                TtsField.BASE_URL -> "Base URL"
            },
            initialValue = when (field) {
                TtsField.NAME -> provider.name
                TtsField.API_KEY -> provider.apiKeyOrNull().orEmpty()
                TtsField.BASE_URL -> provider.baseUrlOrNull().orEmpty()
            },
            singleLine = true,
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
            onConfirm = { value ->
                editField = null
                onUpdate(
                    when (field) {
                        TtsField.NAME -> provider.withName(value.trim())
                        TtsField.API_KEY -> provider.withApiKey(value.trim())
                        TtsField.BASE_URL -> provider.withBaseUrl(value.trim())
                    }
                )
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

@Composable
private fun AsrProviderDetail(
    provider: ASRProviderSetting,
    selected: Boolean,
    onBack: () -> Unit,
    onUpdate: (ASRProviderSetting) -> Unit,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
) {
    var editField by remember(provider.id) { mutableStateOf<AsrField?>(null) }
    var confirmDelete by remember(provider.id) { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    WearScreen(title = provider.name.ifBlank { provider.typeLabel() }, onBack = onBack) {
        infoItem("Type", provider.typeLabel())
        radioItem(
            title = "Use for speech recognition",
            subtitle = if (selected) "Currently active" else "Tap to make active",
            selected = selected,
            onSelect = onSelect,
        )
        navItem(
            title = "Name",
            subtitle = provider.name.ifBlank { "Not set" },
            icon = WearIcons.Settings,
            onClick = { editField = AsrField.NAME },
        )
        navItem(
            title = "API key",
            subtitle = if (provider.apiKeyOrNull().isBlank()) "Not set" else maskSecret(provider.apiKeyOrNull()),
            icon = WearIcons.Cpu,
            onClick = { editField = AsrField.API_KEY },
        )
        navItem(
            title = provider.endpointLabel(),
            subtitle = provider.endpointOrNull().ifBlank { "Not set" },
            icon = WearIcons.Web,
            onClick = { editField = AsrField.ENDPOINT },
        )
        textItem(
            text = "Model and VAD options are edited on the phone app.",
            maxLines = 2,
            small = true,
            muted = true,
        )
        buttonItem(label = "Delete provider", icon = WearIcons.Delete, tonal = true) {
            confirmDelete = true
        }
    }

    val field = editField
    if (field != null) {
        WearInputDialog(
            visible = true,
            title = when (field) {
                AsrField.NAME -> "Provider name"
                AsrField.API_KEY -> "API key"
                AsrField.ENDPOINT -> provider.endpointLabel()
            },
            initialValue = when (field) {
                AsrField.NAME -> provider.name
                AsrField.API_KEY -> provider.apiKeyOrNull()
                AsrField.ENDPOINT -> provider.endpointOrNull()
            },
            singleLine = true,
            onConfirm = { value ->
                editField = null
                val updated = when (field) {
                    AsrField.NAME -> provider.withName(value.trim())
                    AsrField.API_KEY -> provider.withApiKey(value.trim())
                    AsrField.ENDPOINT -> provider.withEndpoint(value.trim())
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

private fun TTSProviderSetting.typeLabel(): String = when (this) {
    is TTSProviderSetting.OpenAI -> "OpenAI"
    is TTSProviderSetting.Gemini -> "Gemini"
    is TTSProviderSetting.SystemTTS -> "System TTS"
    is TTSProviderSetting.MiniMax -> "MiniMax"
    is TTSProviderSetting.Qwen -> "Qwen"
    is TTSProviderSetting.Groq -> "Groq"
    is TTSProviderSetting.XAI -> "xAI"
    is TTSProviderSetting.MiMo -> "MiMo"
    is TTSProviderSetting.ElevenLabs -> "ElevenLabs"
    is TTSProviderSetting.Step -> "Step"
    is TTSProviderSetting.FishAudio -> "Fish Audio"
    is TTSProviderSetting.Volcengine -> "Volcengine"
    is TTSProviderSetting.DeepSeek -> "DeepSeek"
}

private fun TTSProviderSetting.apiKeyOrNull(): String? = when (this) {
    is TTSProviderSetting.OpenAI -> apiKey
    is TTSProviderSetting.Gemini -> apiKey
    is TTSProviderSetting.SystemTTS -> null
    is TTSProviderSetting.MiniMax -> apiKey
    is TTSProviderSetting.Qwen -> apiKey
    is TTSProviderSetting.Groq -> apiKey
    is TTSProviderSetting.XAI -> apiKey
    is TTSProviderSetting.MiMo -> apiKey
    is TTSProviderSetting.ElevenLabs -> apiKey
    is TTSProviderSetting.Step -> apiKey
    is TTSProviderSetting.FishAudio -> apiKey
    is TTSProviderSetting.Volcengine -> apiKey
    is TTSProviderSetting.DeepSeek -> apiKey
}

private fun TTSProviderSetting.baseUrlOrNull(): String? = when (this) {
    is TTSProviderSetting.OpenAI -> baseUrl
    is TTSProviderSetting.Gemini -> baseUrl
    is TTSProviderSetting.SystemTTS -> null
    is TTSProviderSetting.MiniMax -> baseUrl
    is TTSProviderSetting.Qwen -> baseUrl
    is TTSProviderSetting.Groq -> baseUrl
    is TTSProviderSetting.XAI -> baseUrl
    is TTSProviderSetting.MiMo -> baseUrl
    is TTSProviderSetting.ElevenLabs -> baseUrl
    is TTSProviderSetting.Step -> baseUrl
    is TTSProviderSetting.FishAudio -> baseUrl
    is TTSProviderSetting.Volcengine -> baseUrl
    is TTSProviderSetting.DeepSeek -> baseUrl
}

private fun TTSProviderSetting.withName(value: String): TTSProviderSetting = copyProvider(name = value)

private fun TTSProviderSetting.withApiKey(value: String): TTSProviderSetting = when (this) {
    is TTSProviderSetting.OpenAI -> copy(apiKey = value)
    is TTSProviderSetting.Gemini -> copy(apiKey = value)
    is TTSProviderSetting.SystemTTS -> this
    is TTSProviderSetting.MiniMax -> copy(apiKey = value)
    is TTSProviderSetting.Qwen -> copy(apiKey = value)
    is TTSProviderSetting.Groq -> copy(apiKey = value)
    is TTSProviderSetting.XAI -> copy(apiKey = value)
    is TTSProviderSetting.MiMo -> copy(apiKey = value)
    is TTSProviderSetting.ElevenLabs -> copy(apiKey = value)
    is TTSProviderSetting.Step -> copy(apiKey = value)
    is TTSProviderSetting.FishAudio -> copy(apiKey = value)
    is TTSProviderSetting.Volcengine -> copy(apiKey = value)
    is TTSProviderSetting.DeepSeek -> copy(apiKey = value)
}

private fun TTSProviderSetting.withBaseUrl(value: String): TTSProviderSetting = when (this) {
    is TTSProviderSetting.OpenAI -> copy(baseUrl = value)
    is TTSProviderSetting.Gemini -> copy(baseUrl = value)
    is TTSProviderSetting.SystemTTS -> this
    is TTSProviderSetting.MiniMax -> copy(baseUrl = value)
    is TTSProviderSetting.Qwen -> copy(baseUrl = value)
    is TTSProviderSetting.Groq -> copy(baseUrl = value)
    is TTSProviderSetting.XAI -> copy(baseUrl = value)
    is TTSProviderSetting.MiMo -> copy(baseUrl = value)
    is TTSProviderSetting.ElevenLabs -> copy(baseUrl = value)
    is TTSProviderSetting.Step -> copy(baseUrl = value)
    is TTSProviderSetting.FishAudio -> copy(baseUrl = value)
    is TTSProviderSetting.Volcengine -> copy(baseUrl = value)
    is TTSProviderSetting.DeepSeek -> copy(baseUrl = value)
}

private fun ASRProviderSetting.typeLabel(): String = when (this) {
    is ASRProviderSetting.OpenAIRealtime -> "OpenAI Realtime"
    is ASRProviderSetting.DashScope -> "DashScope"
    is ASRProviderSetting.Volcengine -> "Volcengine"
    is ASRProviderSetting.MiMo -> "MiMo"
    is ASRProviderSetting.Step -> "Step"
    is ASRProviderSetting.DeepSeek -> "DeepSeek"
}

private fun ASRProviderSetting.apiKeyOrNull(): String = when (this) {
    is ASRProviderSetting.OpenAIRealtime -> apiKey
    is ASRProviderSetting.DashScope -> apiKey
    is ASRProviderSetting.Volcengine -> apiKey
    is ASRProviderSetting.MiMo -> apiKey
    is ASRProviderSetting.Step -> apiKey
    is ASRProviderSetting.DeepSeek -> apiKey
}

private fun ASRProviderSetting.endpointLabel(): String = when (this) {
    is ASRProviderSetting.OpenAIRealtime -> "WebSocket URL"
    is ASRProviderSetting.DashScope -> "WebSocket URL"
    is ASRProviderSetting.Volcengine -> "WebSocket URL"
    is ASRProviderSetting.MiMo -> "Base URL"
    is ASRProviderSetting.Step -> "Base URL"
    is ASRProviderSetting.DeepSeek -> "Base URL"
}

private fun ASRProviderSetting.endpointOrNull(): String = when (this) {
    is ASRProviderSetting.OpenAIRealtime -> websocketUrl
    is ASRProviderSetting.DashScope -> websocketUrl
    is ASRProviderSetting.Volcengine -> websocketUrl
    is ASRProviderSetting.MiMo -> baseUrl
    is ASRProviderSetting.Step -> baseUrl
    is ASRProviderSetting.DeepSeek -> baseUrl
}

private fun ASRProviderSetting.withName(value: String): ASRProviderSetting = copyProvider(name = value)

private fun ASRProviderSetting.withApiKey(value: String): ASRProviderSetting = when (this) {
    is ASRProviderSetting.OpenAIRealtime -> copy(apiKey = value)
    is ASRProviderSetting.DashScope -> copy(apiKey = value)
    is ASRProviderSetting.Volcengine -> copy(apiKey = value)
    is ASRProviderSetting.MiMo -> copy(apiKey = value)
    is ASRProviderSetting.Step -> copy(apiKey = value)
    is ASRProviderSetting.DeepSeek -> copy(apiKey = value)
}

private fun ASRProviderSetting.withEndpoint(value: String): ASRProviderSetting = when (this) {
    is ASRProviderSetting.OpenAIRealtime -> copy(websocketUrl = value)
    is ASRProviderSetting.DashScope -> copy(websocketUrl = value)
    is ASRProviderSetting.Volcengine -> copy(websocketUrl = value)
    is ASRProviderSetting.MiMo -> copy(baseUrl = value)
    is ASRProviderSetting.Step -> copy(baseUrl = value)
    is ASRProviderSetting.DeepSeek -> copy(baseUrl = value)
}
