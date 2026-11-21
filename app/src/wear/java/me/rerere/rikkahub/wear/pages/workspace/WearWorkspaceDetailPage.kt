package me.rerere.rikkahub.wear.pages.workspace

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.rikkahub.ui.context.LocalToaster
import me.rerere.rikkahub.ui.pages.extensions.workspace.WorkspaceDetailVM
import me.rerere.rikkahub.wear.WearIcons
import me.rerere.rikkahub.wear.components.WearInputDialog
import me.rerere.rikkahub.wear.components.WearScreen
import me.rerere.rikkahub.wear.components.buttonItem
import me.rerere.rikkahub.wear.components.infoItem
import me.rerere.rikkahub.wear.components.section
import me.rerere.rikkahub.wear.components.textItem
import me.rerere.rikkahub.wear.components.toggleItem
import me.rerere.ui.toast.ToastType
import me.rerere.workspace.RootfsInstallStage
import me.rerere.workspace.WorkspaceShellStatus
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.uuid.Uuid

@Composable
fun WearWorkspaceDetailPage(
    workspaceId: Uuid,
    onBack: () -> Unit,
    onOpenTerminal: (Uuid) -> Unit,
) {
    val vm: WorkspaceDetailVM = koinViewModel(
        parameters = { parametersOf(workspaceId.toString()) }
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val installProgress by vm.installProgress.collectAsStateWithLifecycle()
    val installError by vm.installError.collectAsStateWithLifecycle()
    val toaster = LocalToaster.current
    var showInstallUrl by remember { mutableStateOf(false) }

    LaunchedEffect(installError) {
        installError?.let { message ->
            toaster.show(message, ToastType.Error)
            vm.dismissInstallError()
        }
    }

    val workspace = state.workspace

    WearScreen(title = workspace?.name ?: "Workspace", onBack = onBack) {
        if (workspace == null) {
            textItem("Loading workspace…")
            return@WearScreen
        }

        section("Info")
        infoItem("Root", workspace.root)
        infoItem("Kind", if (workspace.isSsh()) "SSH" else "Local · PRoot")
        infoItem("Shell", shellStatusText(workspace.shellStatus))
        val rootfsCapable = workspace.capabilities().rootfs
        if (rootfsCapable) {
            val installing = installProgress != null ||
                workspace.shellStatus == WorkspaceShellStatus.INSTALLING.name
            val progress = installProgress
            if (progress != null) {
                textItem(progressText(progress.stage, progress.bytesRead, progress.totalBytes))
            }
            if (installing) {
                textItem("Installing rootfs — keep the app open. Other features are unaffected.")
            }
            buttonItem(
                label = when {
                    installing -> "Installing…"
                    workspace.shellStatus == WorkspaceShellStatus.READY.name -> "Reinstall rootfs"
                    else -> "Install rootfs"
                },
                icon = WearIcons.Download,
                enabled = !installing,
            ) { showInstallUrl = true }
            buttonItem(label = "Open console", icon = WearIcons.Play) {
                onOpenTerminal(workspaceId)
            }
        }
        if (workspace.isSsh()) {
            buttonItem(label = "Open SSH console", icon = WearIcons.Play) {
                onOpenTerminal(workspaceId)
            }
        }
        section("Compatibility")
        toggleItem(
            title = "Shell compatibility mode",
            subtitle = "Disable PRoot seccomp acceleration for broken kernels/devices",
            checked = workspace.shellCompatibilityMode,
            onCheckedChange = vm::setShellCompatibilityMode,
        )
        textItem(
            "Default rootfs: Alpine minirootfs for this ABI (small, ~3-4MB download). Tap Install rootfs to use it, or enter any tar/tar.gz URL.",
            small = true,
            muted = true,
        )
    }

    if (showInstallUrl) {
        WearInputDialog(
            visible = true,
            title = "Rootfs archive URL",
            initialValue = defaultRootfsUrl(),
            singleLine = false,
            placeholder = "https://…/ubuntu-base-…tar.gz",
            onConfirm = { url ->
                showInstallUrl = false
                if (url.isNotBlank()) vm.installRootfs(url.trim())
            },
            onDismiss = { showInstallUrl = false },
        )
    }
}

private fun shellStatusText(status: String): String = when (status) {
    WorkspaceShellStatus.READY.name -> "ready"
    WorkspaceShellStatus.INSTALLING.name -> "installing"
    WorkspaceShellStatus.BROKEN.name -> "broken"
    else -> "not installed"
}

private fun progressText(stage: RootfsInstallStage, bytesRead: Long, totalBytes: Long?): String = when (stage) {
    RootfsInstallStage.DOWNLOADING -> if (totalBytes != null && totalBytes > 0) {
        "Downloading ${bytesRead / 1024 / 1024}MB / ${totalBytes / 1024 / 1024}MB"
    } else {
        "Downloading ${bytesRead / 1024 / 1024}MB"
    }
    RootfsInstallStage.EXTRACTING -> "Extracting rootfs…"
    RootfsInstallStage.INSTALLED -> "Rootfs installed"
}

/**
 * rootfs 不打包进 APK，按进程 ABI 选择 Alpine minirootfs（约 3-4MB，官方 CDN）。
 *
 * 手机端页面的默认镜像是 Ubuntu base（约 27MB）；手表存储和流量都紧张，默认换成 Alpine。
 * Alpine 没有 bash，WorkspaceManager.rootfsShell 已经会回退到 /bin/sh，RootfsPatcher 也处理了
 * Alpine 的 busybox 软链。用户仍可在安装对话框里换成任意 tar/tar.gz/tar.xz 地址。
 */
internal fun defaultRootfsUrl(): String {
    val arch = when (val abi = Build.SUPPORTED_ABIS.firstOrNull().orEmpty()) {
        "arm64-v8a", "aarch64" -> "aarch64"
        "armeabi-v7a", "arm", "armeabi" -> "armv7"
        "x86_64" -> "x86_64"
        "x86" -> "x86"
        else -> if (abi.startsWith("arm64")) "aarch64" else "armv7"
    }
    return "https://dl-cdn.alpinelinux.org/alpine/v3.22/releases/$arch/alpine-minirootfs-3.22.8-$arch.tar.gz"
}
