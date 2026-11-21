package me.rerere.rikkahub.wear.pages.workspace

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.db.entity.WorkspaceEntity
import me.rerere.rikkahub.ui.pages.extensions.workspace.WorkspaceVM
import me.rerere.rikkahub.wear.WearIcons
import me.rerere.rikkahub.wear.components.WearConfirmDialog
import me.rerere.rikkahub.wear.components.WearInputDialog
import me.rerere.rikkahub.wear.components.WearScreen
import me.rerere.rikkahub.wear.components.buttonItem
import me.rerere.rikkahub.wear.components.navItem
import me.rerere.rikkahub.wear.components.section
import me.rerere.rikkahub.wear.components.textItem
import me.rerere.workspace.WorkspaceShellStatus
import org.koin.androidx.compose.koinViewModel
import kotlin.uuid.Uuid

@Composable
fun WearWorkspacesPage(
    onBack: () -> Unit,
    onOpen: (Uuid) -> Unit,
) {
    val vm: WorkspaceVM = koinViewModel()
    val workspaces by vm.workspaces.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<WorkspaceEntity?>(null) }

    // WearScreen 的 content 是普通 DSL builder（非 @Composable），字符串要先在外部取好。
    val title = stringResource(R.string.wear_ws_title)
    val intro = stringResource(R.string.wear_ws_intro)
    val emptyLabel = stringResource(R.string.wear_ws_empty)
    val workspacesSection = stringResource(R.string.wear_ws_section_workspaces)
    val sshKind = stringResource(R.string.wear_ws_kind_ssh)
    val localKind = stringResource(R.string.wear_ws_kind_local)
    val createWorkspaceLabel = stringResource(R.string.wear_ws_create)
    val deleteWorkspaceLabel = stringResource(R.string.wear_ws_delete)
    val deleteMessage = stringResource(R.string.wear_ws_delete_message)
    val deleteConfirmLabel = stringResource(R.string.wear_ws_delete_confirm)
    val namePlaceholder = stringResource(R.string.wear_ws_create_placeholder)
    val shellStatusLabels = mapOf(
        WorkspaceShellStatus.READY.name to stringResource(R.string.wear_ws_status_rootfs_ready),
        WorkspaceShellStatus.INSTALLING.name to stringResource(R.string.wear_ws_status_installing),
        WorkspaceShellStatus.BROKEN.name to stringResource(R.string.wear_ws_status_broken),
    )
    val noRootfsLabel = stringResource(R.string.wear_ws_status_no_rootfs)

    WearScreen(title = title, onBack = onBack) {
        textItem(intro)
        if (workspaces.isEmpty()) {
            textItem(emptyLabel)
        }
        section(workspacesSection)
        workspaces.forEach { workspace ->
            val statusLabel = shellStatusLabels[workspace.shellStatus] ?: noRootfsLabel
            navItem(
                title = workspace.name,
                subtitle = "$statusLabel · ${if (workspace.isSsh()) sshKind else localKind}",
                icon = WearIcons.Folder,
                onClick = { runCatching { Uuid.parse(workspace.id) }.getOrNull()?.let(onOpen) },
            )
        }
        buttonItem(label = createWorkspaceLabel, icon = WearIcons.Add) { showCreate = true }
        if (workspaces.isNotEmpty()) {
            buttonItem(label = deleteWorkspaceLabel, icon = WearIcons.Delete, tonal = true) {
                pendingDelete = workspaces.lastOrNull()
            }
        }
    }

    if (showCreate) {
        WearInputDialog(
            visible = true,
            title = createWorkspaceLabel,
            initialValue = "",
            singleLine = true,
            placeholder = namePlaceholder,
            onConfirm = { name ->
                showCreate = false
                if (name.isNotBlank()) vm.create(name)
            },
            onDismiss = { showCreate = false },
        )
    }

    pendingDelete?.let { workspace ->
        WearConfirmDialog(
            visible = true,
            title = stringResource(R.string.wear_ws_delete_title, workspace.name),
            message = deleteMessage,
            confirmLabel = deleteConfirmLabel,
            danger = true,
            onConfirm = {
                pendingDelete = null
                vm.delete(workspace)
            },
            onDismiss = { pendingDelete = null },
        )
    }
}
