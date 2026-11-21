package me.rerere.rikkahub.wear.pages.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.rikkahub.data.model.Conversation
import me.rerere.rikkahub.ui.pages.history.HistoryVM
import me.rerere.rikkahub.wear.WearIcons
import me.rerere.rikkahub.wear.components.WearScreen
import me.rerere.rikkahub.wear.components.buttonItem
import me.rerere.rikkahub.wear.components.navItem
import me.rerere.rikkahub.wear.components.textItem
import org.koin.androidx.compose.koinViewModel
import kotlin.uuid.Uuid

@Composable
fun WearHistoryPage(
    onBack: () -> Unit,
    onOpenChat: (Uuid) -> Unit,
    onNewChat: () -> Unit,
) {
    val vm: HistoryVM = koinViewModel()
    val conversations by vm.conversations.collectAsStateWithLifecycle()

    WearScreen(title = "Conversations", onBack = onBack) {
        buttonItem(label = "New chat", icon = WearIcons.Add, onClick = onNewChat)
        if (conversations.isEmpty()) {
            textItem("No conversations yet.")
        } else {
            conversations.forEach { conversation ->
                navItem(
                    title = conversation.title.ifBlank { "Untitled chat" },
                    subtitle = conversationSummary(conversation),
                    icon = WearIcons.Chat,
                    trailingText = if (conversation.isPinned) "Pinned" else null,
                    onClick = { onOpenChat(conversation.id) },
                )
            }
        }
    }
}

private fun conversationSummary(conversation: Conversation): String {
    val last = conversation.currentMessages.lastOrNull()?.toText()?.replace('\n', ' ')?.trim().orEmpty()
    return last.take(60).ifBlank { "Empty conversation" }
}
