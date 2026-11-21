package me.rerere.rikkahub.wear.pages.speech

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import me.rerere.asr.ASRProviderSetting
import me.rerere.rikkahub.R
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

private class TtsFactory(
    val label: String,
    val isSystem: Boolean = false,
    val create: () -> TTSProviderSetting,
)

private class AsrFactory(val label: String, val create: () -> ASRProviderSetting)

private val ttsFactories = listOf(
    TtsFactory("OpenAI") { TTSProviderSetting.OpenAI() },
    TtsFactory("Gemini") { TTSProviderSetting.Gemini() },
    TtsFactory("System TTS", isSystem = true) { TTSProviderSetting.SystemTTS() },
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
    val ttsUpdated = stringResource(R.string.wear_svc_tts_default_updated)
    val ttsDeleted = stringResource(R.string.wear_svc_tts_deleted)
    val asrUpdated = stringResource(R.string.wear_svc_asr_default_updated)
    val asrDeleted = stringResource(R.string.wear_svc_asr_deleted)

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
                toaster.show(ttsUpdated, ToastType.Success)
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
                toaster.show(ttsDeleted, ToastType.Success)
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
                toaster.show(asrUpdated, ToastType.Success)
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
                toaster.show(asrDeleted, ToastType.Success)
            },
        )
        return
    }

    // TTS 和 ASR 共用同一个列表，标题必须整体唯一，否则 LazyColumn 的 key 会冲突。
    val systemTtsLabel = stringResource(R.string.wear_svc_system_tts)
    val allTitles = numberedTitles(
        ttsProviders.map { it.name.ifBlank { it.typeLabel(systemTtsLabel) } } +
            asrProviders.map { it.name.ifBlank { it.typeLabel() } }
    )
    val ttsTitles = allTitles.take(ttsProviders.size)
    val asrTitles = allTitles.drop(ttsProviders.size)
    val currentTts = ttsProviders.find { it.id == settings.selectedTTSProviderId }
    val currentAsr = asrProviders.find { it.id == settings.selectedASRProviderId }
    val noneLabel = stringResource(R.string.wear_svc_none)
    val addTtsLabel = stringResource(R.string.wear_svc_tts_add)
    val addAsrLabel = stringResource(R.string.wear_svc_asr_add)
    val activeLabel = stringResource(R.string.wear_svc_active)
    val currentTtsLabel = if (currentTts != null) {
        currentTts.name.ifBlank { currentTts.typeLabel(systemTtsLabel) }
    } else {
        systemTtsLabel
    }
    val currentAsrLabel = if (currentAsr != null) {
        currentAsr.name.ifBlank { currentAsr.typeLabel() }
    } else {
        noneLabel
    }

    WearScreen(title = stringResource(R.string.wear_svc_speech_title), onBack = onBack) {
        section(stringResource(R.string.wear_svc_tts))
        infoItem(stringResource(R.string.wear_svc_current_tts), currentTtsLabel)
        ttsProviders.forEachIndexed { index, provider ->
            navItem(
                title = ttsTitles[index],
                subtitle = if (provider.id == settings.selectedTTSProviderId) {
                    stringResource(R.string.wear_svc_active_state, provider.typeLabel(systemTtsLabel))
                } else {
                    provider.typeLabel(systemTtsLabel)
                },
                icon = WearIcons.Volume,
                trailingText = if (provider.id == settings.selectedTTSProviderId) activeLabel else null,
                onClick = { editingTtsId = provider.id },
            )
        }
        buttonItem(label = addTtsLabel, icon = WearIcons.Add, tonal = true) {
            showAddTts = true
        }

        section(stringResource(R.string.wear_svc_asr))
        infoItem(stringResource(R.string.wear_svc_current_asr), currentAsrLabel)
        if (asrProviders.isEmpty()) {
            textItem(
                text = stringResource(R.string.wear_svc_asr_empty),
                maxLines = 3,
                small = true,
                muted = true,
            )
        }
        asrProviders.forEachIndexed { index, provider ->
            navItem(
                title = asrTitles[index],
                subtitle = if (provider.id == settings.selectedASRProviderId) {
                    stringResource(R.string.wear_svc_active_state, provider.typeLabel())
                } else {
                    provider.typeLabel()
                },
                icon = WearIcons.Mic,
                trailingText = if (provider.id == settings.selectedASRProviderId) activeLabel else null,
                onClick = { editingAsrId = provider.id },
            )
        }
        buttonItem(label = addAsrLabel, icon = WearIcons.Add, tonal = true) {
            showAddAsr = true
        }
        textItem(
            text = stringResource(R.string.wear_svc_speech_phone_hint),
            maxLines = 3,
            small = true,
            muted = true,
        )
    }

    if (showAddTts) {
        WearOptionDialog(
            visible = true,
            title = addTtsLabel,
            options = ttsFactories.map { if (it.isSystem) systemTtsLabel else it.label },
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
            title = addAsrLabel,
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

    val systemTtsLabel = stringResource(R.string.wear_svc_system_tts)
    val typeLabel = provider.typeLabel(systemTtsLabel)
    val notSetLabel = stringResource(R.string.wear_svc_not_set)
    val currentlyActive = stringResource(R.string.wear_svc_currently_active)
    val tapToActivate = stringResource(R.string.wear_svc_tap_to_activate)

    WearScreen(title = provider.name.ifBlank { typeLabel }, onBack = onBack) {
        infoItem(stringResource(R.string.wear_svc_type), typeLabel)
        radioItem(
            title = stringResource(R.string.wear_svc_tts_use),
            subtitle = if (selected) currentlyActive else tapToActivate,
            selected = selected,
            onSelect = onSelect,
        )
        navItem(
            title = stringResource(R.string.wear_svc_name),
            subtitle = provider.name.ifBlank { notSetLabel },
            icon = WearIcons.Settings,
            onClick = { editField = TtsField.NAME },
        )
        val apiKey = provider.apiKeyOrNull()
        if (apiKey != null) {
            navItem(
                title = stringResource(R.string.wear_svc_api_key),
                subtitle = if (apiKey.isBlank()) notSetLabel else maskSecret(apiKey),
                icon = WearIcons.Cpu,
                onClick = { editField = TtsField.API_KEY },
            )
        }
        val baseUrl = provider.baseUrlOrNull()
        if (baseUrl != null) {
            navItem(
                title = stringResource(R.string.wear_svc_base_url),
                subtitle = baseUrl,
                icon = WearIcons.Web,
                onClick = { editField = TtsField.BASE_URL },
            )
        }
        if (provider is TTSProviderSetting.SystemTTS) {
            textItem(stringResource(R.string.wear_svc_system_tts_hint), maxLines = 2, small = true, muted = true)
        }
        if (canDelete) {
            buttonItem(label = stringResource(R.string.wear_svc_delete_provider), icon = WearIcons.Delete, tonal = true) {
                confirmDelete = true
            }
        }
    }

    val field = editField
    if (field != null) {
        WearInputDialog(
            visible = true,
            title = when (field) {
                TtsField.NAME -> stringResource(R.string.wear_svc_provider_name)
                TtsField.API_KEY -> stringResource(R.string.wear_svc_api_key)
                TtsField.BASE_URL -> stringResource(R.string.wear_svc_base_url)
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
        title = stringResource(R.string.wear_svc_delete_provider_title),
        message = deleteProviderMessage(provider.name),
        confirmLabel = stringResource(R.string.wear_common_delete),
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

    val typeLabel = provider.typeLabel()
    val endpointLabel = provider.endpointLabel()
    val notSetLabel = stringResource(R.string.wear_svc_not_set)
    val currentlyActive = stringResource(R.string.wear_svc_currently_active)
    val tapToActivate = stringResource(R.string.wear_svc_tap_to_activate)

    WearScreen(title = provider.name.ifBlank { typeLabel }, onBack = onBack) {
        infoItem(stringResource(R.string.wear_svc_type), typeLabel)
        radioItem(
            title = stringResource(R.string.wear_svc_asr_use),
            subtitle = if (selected) currentlyActive else tapToActivate,
            selected = selected,
            onSelect = onSelect,
        )
        navItem(
            title = stringResource(R.string.wear_svc_name),
            subtitle = provider.name.ifBlank { notSetLabel },
            icon = WearIcons.Settings,
            onClick = { editField = AsrField.NAME },
        )
        navItem(
            title = stringResource(R.string.wear_svc_api_key),
            subtitle = if (provider.apiKeyOrNull().isBlank()) notSetLabel else maskSecret(provider.apiKeyOrNull()),
            icon = WearIcons.Cpu,
            onClick = { editField = AsrField.API_KEY },
        )
        navItem(
            title = endpointLabel,
            subtitle = provider.endpointOrNull().ifBlank { notSetLabel },
            icon = WearIcons.Web,
            onClick = { editField = AsrField.ENDPOINT },
        )
        textItem(
            text = stringResource(R.string.wear_svc_asr_phone_hint),
            maxLines = 2,
            small = true,
            muted = true,
        )
        buttonItem(label = stringResource(R.string.wear_svc_delete_provider), icon = WearIcons.Delete, tonal = true) {
            confirmDelete = true
        }
    }

    val field = editField
    if (field != null) {
        WearInputDialog(
            visible = true,
            title = when (field) {
                AsrField.NAME -> stringResource(R.string.wear_svc_provider_name)
                AsrField.API_KEY -> stringResource(R.string.wear_svc_api_key)
                AsrField.ENDPOINT -> endpointLabel
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
        title = stringResource(R.string.wear_svc_delete_provider_title),
        message = deleteProviderMessage(provider.name),
        confirmLabel = stringResource(R.string.wear_common_delete),
        danger = true,
        onConfirm = {
            confirmDelete = false
            onDelete()
        },
        onDismiss = { confirmDelete = false },
    )
}

@Composable
private fun deleteProviderMessage(name: String): String {
    val thisProvider = stringResource(R.string.wear_svc_this_provider)
    return stringResource(R.string.wear_svc_speech_delete_message, name.ifBlank { thisProvider })
}

@Composable
private fun numberedTitles(names: List<String>): List<String> {
    val unnamed = stringResource(R.string.wear_svc_unnamed_provider)
    val seen = mutableMapOf<String, Int>()
    return names.map { name ->
        val label = name.ifBlank { unnamed }
        val count = (seen[label] ?: 0) + 1
        seen[label] = count
        if (count == 1) label else "$label #$count"
    }
}

@Composable
private fun maskSecret(value: String): String = when {
    value.isBlank() -> stringResource(R.string.wear_svc_not_set)
    value.length <= 4 -> "••••"
    else -> "••••${value.takeLast(4)}"
}

private fun TTSProviderSetting.typeLabel(systemTts: String): String = when (this) {
    is TTSProviderSetting.OpenAI -> "OpenAI"
    is TTSProviderSetting.Gemini -> "Gemini"
    is TTSProviderSetting.SystemTTS -> systemTts
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

@Composable
private fun ASRProviderSetting.endpointLabel(): String = when (this) {
    is ASRProviderSetting.OpenAIRealtime -> stringResource(R.string.wear_svc_websocket_url)
    is ASRProviderSetting.DashScope -> stringResource(R.string.wear_svc_websocket_url)
    is ASRProviderSetting.Volcengine -> stringResource(R.string.wear_svc_websocket_url)
    is ASRProviderSetting.MiMo -> stringResource(R.string.wear_svc_base_url)
    is ASRProviderSetting.Step -> stringResource(R.string.wear_svc_base_url)
    is ASRProviderSetting.DeepSeek -> stringResource(R.string.wear_svc_base_url)
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
