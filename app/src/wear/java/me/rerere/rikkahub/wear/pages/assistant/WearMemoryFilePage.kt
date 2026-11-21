package me.rerere.rikkahub.wear.pages.assistant

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.rikkahub.R
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

    // WearScreen 的 content 是普通 DSL builder（非 @Composable），字符串要先在外部取好。
    val newMemoryFileTitle = stringResource(R.string.wear_asst_new_memory_file)
    val memoryFileTitle = stringResource(R.string.wear_asst_memory_file)
    val pathLabel = stringResource(R.string.wear_asst_path)
    val pathPlaceholder = stringResource(R.string.wear_asst_path_placeholder)
    val pathHint = stringResource(R.string.wear_asst_path_hint)
    val loadingLabel = stringResource(R.string.wear_asst_loading)
    val missingFileLabel = stringResource(R.string.wear_asst_memory_file_missing)
    val contentLabel = stringResource(R.string.wear_asst_content_markdown)
    val contentPlaceholder = stringResource(R.string.wear_asst_content_placeholder)
    val createFileLabel = stringResource(R.string.wear_asst_create_file)
    val saveLabel = stringResource(R.string.wear_asst_save)
    val cancelEditingLabel = stringResource(R.string.wear_asst_cancel_editing)
    val fileEmptyLabel = stringResource(R.string.wear_asst_file_empty)
    val editContentLabel = stringResource(R.string.wear_asst_edit_content)
    val dangerLabel = stringResource(R.string.wear_asst_danger)
    val deleteFileLabel = stringResource(R.string.wear_asst_delete_file)
    val deleteMemoryFileTitle = stringResource(R.string.wear_asst_delete_memory_file)
    val deleteLabel = stringResource(R.string.wear_common_delete)
    val cancelLabel = stringResource(R.string.wear_common_cancel)
    val pathConflictMessage = stringResource(R.string.wear_asst_path_conflict)
    val memoryFileCreatedMessage = stringResource(R.string.wear_asst_memory_file_created)
    val memoryFileSavedMessage = stringResource(R.string.wear_asst_memory_file_saved)

    val title = when {
        isNew -> newMemoryFileTitle
        file != null -> file.title
        else -> memoryFileTitle
    }

    fun handleResult(result: MemoryWriteResult): Boolean = when (result) {
        is MemoryWriteResult.Success -> true
        is MemoryWriteResult.Conflict -> {
            toaster.show(pathConflictMessage, ToastType.Error)
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
                    label = pathLabel,
                    placeholder = pathPlaceholder,
                    singleLine = true,
                )
            }
            textItem(pathHint, small = true, muted = true)
        } else {
            infoItem(pathLabel, file?.path ?: path.orEmpty())
        }

        when {
            !isNew && state is MemoryFileState.Loading -> textItem(loadingLabel)
            !isNew && state is MemoryFileState.Missing -> textItem(missingFileLabel)
            editing -> {
                item(key = "memory-content-editor") {
                    WearTextField(
                        value = draftContent,
                        onValueChange = { draftContent = it },
                        label = contentLabel,
                        placeholder = contentPlaceholder,
                        singleLine = false,
                        minLines = 8,
                    )
                }
                buttonItem(
                    label = if (isNew) createFileLabel else saveLabel,
                    icon = WearIcons.Check,
                    enabled = if (isNew) draftPath.isNotBlank() else draftContent != file?.content,
                    onClick = {
                        if (isNew) {
                            vm.create(draftPath, draftContent) { result ->
                                if (handleResult(result)) {
                                    toaster.show(memoryFileCreatedMessage, ToastType.Success)
                                    onBack()
                                }
                            }
                        } else {
                            vm.save(draftContent) { result ->
                                if (handleResult(result)) {
                                    editing = false
                                    toaster.show(memoryFileSavedMessage, ToastType.Success)
                                }
                            }
                        }
                    },
                )
                if (!isNew) {
                    buttonItem(label = cancelEditingLabel, tonal = true) {
                        editing = false
                        draftContent = file?.content.orEmpty()
                    }
                }
            }

            file != null -> {
                textItem(
                    text = file.displayBody.ifBlank { file.content }.ifBlank { fileEmptyLabel },
                    maxLines = 80,
                )
                buttonItem(label = editContentLabel, icon = WearIcons.Settings) {
                    draftContent = file.content
                    editing = true
                }
                section(dangerLabel)
                buttonItem(label = deleteFileLabel, icon = WearIcons.Delete, tonal = true) {
                    showDeleteDialog = true
                }
            }
        }
    }

    WearConfirmDialog(
        visible = showDeleteDialog,
        title = deleteMemoryFileTitle,
        message = path ?: "",
        confirmLabel = deleteLabel,
        dismissLabel = cancelLabel,
        danger = true,
        onConfirm = {
            showDeleteDialog = false
            vm.delete { onBack() }
        },
        onDismiss = { showDeleteDialog = false },
    )
}
