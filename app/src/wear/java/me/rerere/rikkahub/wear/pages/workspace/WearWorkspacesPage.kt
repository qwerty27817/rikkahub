package me.rerere.rikkahub.wear.pages.workspace

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

    WearScreen(title = "Workspace", onBack = onBack) {
        textItem(
            "Workspaces run a Linux rootfs with PRoot. The image is downloaded on demand, " +
                "so everything else keeps working offline."
        )
        if (workspaces.isEmpty()) {
            textItem("No workspace yet.")
        }
        section("Workspaces")
        workspaces.forEach { workspace ->
            navItem(
                title = workspace.name,
                subtitle = shellStatusLabel(workspace.shellStatus) +
                    if (workspace.isSsh()) " · SSH" else " · local",
                icon = WearIcons.Folder,
                onClick = { runCatching { Uuid.parse(workspace.id) }.getOrNull()?.let(onOpen) },
            )
        }
        buttonItem(label = "New workspace", icon = WearIcons.Add) { showCreate = true }
        if (workspaces.isNotEmpty()) {
            buttonItem(label = "Delete workspace", icon = WearIcons.Delete, tonal = true) {
                pendingDelete = workspaces.lastOrNull()
            }
        }
    }

    if (showCreate) {
        WearInputDialog(
            visible = true,
            title = "New workspace",
            initialValue = "",
            singleLine = true,
            placeholder = "linux",
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
            title = "Delete ${workspace.name}?",
            message = "The rootfs and files inside this workspace will be removed.",
            confirmLabel = "Delete",
            danger = true,
            onConfirm = {
                pendingDelete = null
                vm.delete(workspace)
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

private fun shellStatusLabel(status: String): String = when (status) {
    WorkspaceShellStatus.READY.name -> "rootfs ready"
    WorkspaceShellStatus.INSTALLING.name -> "installing"
    WorkspaceShellStatus.BROKEN.name -> "broken"
    else -> "no rootfs"
}
