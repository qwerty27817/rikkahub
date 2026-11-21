package me.rerere.rikkahub.wear.pages.backup

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.rerere.asr.ASRProviderSetting
import me.rerere.rikkahub.data.ai.mcp.McpServerConfig
import me.rerere.rikkahub.data.datastore.DisplaySetting
import me.rerere.rikkahub.data.datastore.NetworkSetting
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.data.datastore.WebDavConfig
import me.rerere.rikkahub.data.sync.s3.S3Config
import me.rerere.rikkahub.ui.context.LocalSettings
import me.rerere.rikkahub.ui.context.LocalToaster
import me.rerere.rikkahub.ui.pages.backup.BackupVM
import me.rerere.rikkahub.ui.theme.CustomTheme
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
import me.rerere.rikkahub.wear.components.toggleItem
import me.rerere.mediagen.provider.MediaGenerationProviderSetting
import me.rerere.search.SearchCommonOptions
import me.rerere.search.SearchServiceOptions
import me.rerere.tts.provider.TTSProviderSetting
import me.rerere.ui.toast.ToastType
import org.koin.androidx.compose.koinViewModel
import kotlin.uuid.Uuid

private const val QR_PREFIX = "RIKKAHUB-BACKUP-V1:"
private const val QR_PIXEL_SIZE = 480
private const val QR_MODE_FULL = "FULL"
private const val QR_MODE_WATCH = "WATCH"

private val backupJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
}

private enum class BackupField {
    WEBDAV_URL,
    WEBDAV_USERNAME,
    WEBDAV_PASSWORD,
    WEBDAV_PATH,
    S3_ENDPOINT,
    S3_BUCKET,
    S3_REGION,
    S3_ACCESS_KEY,
    S3_SECRET_KEY,
}

private enum class ImportType(val label: String) {
    LOCAL("Local RikkaHub backup"),
    CHATBOX("Chatbox backup"),
    CHERRY("Cherry Studio providers"),
    DEEPSEEK("DeepSeek data"),
}

/** QR 传输用的小体积设置快照：只带手表端能编辑的字段，导入时合并而不是整份覆盖。 */
@Serializable
private data class WatchSettingsPayload(
    val dynamicColor: Boolean = true,
    val themeId: String = "",
    val customThemes: List<CustomTheme> = emptyList(),
    val displaySetting: DisplaySetting = DisplaySetting(),
    val networkSetting: NetworkSetting = NetworkSetting(),
    val searchServices: List<SearchServiceOptions> = emptyList(),
    val searchCommonOptions: SearchCommonOptions = SearchCommonOptions(),
    val searchServiceSelected: Int = 0,
    val mcpServers: List<McpServerConfig> = emptyList(),
    val ttsProviders: List<TTSProviderSetting> = emptyList(),
    val selectedTTSProviderId: Uuid? = null,
    val asrProviders: List<ASRProviderSetting> = emptyList(),
    val selectedASRProviderId: Uuid? = null,
    val mediaGenerationProviders: List<MediaGenerationProviderSetting> = emptyList(),
    val imageGenerationModelId: Uuid? = null,
    val webServerEnabled: Boolean = false,
    val webServerPort: Int = 8080,
    val webServerJwtEnabled: Boolean = false,
    val webServerAccessPassword: String = "",
    val webServerLocalhostOnly: Boolean = false,
    val webDavConfig: WebDavConfig = WebDavConfig(),
    val s3Config: S3Config = S3Config(),
)

@Serializable
private data class BackupQrPayload(
    val mode: String = QR_MODE_WATCH,
    val settings: Settings? = null,
    val watch: WatchSettingsPayload? = null,
)

/**
 * 手表端备份页：本地文件导入/导出、QR 设置传输、WebDAV / S3 配置与远端备份。
 *
 * 本地导入沿用手机的 `BackupManager` 暂存机制（重启后生效），因此恢复成功后提示重启。
 * 相机不可用时，QR 导入改为手动粘贴文本。
 */
@Composable
fun WearBackupPage(
    onBack: () -> Unit,
) {
    val vm: BackupVM = koinViewModel()
    val settings = LocalSettings.current
    val toaster = LocalToaster.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val localItems by vm.localBackupItems.collectAsStateWithLifecycle()

    var busy by remember { mutableStateOf<String?>(null) }
    var pendingImportType by remember { mutableStateOf<ImportType?>(null) }
    var showImportType by remember { mutableStateOf(false) }
    var showQrImport by remember { mutableStateOf(false) }
    var showRestartHint by remember { mutableStateOf(false) }
    var deepSeekMessage by remember { mutableStateOf<String?>(null) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var qrNote by remember { mutableStateOf<String?>(null) }
    var qrError by remember { mutableStateOf<String?>(null) }
    var editField by remember { mutableStateOf<BackupField?>(null) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                busy = "Exporting backup…"
                runCatching { exportToUri(vm, context, uri) }
                    .onSuccess { toaster.show("Backup exported", ToastType.Success) }
                    .onFailure { toaster.show(it.message ?: "Backup export failed", ToastType.Error) }
                busy = null
            }
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        val type = pendingImportType ?: ImportType.LOCAL
        pendingImportType = null
        if (uri != null) {
            scope.launch {
                busy = "Importing backup…"
                runCatching {
                    when (type) {
                        ImportType.LOCAL -> {
                            val temp = copyToCache(context, uri, "rikkahub_restore")
                            try {
                                vm.restoreFromLocalFile(temp)
                            } finally {
                                temp.delete()
                            }
                            showRestartHint = true
                        }

                        ImportType.CHATBOX -> {
                            val temp = copyToCache(context, uri, "chatbox_restore")
                            try {
                                vm.restoreFromChatBox(temp)
                            } finally {
                                temp.delete()
                            }
                        }

                        ImportType.CHERRY -> {
                            val temp = copyToCache(context, uri, "cherry_restore")
                            try {
                                withContext(Dispatchers.IO) { vm.restoreFromCherryStudio(temp) }
                            } finally {
                                temp.delete()
                            }
                        }

                        ImportType.DEEPSEEK -> {
                            val result = vm.restoreFromDeepSeek(uri)
                            deepSeekMessage = buildString {
                                append("Imported ${result.importedConversations} conversations")
                                if (result.skippedExistingConversations > 0) {
                                    append(", skipped ${result.skippedExistingConversations} existing")
                                }
                                if (result.skippedEmptyConversations > 0) {
                                    append(", ${result.skippedEmptyConversations} empty")
                                }
                                if (result.missingAttachments > 0) {
                                    append(", ${result.missingAttachments} missing attachments")
                                }
                                append(".")
                            }
                        }
                    }
                }.onSuccess {
                    if (type != ImportType.LOCAL && type != ImportType.DEEPSEEK) {
                        toaster.show("Import finished", ToastType.Success)
                    }
                }.onFailure {
                    toaster.show(it.message ?: "Import failed", ToastType.Error)
                }
                busy = null
            }
        }
    }

    fun launchImport(type: ImportType) {
        pendingImportType = type
        openDocumentLauncher.launch(arrayOf("application/zip"))
    }

    fun generateQr() {
        scope.launch {
            busy = "Building QR code…"
            qrBitmap = null
            qrError = null
            runCatching { withContext(Dispatchers.Default) { buildBackupQr(settings) } }
                .onSuccess { (bitmap, note) ->
                    qrBitmap = bitmap
                    qrNote = note
                }
                .onFailure { qrError = it.message ?: "Could not encode backup QR" }
            busy = null
        }
    }

    fun runRemote(action: String, block: suspend () -> Unit) {
        scope.launch {
            busy = action
            runCatching { block() }
                .onSuccess { toaster.show("$action finished", ToastType.Success) }
                .onFailure { toaster.show(it.message ?: "$action failed", ToastType.Error) }
            busy = null
        }
    }

    val exportFileName = "rikkahub_backup_${
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
    }.zip"

    val webDavConfig = settings.webDavConfig
    val s3Config = settings.s3Config
    val lastBackup = formatTime(settings.backupReminderConfig.lastBackupTime)
    val qrImage = qrBitmap
    val currentQrError = qrError
    val currentQrNote = qrNote
    val currentBusy = busy
    val isIdle = currentBusy == null
    val importOptions = ImportType.entries.map { it.label }

    WearScreen(title = "Backup", onBack = onBack) {
        section("Local backup")
        toggleItem(
            title = "Chat records (database)",
            checked = WebDavConfig.BackupItem.DATABASE in localItems,
            enabled = isIdle,
            onCheckedChange = { checked -> updateLocalItem(vm, WebDavConfig.BackupItem.DATABASE, checked) },
        )
        toggleItem(
            title = "Files and attachments",
            checked = WebDavConfig.BackupItem.FILES in localItems,
            enabled = isIdle,
            onCheckedChange = { checked -> updateLocalItem(vm, WebDavConfig.BackupItem.FILES, checked) },
        )
        buttonItem(
            label = "Export backup file",
            icon = WearIcons.Download,
            enabled = isIdle,
        ) {
            createDocumentLauncher.launch(exportFileName)
        }
        buttonItem(
            label = "Restore from file",
            icon = WearIcons.Upload,
            tonal = true,
            enabled = isIdle,
        ) {
            showImportType = true
        }
        if (currentBusy != null) {
            textItem(text = currentBusy, maxLines = 2, small = true, muted = true)
        }

        section("QR transfer")
        textItem(
            text = "QR carries settings only (no chats or files). Paste the code below to import on another watch.",
            maxLines = 4,
            small = true,
            muted = true,
        )
        buttonItem(
            label = "Show settings QR",
            icon = WearIcons.Qr,
            enabled = isIdle,
        ) {
            generateQr()
        }
        if (qrImage != null) {
            item(key = "backup_qr") {
                Image(
                    bitmap = qrImage.asImageBitmap(),
                    contentDescription = "Backup QR code",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp)
                        .aspectRatio(1f),
                )
            }
            if (currentQrNote != null) {
                textItem(text = currentQrNote, maxLines = 2, small = true, muted = true)
            }
        }
        if (currentQrError != null) {
            textItem(text = currentQrError, maxLines = 3, small = true, muted = true)
        }
        buttonItem(
            label = "Import settings from QR text",
            icon = WearIcons.Qr,
            tonal = true,
            enabled = isIdle,
        ) {
            showQrImport = true
        }

        section("WebDAV")
        infoItem("Last backup", lastBackup)
        navItem(
            title = "WebDAV URL",
            subtitle = webDavConfig.url.ifBlank { "Not set" },
            icon = WearIcons.Web,
            onClick = { editField = BackupField.WEBDAV_URL },
        )
        navItem(
            title = "WebDAV username",
            subtitle = webDavConfig.username.ifBlank { "Not set" },
            icon = WearIcons.Settings,
            onClick = { editField = BackupField.WEBDAV_USERNAME },
        )
        navItem(
            title = "WebDAV password",
            subtitle = if (webDavConfig.password.isBlank()) "Not set" else "••••••",
            icon = WearIcons.Settings,
            onClick = { editField = BackupField.WEBDAV_PASSWORD },
        )
        navItem(
            title = "WebDAV folder",
            subtitle = webDavConfig.path,
            icon = WearIcons.Folder,
            onClick = { editField = BackupField.WEBDAV_PATH },
        )
        buttonItem(
            label = "Test WebDAV",
            tonal = true,
            enabled = isIdle,
        ) {
            runRemote("WebDAV connection") { vm.testWebDav() }
        }
        buttonItem(
            label = "Back up to WebDAV",
            icon = WearIcons.Upload,
            enabled = isIdle,
        ) {
            runRemote("WebDAV backup") { vm.backup() }
        }

        section("S3")
        navItem(
            title = "S3 endpoint",
            subtitle = s3Config.endpoint.ifBlank { "Not set" },
            icon = WearIcons.Server,
            onClick = { editField = BackupField.S3_ENDPOINT },
        )
        navItem(
            title = "S3 bucket",
            subtitle = s3Config.bucket.ifBlank { "Not set" },
            icon = WearIcons.Folder,
            onClick = { editField = BackupField.S3_BUCKET },
        )
        navItem(
            title = "S3 region",
            subtitle = s3Config.region.ifBlank { "auto" },
            icon = WearIcons.Web,
            onClick = { editField = BackupField.S3_REGION },
        )
        navItem(
            title = "S3 access key",
            subtitle = if (s3Config.accessKeyId.isBlank()) "Not set" else maskSecret(s3Config.accessKeyId),
            icon = WearIcons.Settings,
            onClick = { editField = BackupField.S3_ACCESS_KEY },
        )
        navItem(
            title = "S3 secret key",
            subtitle = if (s3Config.secretAccessKey.isBlank()) "Not set" else "••••••",
            icon = WearIcons.Settings,
            onClick = { editField = BackupField.S3_SECRET_KEY },
        )
        toggleItem(
            title = "S3 path-style URLs",
            checked = s3Config.pathStyle,
            enabled = isIdle,
            onCheckedChange = { checked ->
                vm.updateSettings { it.copy(s3Config = it.s3Config.copy(pathStyle = checked)) }
            },
        )
        buttonItem(
            label = "Test S3",
            tonal = true,
            enabled = isIdle,
        ) {
            runRemote("S3 connection") { vm.testS3() }
        }
        buttonItem(
            label = "Back up to S3",
            icon = WearIcons.Upload,
            enabled = isIdle,
        ) {
            runRemote("S3 backup") { vm.backupToS3() }
        }

        section("Other apps")
        buttonItem(
            label = "Import Chatbox backup",
            icon = WearIcons.File,
            enabled = isIdle,
        ) {
            launchImport(ImportType.CHATBOX)
        }
        buttonItem(
            label = "Import Cherry Studio providers",
            icon = WearIcons.File,
            enabled = isIdle,
        ) {
            launchImport(ImportType.CHERRY)
        }
        buttonItem(
            label = "Import DeepSeek data",
            icon = WearIcons.File,
            enabled = isIdle,
        ) {
            launchImport(ImportType.DEEPSEEK)
        }
        textItem(
            text = "Remote restore and full database restore run on the phone app.",
            maxLines = 3,
            small = true,
            muted = true,
        )
    }

    if (showImportType) {
        WearOptionDialog(
            visible = true,
            title = "Restore from file",
            options = importOptions,
            onSelect = { index ->
                val type = ImportType.entries.getOrNull(index)
                if (type != null) {
                    showImportType = false
                    launchImport(type)
                }
            },
            onDismiss = { showImportType = false },
        )
    }

    if (showQrImport) {
        WearInputDialog(
            visible = true,
            title = "Paste backup code",
            initialValue = "",
            placeholder = QR_PREFIX,
            singleLine = false,
            onConfirm = { text ->
                showQrImport = false
                scope.launch {
                    busy = "Importing settings…"
                    runCatching {
                        val payload = withContext(Dispatchers.Default) { decodeBackupPayload(text) }
                        when (payload.mode) {
                            QR_MODE_FULL -> {
                                val imported = payload.settings
                                    ?: throw IllegalArgumentException("Backup is missing settings")
                                vm.updateSettings { imported }
                            }

                            QR_MODE_WATCH -> {
                                val imported = payload.watch
                                    ?: throw IllegalArgumentException("Backup is missing settings")
                                vm.updateSettings { latest -> latest.applyWatchPayload(imported) }
                            }

                            else -> throw IllegalArgumentException("Unsupported backup code")
                        }
                    }.onSuccess {
                        toaster.show("Settings imported", ToastType.Success)
                    }.onFailure {
                        toaster.show(it.message ?: "Invalid backup code", ToastType.Error)
                    }
                    busy = null
                }
            },
            onDismiss = { showQrImport = false },
        )
    }

    val field = editField
    if (field != null) {
        WearInputDialog(
            visible = true,
            title = fieldTitle(field),
            initialValue = fieldValue(settings, field),
            singleLine = true,
            keyboardType = if (field == BackupField.WEBDAV_PASSWORD || field == BackupField.S3_SECRET_KEY) {
                KeyboardType.Password
            } else {
                KeyboardType.Text
            },
            onConfirm = { value ->
                editField = null
                vm.updateSettings { latest -> latest.withBackupField(field, value.trim()) }
                toaster.show("${fieldTitle(field)} saved", ToastType.Success)
            },
            onDismiss = { editField = null },
        )
    }

    WearConfirmDialog(
        visible = showRestartHint,
        title = "Restart required",
        message = "The restore is staged. Close RikkaHub completely and open it again to apply the backup.",
        confirmLabel = "OK",
        dismissLabel = "Later",
        onConfirm = { showRestartHint = false },
        onDismiss = { showRestartHint = false },
    )

    val currentDeepSeekMessage = deepSeekMessage
    if (currentDeepSeekMessage != null) {
        WearConfirmDialog(
            visible = true,
            title = "DeepSeek import finished",
            message = currentDeepSeekMessage,
            confirmLabel = "OK",
            dismissLabel = "Close",
            onConfirm = { deepSeekMessage = null },
            onDismiss = { deepSeekMessage = null },
        )
    }
}

private suspend fun exportToUri(vm: BackupVM, context: Context, uri: Uri) = withContext(Dispatchers.IO) {
    val file = vm.exportToFile()
    try {
        context.contentResolver.openOutputStream(uri)?.use { output ->
            FileInputStream(file).use { input -> input.copyTo(output) }
        } ?: throw IllegalStateException("Could not open selected destination")
    } finally {
        file.delete()
    }
}

private suspend fun copyToCache(context: Context, uri: Uri, prefix: String): File =
    withContext(Dispatchers.IO) {
        val temp = File(context.cacheDir, "${prefix}_${System.currentTimeMillis()}.zip")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(temp).use { output -> input.copyTo(output) }
        } ?: throw IllegalStateException("Could not open selected backup")
        temp
    }

private fun updateLocalItem(vm: BackupVM, item: WebDavConfig.BackupItem, enabled: Boolean) {
    val current = vm.localBackupItems.value
    val next = if (enabled) current + item else current - item
    vm.updateLocalBackupItems(next.distinct().sortedBy { it.ordinal })
}

private fun formatTime(epochMillis: Long): String = if (epochMillis <= 0L) {
    "Never"
} else {
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
}

private fun maskSecret(value: String): String = when {
    value.isBlank() -> "Not set"
    value.length <= 6 -> "••••"
    else -> "••••${value.takeLast(4)}"
}

private fun fieldTitle(field: BackupField): String = when (field) {
    BackupField.WEBDAV_URL -> "WebDAV URL"
    BackupField.WEBDAV_USERNAME -> "WebDAV username"
    BackupField.WEBDAV_PASSWORD -> "WebDAV password"
    BackupField.WEBDAV_PATH -> "WebDAV folder"
    BackupField.S3_ENDPOINT -> "S3 endpoint"
    BackupField.S3_BUCKET -> "S3 bucket"
    BackupField.S3_REGION -> "S3 region"
    BackupField.S3_ACCESS_KEY -> "S3 access key"
    BackupField.S3_SECRET_KEY -> "S3 secret key"
}

private fun fieldValue(settings: Settings, field: BackupField): String = when (field) {
    BackupField.WEBDAV_URL -> settings.webDavConfig.url
    BackupField.WEBDAV_USERNAME -> settings.webDavConfig.username
    BackupField.WEBDAV_PASSWORD -> settings.webDavConfig.password
    BackupField.WEBDAV_PATH -> settings.webDavConfig.path
    BackupField.S3_ENDPOINT -> settings.s3Config.endpoint
    BackupField.S3_BUCKET -> settings.s3Config.bucket
    BackupField.S3_REGION -> settings.s3Config.region
    BackupField.S3_ACCESS_KEY -> settings.s3Config.accessKeyId
    BackupField.S3_SECRET_KEY -> settings.s3Config.secretAccessKey
}

private fun Settings.withBackupField(field: BackupField, value: String): Settings = when (field) {
    BackupField.WEBDAV_URL -> copy(webDavConfig = webDavConfig.copy(url = value))
    BackupField.WEBDAV_USERNAME -> copy(webDavConfig = webDavConfig.copy(username = value))
    BackupField.WEBDAV_PASSWORD -> copy(webDavConfig = webDavConfig.copy(password = value))
    BackupField.WEBDAV_PATH -> copy(webDavConfig = webDavConfig.copy(path = value))
    BackupField.S3_ENDPOINT -> copy(s3Config = s3Config.copy(endpoint = value))
    BackupField.S3_BUCKET -> copy(s3Config = s3Config.copy(bucket = value))
    BackupField.S3_REGION -> copy(s3Config = s3Config.copy(region = value))
    BackupField.S3_ACCESS_KEY -> copy(s3Config = s3Config.copy(accessKeyId = value))
    BackupField.S3_SECRET_KEY -> copy(s3Config = s3Config.copy(secretAccessKey = value))
}

private fun Settings.toWatchPayload(): WatchSettingsPayload = WatchSettingsPayload(
    dynamicColor = dynamicColor,
    themeId = themeId,
    customThemes = customThemes,
    displaySetting = displaySetting,
    networkSetting = networkSetting,
    searchServices = searchServices,
    searchCommonOptions = searchCommonOptions,
    searchServiceSelected = searchServiceSelected,
    mcpServers = mcpServers,
    ttsProviders = ttsProviders,
    selectedTTSProviderId = selectedTTSProviderId,
    asrProviders = asrProviders,
    selectedASRProviderId = selectedASRProviderId,
    mediaGenerationProviders = mediaGenerationProviders,
    imageGenerationModelId = imageGenerationModelId,
    webServerEnabled = webServerEnabled,
    webServerPort = webServerPort,
    webServerJwtEnabled = webServerJwtEnabled,
    webServerAccessPassword = webServerAccessPassword,
    webServerLocalhostOnly = webServerLocalhostOnly,
    webDavConfig = webDavConfig,
    s3Config = s3Config,
)

private fun Settings.applyWatchPayload(payload: WatchSettingsPayload): Settings = copy(
    dynamicColor = payload.dynamicColor,
    themeId = payload.themeId.ifBlank { themeId },
    customThemes = payload.customThemes,
    displaySetting = payload.displaySetting,
    networkSetting = payload.networkSetting,
    searchServices = payload.searchServices.ifEmpty { searchServices },
    searchCommonOptions = payload.searchCommonOptions,
    searchServiceSelected = payload.searchServiceSelected
        .coerceIn(0, (payload.searchServices.size - 1).coerceAtLeast(0)),
    mcpServers = payload.mcpServers,
    ttsProviders = payload.ttsProviders.ifEmpty { ttsProviders },
    selectedTTSProviderId = payload.selectedTTSProviderId ?: selectedTTSProviderId,
    asrProviders = payload.asrProviders,
    selectedASRProviderId = payload.selectedASRProviderId,
    mediaGenerationProviders = payload.mediaGenerationProviders,
    imageGenerationModelId = payload.imageGenerationModelId ?: imageGenerationModelId,
    webServerEnabled = payload.webServerEnabled,
    webServerPort = payload.webServerPort,
    webServerJwtEnabled = payload.webServerJwtEnabled,
    webServerAccessPassword = payload.webServerAccessPassword,
    webServerLocalhostOnly = payload.webServerLocalhostOnly,
    webDavConfig = payload.webDavConfig,
    s3Config = payload.s3Config,
)

private fun encodePayload(payload: BackupQrPayload): String {
    val jsonText = backupJson.encodeToString(BackupQrPayload.serializer(), payload)
    val compressed = ByteArrayOutputStream().use { buffer ->
        GZIPOutputStream(buffer).use { gzip -> gzip.write(jsonText.toByteArray(Charsets.UTF_8)) }
        buffer.toByteArray()
    }
    return QR_PREFIX + Base64.encodeToString(compressed, Base64.NO_WRAP)
}

private fun decodeBackupPayload(text: String): BackupQrPayload {
    val trimmed = text.trim()
    require(trimmed.startsWith(QR_PREFIX)) { "Not a RikkaHub backup code" }
    val compressed = Base64.decode(trimmed.removePrefix(QR_PREFIX), Base64.NO_WRAP)
    val jsonText = GZIPInputStream(compressed.inputStream()).use { it.readBytes().decodeToString() }
    return backupJson.decodeFromString(BackupQrPayload.serializer(), jsonText)
}

/** 先尝试完整设置，超出 QR 容量时退化为手表可编辑设置的子集。 */
private fun buildBackupQr(settings: Settings): Pair<Bitmap, String> {
    val candidates = listOf(
        QR_MODE_FULL to BackupQrPayload(mode = QR_MODE_FULL, settings = settings),
        QR_MODE_WATCH to BackupQrPayload(mode = QR_MODE_WATCH, watch = settings.toWatchPayload()),
    )
    var failure: Throwable? = null
    for ((mode, payload) in candidates) {
        val encoded = encodePayload(payload)
        try {
            val note = if (mode == QR_MODE_FULL) {
                "QR contains the full settings snapshot."
            } else {
                "Full settings exceed QR capacity; this QR contains watch settings only."
            }
            return renderQr(encoded) to note
        } catch (e: Exception) {
            failure = e
        }
    }
    throw IllegalStateException("Settings are too large for a QR code. Use the file export instead.", failure)
}

private fun renderQr(payload: String): Bitmap {
    val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, QR_PIXEL_SIZE, QR_PIXEL_SIZE)
    val black = android.graphics.Color.BLACK
    val white = android.graphics.Color.WHITE
    val bitmap = createBitmap(QR_PIXEL_SIZE, QR_PIXEL_SIZE)
    for (x in 0 until QR_PIXEL_SIZE) {
        for (y in 0 until QR_PIXEL_SIZE) {
            bitmap.setPixel(x, y, if (matrix[x, y]) black else white)
        }
    }
    return bitmap
}
