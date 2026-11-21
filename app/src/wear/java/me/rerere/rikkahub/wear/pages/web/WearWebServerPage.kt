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
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
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

    fun startServer() {
        val intent = Intent(context, WebServerService::class.java).apply {
            action = WebServerService.ACTION_START
            putExtra(WebServerService.EXTRA_PORT, settings.webServerPort)
            putExtra(WebServerService.EXTRA_LOCALHOST_ONLY, settings.webServerLocalhostOnly)
        }
        runCatching { context.startForegroundService(intent) }
            .onFailure { toaster.show(it.message ?: "Could not start web server", ToastType.Error) }
        scope.launch { store.update { it.copy(webServerEnabled = true) } }
    }

    fun stopServer() {
        val intent = Intent(context, WebServerService::class.java).apply {
            action = WebServerService.ACTION_STOP
        }
        runCatching { context.startService(intent) }
            .onFailure { toaster.show(it.message ?: "Could not stop web server", ToastType.Error) }
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
            toaster.show("Permission denied; web server was not started", ToastType.Error)
        }
    }

    WearScreen(title = "Web server", onBack = onBack) {
        infoItem("Status", when {
            state.isLoading -> "Working…"
            state.isRunning -> "Running"
            else -> "Stopped"
        })
        if (state.isRunning) {
            infoItem("Local address", "http://localhost:${state.port}")
            if (!state.localhostOnly) {
                infoItem("LAN address", "http://${state.address ?: "localhost"}:${state.port}")
                val hostname = state.hostname
                if (hostname != null) {
                    infoItem("mDNS address", "http://$hostname:${state.port}")
                }
            }
        }
        val error = state.error
        if (error != null) {
            textItem(text = "Error: $error", maxLines = 4, small = true, muted = true)
        }
        buttonItem(
            label = if (state.isRunning) "Stop server" else "Start server",
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
            "Start on launch",
            if (settings.webServerEnabled) "Enabled" else "Disabled",
        )

        section("Configuration")
        if (state.isRunning) {
            infoItem("Port", state.port.toString())
            textItem("Stop the server to change port or localhost mode.", maxLines = 3, small = true, muted = true)
        } else {
            navItem(
                title = "Port",
                subtitle = "${settings.webServerPort} (1024 - 65535)",
                icon = WearIcons.Settings,
                onClick = { editPort = true },
            )
        }
        toggleItem(
            title = "Localhost only",
            checked = settings.webServerLocalhostOnly,
            subtitle = "Only serve on 127.0.0.1",
            enabled = !state.isRunning,
            onCheckedChange = { checked ->
                scope.launch { store.update { it.copy(webServerLocalhostOnly = checked) } }
            },
        )
        navItem(
            title = "Access password",
            subtitle = if (settings.webServerAccessPassword.isBlank()) "Not set" else "••••••",
            icon = WearIcons.Settings,
            onClick = { editPassword = true },
        )
        toggleItem(
            title = "JWT auth",
            checked = settings.webServerJwtEnabled,
            subtitle = "Sign requests with a JWT",
            enabled = settings.webServerJwtEnabled || settings.webServerAccessPassword.isNotBlank(),
            onCheckedChange = { checked ->
                scope.launch { store.update { it.copy(webServerJwtEnabled = checked) } }
            },
        )
        textItem(
            text = "JWT requires an access password.",
            maxLines = 2,
            small = true,
            muted = true,
        )
    }

    if (editPort) {
        WearInputDialog(
            visible = true,
            title = "Web server port",
            initialValue = settings.webServerPort.toString(),
            label = "1024 - 65535",
            keyboardType = KeyboardType.Number,
            onConfirm = { value ->
                editPort = false
                val port = value.trim().toIntOrNull()
                if (port != null && port in 1024..65535) {
                    scope.launch { store.update { it.copy(webServerPort = port) } }
                } else {
                    toaster.show("Port must be between 1024 and 65535", ToastType.Error)
                }
            },
            onDismiss = { editPort = false },
        )
    }

    if (editPassword) {
        WearInputDialog(
            visible = true,
            title = "Access password",
            initialValue = settings.webServerAccessPassword,
            label = "Leave empty to disable",
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
