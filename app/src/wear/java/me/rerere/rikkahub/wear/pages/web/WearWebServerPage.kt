package me.rerere.rikkahub.wear.pages.web

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.service.WebServerService
import me.rerere.rikkahub.ui.context.LocalSettings
import me.rerere.rikkahub.ui.context.LocalToaster
import me.rerere.rikkahub.wear.WearIcons
import me.rerere.rikkahub.wear.components.WearInputDialog
import me.rerere.rikkahub.wear.components.WearScreen
import me.rerere.rikkahub.wear.components.buttonItem
import me.rerere.rikkahub.wear.components.infoItem
import me.rerere.rikkahub.wear.components.navItem
import me.rerere.rikkahub.wear.components.section
import me.rerere.rikkahub.wear.components.textItem
import me.rerere.rikkahub.wear.components.toggleItem
import me.rerere.rikkahub.web.WebServerManager
import me.rerere.ui.toast.ToastType
import org.koin.compose.koinInject

/**
 * 手表端本地 Web 服务器设置。
 *
 * 开关走手机端同一个 [WebServerService]（前台服务 + [WebServerManager]），
 * 状态直接读 [WebServerManager.state]，端口/密码等与手机端共用同一份 Settings。
 */
@Composable
fun WearWebServerPage(
    onBack: () -> Unit,
) {
    val settings = LocalSettings.current
    val store: SettingsStore = koinInject()
    val manager: WebServerManager = koinInject()
    val scope = rememberCoroutineScope()
    val toaster = LocalToaster.current
    val context = LocalContext.current
    val state by manager.state.collectAsStateWithLifecycle()

    var editPort by remember { mutableStateOf(false) }
    var editPassword by remember { mutableStateOf(false) }

    val startFailedMessage = stringResource(R.string.wear_svc_web_start_failed)
    val stopFailedMessage = stringResource(R.string.wear_svc_web_stop_failed)
    val permissionDeniedMessage = stringResource(R.string.wear_svc_web_permission_denied)
    val notSetLabel = stringResource(R.string.wear_svc_not_set)
    val enabledLabel = stringResource(R.string.wear_svc_enabled)
    val disabledLabel = stringResource(R.string.wear_svc_disabled)
    val portError = stringResource(R.string.wear_svc_web_port_error)

    fun startServer() {
        val intent = Intent(context, WebServerService::class.java).apply {
            action = WebServerService.ACTION_START
            putExtra(WebServerService.EXTRA_PORT, settings.webServerPort)
            putExtra(WebServerService.EXTRA_LOCALHOST_ONLY, settings.webServerLocalhostOnly)
        }
        runCatching { context.startForegroundService(intent) }
            .onFailure { toaster.show(it.message ?: startFailedMessage, ToastType.Error) }
        scope.launch { store.update { it.copy(webServerEnabled = true) } }
    }

    fun stopServer() {
        val intent = Intent(context, WebServerService::class.java).apply {
            action = WebServerService.ACTION_STOP
        }
        runCatching { context.startService(intent) }
            .onFailure { toaster.show(it.message ?: stopFailedMessage, ToastType.Error) }
        scope.launch { store.update { it.copy(webServerEnabled = false) } }
    }

    val requiredPermissions = remember(settings.webServerLocalhostOnly) {
        buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (Build.VERSION.SDK_INT >= 37 && !settings.webServerLocalhostOnly) {
                add(Manifest.permission.ACCESS_LOCAL_NETWORK)
            }
        }.toTypedArray()
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it }) {
            startServer()
        } else {
            toaster.show(permissionDeniedMessage, ToastType.Error)
        }
    }

    WearScreen(title = stringResource(R.string.wear_svc_web_title), onBack = onBack) {
        infoItem(
            stringResource(R.string.wear_svc_web_status),
            when {
                state.isLoading -> stringResource(R.string.wear_svc_web_working)
                state.isRunning -> stringResource(R.string.wear_svc_web_running)
                else -> stringResource(R.string.wear_svc_web_stopped)
            },
        )
        if (state.isRunning) {
            infoItem(stringResource(R.string.wear_svc_web_local_address), "http://localhost:${state.port}")
            if (!state.localhostOnly) {
                infoItem(
                    stringResource(R.string.wear_svc_web_lan_address),
                    "http://${state.address ?: "localhost"}:${state.port}",
                )
                val hostname = state.hostname
                if (hostname != null) {
                    infoItem(stringResource(R.string.wear_svc_web_mdns_address), "http://$hostname:${state.port}")
                }
            }
        }
        val error = state.error
        if (error != null) {
            textItem(
                text = stringResource(R.string.wear_svc_web_error, error),
                maxLines = 4,
                small = true,
                muted = true,
            )
        }
        buttonItem(
            label = if (state.isRunning) {
                stringResource(R.string.wear_svc_web_stop)
            } else {
                stringResource(R.string.wear_svc_web_start)
            },
            icon = if (state.isRunning) WearIcons.Stop else WearIcons.Play,
            tonal = true,
            enabled = !state.isLoading,
        ) {
            if (state.isRunning) {
                stopServer()
            } else {
                permissionLauncher.launch(requiredPermissions)
            }
        }
        infoItem(
            stringResource(R.string.wear_svc_web_start_on_launch),
            if (settings.webServerEnabled) enabledLabel else disabledLabel,
        )

        section(stringResource(R.string.wear_svc_web_configuration))
        if (state.isRunning) {
            infoItem(stringResource(R.string.wear_svc_web_port), state.port.toString())
            textItem(
                text = stringResource(R.string.wear_svc_web_stop_to_edit),
                maxLines = 3,
                small = true,
                muted = true,
            )
        } else {
            navItem(
                title = stringResource(R.string.wear_svc_web_port),
                subtitle = stringResource(R.string.wear_svc_web_port_subtitle, settings.webServerPort),
                icon = WearIcons.Settings,
                onClick = { editPort = true },
            )
        }
        toggleItem(
            title = stringResource(R.string.wear_svc_web_localhost_only),
            checked = settings.webServerLocalhostOnly,
            subtitle = stringResource(R.string.wear_svc_web_localhost_only_desc),
            enabled = !state.isRunning,
            onCheckedChange = { checked ->
                scope.launch { store.update { it.copy(webServerLocalhostOnly = checked) } }
            },
        )
        navItem(
            title = stringResource(R.string.wear_svc_web_access_password),
            subtitle = if (settings.webServerAccessPassword.isBlank()) notSetLabel else "••••••",
            icon = WearIcons.Settings,
            onClick = { editPassword = true },
        )
        toggleItem(
            title = stringResource(R.string.wear_svc_web_jwt),
            checked = settings.webServerJwtEnabled,
            subtitle = stringResource(R.string.wear_svc_web_jwt_desc),
            enabled = settings.webServerJwtEnabled || settings.webServerAccessPassword.isNotBlank(),
            onCheckedChange = { checked ->
                scope.launch { store.update { it.copy(webServerJwtEnabled = checked) } }
            },
        )
        textItem(
            text = stringResource(R.string.wear_svc_web_jwt_hint),
            maxLines = 2,
            small = true,
            muted = true,
        )
    }

    if (editPort) {
        WearInputDialog(
            visible = true,
            title = stringResource(R.string.wear_svc_web_port_title),
            initialValue = settings.webServerPort.toString(),
            label = "1024 - 65535",
            keyboardType = KeyboardType.Number,
            onConfirm = { value ->
                editPort = false
                val port = value.trim().toIntOrNull()
                if (port != null && port in 1024..65535) {
                    scope.launch { store.update { it.copy(webServerPort = port) } }
                } else {
                    toaster.show(portError, ToastType.Error)
                }
            },
            onDismiss = { editPort = false },
        )
    }

    if (editPassword) {
        WearInputDialog(
            visible = true,
            title = stringResource(R.string.wear_svc_web_access_password),
            initialValue = settings.webServerAccessPassword,
            label = stringResource(R.string.wear_svc_web_password_label),
            singleLine = true,
            keyboardType = KeyboardType.Password,
            onConfirm = { value ->
                editPassword = false
                val password = value.trim()
                scope.launch {
                    store.update {
                        it.copy(
                            webServerAccessPassword = password,
                            webServerJwtEnabled = it.webServerJwtEnabled && password.isNotBlank(),
                        )
                    }
                }
            },
            onDismiss = { editPassword = false },
        )
    }
}
