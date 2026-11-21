package me.rerere.rikkahub.wear.pages.extensions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import me.rerere.rikkahub.data.model.QuickMessage
import me.rerere.rikkahub.ui.pages.extensions.QuickMessagesVM
import me.rerere.rikkahub.wear.WearIcons
import me.rerere.rikkahub.wear.components.WearConfirmDialog
import me.rerere.rikkahub.wear.components.WearScreen
import me.rerere.rikkahub.wear.components.WearTextField
import me.rerere.rikkahub.wear.components.buttonItem
import me.rerere.rikkahub.wear.components.section
import me.rerere.rikkahub.wear.components.textItem
import org.koin.androidx.compose.koinViewModel
import kotlin.uuid.Uuid

private data class QuickMessageDraft(
    val id: Uuid?,
    val title: String,
    val content: String,
)

/** 快捷消息的新增 / 编辑 / 删除。 */
@Composable
fun WearQuickMessagesPage(
    onBack: () -> Unit,
) {
    val vm: QuickMessagesVM = koinViewModel()
    val settings by vm.settings.collectAsStateWithLifecycle()
    var draft by remember { mutableStateOf<QuickMessageDraft?>(null) }
    var pendingDelete by remember { mutableStateOf<QuickMessageDraft?>(null) }

    val currentDraft = draft
    if (currentDraft != null) {
        WearScreen(
            title = if (currentDraft.id == null) "New quick message" else "Edit quick message",
            onBack = { draft = null },
        ) {
            item(key = "quick-message-title") {
                WearTextField(
                    value = currentDraft.title,
                    onValueChange = { value -> draft = currentDraft.copy(title = value) },
                    label = "Title",
                    placeholder = "Explain this code",
                    singleLine = true,
                )
            }
            item(key = "quick-message-content") {
                WearTextField(
                    value = currentDraft.content,
                    onValueChange = { value -> draft = currentDraft.copy(content = value) },
                    label = "Content",
                    placeholder = "Please explain the selected code.",
                    singleLine = false,
                    minLines = 4,
                )
            }
            buttonItem(
                label = "Save",
                icon = WearIcons.Check,
                enabled = currentDraft.title.isNotBlank() && currentDraft.content.isNotBlank(),
                onClick = {
                    val saved = currentDraft
                    draft = null
                    if (saved.id == null) {
                        vm.addQuickMessage(saved.title.trim(), saved.content.trim())
                    } else {
                        vm.updateQuickMessage(
                            QuickMessage(
                                id = saved.id,
                                title = saved.title.trim(),
                                content = saved.content.trim(),
                            )
                        )
                    }
                },
            )
            buttonItem(label = "Cancel", tonal = true) { draft = null }
            if (currentDraft.id != null) {
                section("Danger")
                buttonItem(label = "Delete quick message", icon = WearIcons.Delete, tonal = true) {
                    pendingDelete = currentDraft
                }
            }
        }
    } else {
        WearScreen(title = "Quick messages", onBack = onBack) {
            buttonItem(label = "New quick message", icon = WearIcons.Add, tonal = true) {
                draft = QuickMessageDraft(id = null, title = "", content = "")
            }
            if (settings.quickMessages.isEmpty()) {
                textItem(
                    "No quick messages yet. Quick messages are reusable prompts you can send from the chat page.",
                )
            } else {
                settings.quickMessages.forEach { quickMessage ->
                    item(key = "quick-message:${quickMessage.id}") {
                        QuickMessageRow(
                            quickMessage = quickMessage,
                            onClick = {
                                draft = QuickMessageDraft(
                                    id = quickMessage.id,
                                    title = quickMessage.title,
                                    content = quickMessage.content,
                                )
                            },
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { target ->
        WearConfirmDialog(
            visible = true,
            title = "Delete quick message?",
            message = target.title.ifBlank { "Untitled" },
            confirmLabel = "Delete",
            danger = true,
            onConfirm = {
                pendingDelete = null
                draft = null
                target.id?.let(vm::deleteQuickMessage)
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun QuickMessageRow(
    quickMessage: QuickMessage,
    onClick: () -> Unit,
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(
                imageVector = WearIcons.Messages,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = quickMessage.title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = quickMessage.content.replace('\n', ' ').ifBlank { "Empty content" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
