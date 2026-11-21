package me.rerere.rikkahub.wear.pages.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.rikkahub.R
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

    // WearScreen 的 content 是普通 DSL builder（非 @Composable），先算好所有字符串。
    val appName = stringResource(R.string.app_name)
    val assistantFallback = stringResource(R.string.wear_set_assistant)
    val setUpModel = stringResource(R.string.wear_set_set_up_model)
    val noChatModel = stringResource(R.string.wear_set_no_chat_model)
    val newChat = stringResource(R.string.wear_set_new_chat)
    val recentSection = stringResource(R.string.wear_set_recent)
    val allConversations = stringResource(R.string.wear_set_all_conversations)
    val conversationsTotal = stringResource(R.string.wear_set_conversations_total, conversations.size)
    val moreSection = stringResource(R.string.wear_set_more)
    val favoritesTitle = stringResource(R.string.wear_set_favorites)
    val favoritesDesc = stringResource(R.string.wear_set_favorites_desc)
    val settingsTitle = stringResource(R.string.wear_set_settings_title)
    val settingsDesc = stringResource(R.string.wear_set_home_settings_desc)
    val untitledChat = stringResource(R.string.wear_set_untitled_chat)

    WearScreen(
        title = appName,
        subtitle = assistant.name.ifBlank { assistantFallback },
    ) {
        if (chatModel == null) {
            navItem(
                title = setUpModel,
                subtitle = noChatModel,
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
            label = newChat,
            icon = WearIcons.Add,
            onClick = onNewChat,
        )
        if (conversations.isNotEmpty()) {
            section(recentSection)
            conversations.take(8).forEach { conversation ->
                conversationItem(conversation, untitledChat) { onOpenChat(conversation.id) }
            }
            navItem(
                title = allConversations,
                subtitle = conversationsTotal,
                icon = WearIcons.History,
                onClick = onOpenHistory,
            )
        }
        section(moreSection)
        navItem(
            title = favoritesTitle,
            subtitle = favoritesDesc,
            icon = WearIcons.Star,
            onClick = onOpenFavorites,
        )
        navItem(
            title = settingsTitle,
            subtitle = settingsDesc,
            icon = WearIcons.Settings,
            onClick = onOpenSettings,
        )
    }
}

private fun WearListScope.conversationItem(
    conversation: Conversation,
    untitledChat: String,
    onClick: () -> Unit,
) {
    navItem(
        title = conversation.title.ifBlank { untitledChat },
        subtitle = conversation.currentMessages.lastOrNull()
            ?.toText()
            ?.replace('\n', ' ')
            ?.take(48),
        icon = WearIcons.Chat,
        onClick = onClick,
    )
}
