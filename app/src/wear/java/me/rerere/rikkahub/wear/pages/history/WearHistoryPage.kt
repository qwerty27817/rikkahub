package me.rerere.rikkahub.wear.pages.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.rikkahub.R
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

    // WearScreen 的 content 是普通 DSL builder（非 @Composable），先算好所有字符串。
    val title = stringResource(R.string.wear_set_conversations)
    val newChat = stringResource(R.string.wear_set_new_chat)
    val noConversations = stringResource(R.string.wear_set_no_conversations)
    val untitledChat = stringResource(R.string.wear_set_untitled_chat)
    val pinned = stringResource(R.string.wear_set_pinned)
    val emptyConversation = stringResource(R.string.wear_set_empty_conversation)

    WearScreen(title = title, onBack = onBack) {
        buttonItem(label = newChat, icon = WearIcons.Add, onClick = onNewChat)
        if (conversations.isEmpty()) {
            textItem(noConversations)
        } else {
            conversations.forEach { conversation ->
                navItem(
                    title = conversation.title.ifBlank { untitledChat },
                    subtitle = conversationSummary(conversation, emptyConversation),
                    icon = WearIcons.Chat,
                    trailingText = if (conversation.isPinned) pinned else null,
                    onClick = { onOpenChat(conversation.id) },
                )
            }
        }
    }
}

private fun conversationSummary(conversation: Conversation, emptyLabel: String): String {
    val last = conversation.currentMessages.lastOrNull()?.toText()?.replace('\n', ' ')?.trim().orEmpty()
    return last.take(60).ifBlank { emptyLabel }
}
