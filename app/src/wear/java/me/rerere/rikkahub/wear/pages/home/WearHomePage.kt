package me.rerere.rikkahub.wear.pages.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.rikkahub.data.datastore.findModelById
import me.rerere.rikkahub.data.datastore.getCurrentAssistant
import me.rerere.rikkahub.data.model.Conversation
import me.rerere.rikkahub.ui.context.LocalSettings
import me.rerere.rikkahub.ui.pages.history.HistoryVM
import me.rerere.rikkahub.wear.WearIcons
import me.rerere.rikkahub.wear.components.WearListScope
import me.rerere.rikkahub.wear.components.WearScreen
import me.rerere.rikkahub.wear.components.buttonItem
import me.rerere.rikkahub.wear.components.navItem
import me.rerere.rikkahub.wear.components.section
import me.rerere.rikkahub.wear.components.textItem
import org.koin.androidx.compose.koinViewModel
import kotlin.uuid.Uuid

@Composable
fun WearHomePage(
    onNewChat: () -> Unit,
    onOpenChat: (Uuid) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenQuickStart: () -> Unit,
    onOpenFavorites: () -> Unit,
) {
    val settings = LocalSettings.current
    val assistant = settings.getCurrentAssistant()
    val chatModel = settings.findModelById(assistant.chatModelId ?: settings.chatModelId)
    val vm: HistoryVM = koinViewModel()
    val conversations by vm.conversations.collectAsStateWithLifecycle()

    WearScreen(
        title = "RikkaHub",
        subtitle = assistant.name.ifBlank { "Assistant" },
    ) {
        if (chatModel == null) {
            navItem(
                title = "Set up a model",
                subtitle = "No chat model configured yet",
                icon = WearIcons.Cpu,
                onClick = onOpenQuickStart,
            )
        } else {
            textItem(
                text = chatModel.displayName.ifBlank { chatModel.modelId },
                small = true,
                muted = true,
            )
        }
        buttonItem(
            label = "New chat",
            icon = WearIcons.Add,
            onClick = onNewChat,
        )
        if (conversations.isNotEmpty()) {
            section("Recent")
            conversations.take(8).forEach { conversation ->
                conversationItem(conversation) { onOpenChat(conversation.id) }
            }
            navItem(
                title = "All conversations",
                subtitle = "${conversations.size} total",
                icon = WearIcons.History,
                onClick = onOpenHistory,
            )
        }
        section("More")
        navItem(
            title = "Favorites",
            subtitle = "Starred messages",
            icon = WearIcons.Star,
            onClick = onOpenFavorites,
        )
        navItem(
            title = "Settings",
            subtitle = "Providers, models, screen shape",
            icon = WearIcons.Settings,
            onClick = onOpenSettings,
        )
    }
}

private fun WearListScope.conversationItem(
    conversation: Conversation,
    onClick: () -> Unit,
) {
    navItem(
        title = conversation.title.ifBlank { "Untitled chat" },
        subtitle = conversation.currentMessages.lastOrNull()
            ?.toText()
            ?.replace('\n', ' ')
            ?.take(48),
        icon = WearIcons.Chat,
        onClick = onClick,
    )
}
