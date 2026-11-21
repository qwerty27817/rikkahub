package me.rerere.rikkahub.wear.pages.workspace

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.rikkahub.R
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

    // WearScreen 的 content 是普通 DSL builder（非 @Composable），字符串要先在外部取好。
    val fallbackTitle = stringResource(R.string.wear_ws_title)
    val loadingLabel = stringResource(R.string.wear_ws_loading)
    val infoSection = stringResource(R.string.wear_ws_section_info)
    val rootLabel = stringResource(R.string.wear_ws_info_root)
    val kindLabel = stringResource(R.string.wear_ws_info_kind)
    val shellLabel = stringResource(R.string.wear_ws_info_shell)
    val sshKind = stringResource(R.string.wear_ws_kind_ssh)
    val localProotKind = stringResource(R.string.wear_ws_kind_local_proot)
    val shellStatusValue = shellStatusText(workspace?.shellStatus.orEmpty())
    val installHint = stringResource(R.string.wear_ws_install_hint)
    val installingLabel = stringResource(R.string.wear_ws_install_installing)
    val reinstallLabel = stringResource(R.string.wear_ws_reinstall_rootfs)
    val installLabel = stringResource(R.string.wear_ws_install_rootfs)
    val openConsoleLabel = stringResource(R.string.wear_ws_open_console)
    val openSshConsoleLabel = stringResource(R.string.wear_ws_open_ssh_console)
    val compatibilitySection = stringResource(R.string.wear_ws_section_compatibility)
    val compatibilityTitle = stringResource(R.string.wear_ws_compat_title)
    val compatibilitySubtitle = stringResource(R.string.wear_ws_compat_subtitle)
    val defaultRootfsHint = stringResource(R.string.wear_ws_default_rootfs_hint)
    val rootfsUrlTitle = stringResource(R.string.wear_ws_rootfs_url_title)
    val rootfsUrlPlaceholder = stringResource(R.string.wear_ws_rootfs_url_placeholder)
    val progress = installProgress
    val progressLabel = if (progress != null) {
        progressText(progress.stage, progress.bytesRead, progress.totalBytes)
    } else {
        null
    }

    WearScreen(title = workspace?.name ?: fallbackTitle, onBack = onBack) {
        if (workspace == null) {
            textItem(loadingLabel)
            return@WearScreen
        }

        section(infoSection)
        infoItem(rootLabel, workspace.root)
        infoItem(kindLabel, if (workspace.isSsh()) sshKind else localProotKind)
        infoItem(shellLabel, shellStatusValue)
        val rootfsCapable = workspace.capabilities().rootfs
        if (rootfsCapable) {
            val installing = installProgress != null ||
                workspace.shellStatus == WorkspaceShellStatus.INSTALLING.name
            if (progressLabel != null) {
                textItem(progressLabel)
            }
            if (installing) {
                textItem(installHint)
            }
            buttonItem(
                label = when {
                    installing -> installingLabel
                    workspace.shellStatus == WorkspaceShellStatus.READY.name -> reinstallLabel
                    else -> installLabel
                },
                icon = WearIcons.Download,
                enabled = !installing,
            ) { showInstallUrl = true }
            buttonItem(label = openConsoleLabel, icon = WearIcons.Play) {
                onOpenTerminal(workspaceId)
            }
        }
        if (workspace.isSsh()) {
            buttonItem(label = openSshConsoleLabel, icon = WearIcons.Play) {
                onOpenTerminal(workspaceId)
            }
        }
        section(compatibilitySection)
        toggleItem(
            title = compatibilityTitle,
            subtitle = compatibilitySubtitle,
            checked = workspace.shellCompatibilityMode,
            onCheckedChange = vm::setShellCompatibilityMode,
        )
        textItem(
            defaultRootfsHint,
            small = true,
            muted = true,
        )
    }

    if (showInstallUrl) {
        WearInputDialog(
            visible = true,
            title = rootfsUrlTitle,
            initialValue = defaultRootfsUrl(),
            singleLine = false,
            placeholder = rootfsUrlPlaceholder,
            onConfirm = { url ->
                showInstallUrl = false
                if (url.isNotBlank()) vm.installRootfs(url.trim())
            },
            onDismiss = { showInstallUrl = false },
        )
    }
}

@Composable
private fun shellStatusText(status: String): String = when (status) {
    WorkspaceShellStatus.READY.name -> stringResource(R.string.wear_ws_status_ready)
    WorkspaceShellStatus.INSTALLING.name -> stringResource(R.string.wear_ws_status_installing)
    WorkspaceShellStatus.BROKEN.name -> stringResource(R.string.wear_ws_status_broken)
    else -> stringResource(R.string.wear_ws_status_not_installed)
}

@Composable
private fun progressText(stage: RootfsInstallStage, bytesRead: Long, totalBytes: Long?): String = when (stage) {
    RootfsInstallStage.DOWNLOADING -> if (totalBytes != null && totalBytes > 0) {
        stringResource(
            R.string.wear_ws_downloading_total,
            bytesRead / 1024 / 1024,
            totalBytes / 1024 / 1024,
        )
    } else {
        stringResource(R.string.wear_ws_downloading, bytesRead / 1024 / 1024)
    }
    RootfsInstallStage.EXTRACTING -> stringResource(R.string.wear_ws_extracting)
    RootfsInstallStage.INSTALLED -> stringResource(R.string.wear_ws_rootfs_installed)
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
