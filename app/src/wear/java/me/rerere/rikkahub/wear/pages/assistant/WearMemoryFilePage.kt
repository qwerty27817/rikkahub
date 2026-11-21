package me.rerere.rikkahub.wear.pages.assistant

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.rikkahub.data.repository.MemoryWriteResult
import me.rerere.rikkahub.ui.context.LocalToaster
import me.rerere.rikkahub.ui.pages.memory.MemoryFileState
import me.rerere.rikkahub.ui.pages.memory.MemoryFileVM
import me.rerere.rikkahub.wear.WearIcons
import me.rerere.rikkahub.wear.components.WearConfirmDialog
import me.rerere.rikkahub.wear.components.WearScreen
import me.rerere.rikkahub.wear.components.WearTextField
import me.rerere.rikkahub.wear.components.buttonItem
import me.rerere.rikkahub.wear.components.infoItem
import me.rerere.rikkahub.wear.components.section
import me.rerere.rikkahub.wear.components.textItem
import me.rerere.ui.toast.ToastType
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * 单个记忆文件：读取、编辑、删除，以及新建时的路径输入。
 *
 * 手机版把 frontmatter 拆成多个输入框；手表上直接编辑整份 Markdown 文本。
 */
@Composable
fun WearMemoryFilePage(
    memoryId: String,
    path: String?,
    onBack: () -> Unit,
) {
    val vm: MemoryFileVM = koinViewModel(parameters = { parametersOf(memoryId, path) })
    val state by vm.state.collectAsStateWithLifecycle()
    val toaster = LocalToaster.current
    val isNew = path == null
    val file = (state as? MemoryFileState.Loaded)?.file

    var editing by remember(isNew) { mutableStateOf(isNew) }
    var draftContent by remember(file?.path) { mutableStateOf(file?.content.orEmpty()) }
    var draftPath by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val title = when {
        isNew -> "New memory file"
        file != null -> file.title
        else -> "Memory file"
    }

    fun handleResult(result: MemoryWriteResult): Boolean = when (result) {
        is MemoryWriteResult.Success -> true
        is MemoryWriteResult.Conflict -> {
            toaster.show("A file already exists at that path", ToastType.Error)
            false
        }

        is MemoryWriteResult.Rejected -> {
            toaster.show(result.reason, ToastType.Error)
            false
        }
    }

    WearScreen(
        title = title,
        onBack = {
            // 编辑已有文件时，返回先放弃修改回到查看状态
            if (editing && !isNew) {
                editing = false
                draftContent = file?.content.orEmpty()
            } else {
                onBack()
            }
        },
    ) {
        if (isNew) {
            item(key = "new-memory-path") {
                WearTextField(
                    value = draftPath,
                    onValueChange = { draftPath = it },
                    label = "Path",
                    placeholder = "/topics/example.md",
                    singleLine = true,
                )
            }
            textItem("Path must end with .md and use / separators.", small = true, muted = true)
        } else {
            infoItem("Path", file?.path ?: path.orEmpty())
        }

        when {
            !isNew && state is MemoryFileState.Loading -> textItem("Loading…")
            !isNew && state is MemoryFileState.Missing -> textItem("This memory file no longer exists.")
            editing -> {
                item(key = "memory-content-editor") {
                    WearTextField(
                        value = draftContent,
                        onValueChange = { draftContent = it },
                        label = "Content (Markdown)",
                        placeholder = "# Title\n\nWhat to remember…",
                        singleLine = false,
                        minLines = 8,
                    )
                }
                buttonItem(
                    label = if (isNew) "Create file" else "Save",
                    icon = WearIcons.Check,
                    enabled = if (isNew) draftPath.isNotBlank() else draftContent != file?.content,
                    onClick = {
                        if (isNew) {
                            vm.create(draftPath, draftContent) { result ->
                                if (handleResult(result)) {
                                    toaster.show("Memory file created", ToastType.Success)
                                    onBack()
                                }
                            }
                        } else {
                            vm.save(draftContent) { result ->
                                if (handleResult(result)) {
                                    editing = false
                                    toaster.show("Memory file saved", ToastType.Success)
                                }
                            }
                        }
                    },
                )
                if (!isNew) {
                    buttonItem(label = "Cancel editing", tonal = true) {
                        editing = false
                        draftContent = file?.content.orEmpty()
                    }
                }
            }

            file != null -> {
                textItem(
                    text = file.displayBody.ifBlank { file.content }.ifBlank { "This file is empty." },
                    maxLines = 80,
                )
                buttonItem(label = "Edit content", icon = WearIcons.Settings) {
                    draftContent = file.content
                    editing = true
                }
                section("Danger")
                buttonItem(label = "Delete file", icon = WearIcons.Delete, tonal = true) {
                    showDeleteDialog = true
                }
            }
        }
    }

    WearConfirmDialog(
        visible = showDeleteDialog,
        title = "Delete memory file?",
        message = path ?: "",
        confirmLabel = "Delete",
        danger = true,
        onConfirm = {
            showDeleteDialog = false
            vm.delete { onBack() }
        },
        onDismiss = { showDeleteDialog = false },
    )
}
