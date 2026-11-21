package me.rerere.rikkahub.wear

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.TimeText
import kotlin.uuid.Uuid
import me.rerere.rikkahub.wear.pages.about.WearAboutPage
import me.rerere.rikkahub.wear.pages.assistant.WearAssistantDetailPage
import me.rerere.rikkahub.wear.pages.assistant.WearAssistantInjectionsPage
import me.rerere.rikkahub.wear.pages.assistant.WearAssistantMcpPage
import me.rerere.rikkahub.wear.pages.assistant.WearAssistantMemoryPage
import me.rerere.rikkahub.wear.pages.assistant.WearAssistantPromptPage
import me.rerere.rikkahub.wear.pages.assistant.WearAssistantToolsPage
import me.rerere.rikkahub.wear.pages.assistant.WearAssistantsPage
import me.rerere.rikkahub.wear.pages.assistant.WearMemoryFilePage
import me.rerere.rikkahub.wear.pages.backup.WearBackupPage
import me.rerere.rikkahub.wear.pages.chat.WearChatPage
import me.rerere.rikkahub.wear.pages.debug.WearDebugPage
import me.rerere.rikkahub.wear.pages.extensions.WearExtensionsPage
import me.rerere.rikkahub.wear.pages.extensions.WearPromptsPage
import me.rerere.rikkahub.wear.pages.extensions.WearQuickMessagesPage
import me.rerere.rikkahub.wear.pages.extensions.WearSkillsPage
import me.rerere.rikkahub.wear.pages.favorites.WearFavoritesPage
import me.rerere.rikkahub.wear.pages.history.WearHistoryPage
import me.rerere.rikkahub.wear.pages.home.WearHomePage
import me.rerere.rikkahub.wear.pages.imggen.WearImageGenPage
import me.rerere.rikkahub.wear.pages.logs.WearLogsPage
import me.rerere.rikkahub.wear.pages.mcp.WearMcpSettingsPage
import me.rerere.rikkahub.wear.pages.media.WearMediaSettingsPage
import me.rerere.rikkahub.wear.pages.mediacreation.WearMediaCreationPage
import me.rerere.rikkahub.wear.pages.mediacreation.WearMediaCreationSessionsPage
import me.rerere.rikkahub.wear.pages.provider.WearProviderDetailPage
import me.rerere.rikkahub.wear.pages.provider.WearProvidersPage
import me.rerere.rikkahub.wear.pages.quickstart.WearQuickStartPage
import me.rerere.rikkahub.wear.pages.search.WearSearchSettingsPage
import me.rerere.rikkahub.wear.pages.settings.WearModelsPage
import me.rerere.rikkahub.wear.pages.settings.WearScreenShapePage
import me.rerere.rikkahub.wear.pages.settings.WearSettingsPage
import me.rerere.rikkahub.wear.pages.speech.WearSpeechSettingsPage
import me.rerere.rikkahub.wear.pages.stats.WearStatsPage
import me.rerere.rikkahub.wear.pages.theme.WearAppearancePage
import me.rerere.rikkahub.wear.pages.translator.WearTranslatorPage
import me.rerere.rikkahub.wear.pages.web.WearWebServerPage
import me.rerere.rikkahub.wear.pages.webview.WearWebViewPage
import me.rerere.rikkahub.wear.pages.workspace.WearWorkspaceDetailPage
import me.rerere.rikkahub.wear.pages.workspace.WearWorkspaceTerminalPage
import me.rerere.rikkahub.wear.pages.workspace.WearWorkspacesPage

@Composable
fun WearApp(
    pendingDestination: WearDestination? = null,
    onPendingConsumed: () -> Unit = {},
) {
    val backStack = rememberNavBackStack(WearDestination.Home)

    val goBack: () -> Unit = {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }
    val push: (WearDestination) -> Unit = { destination ->
        if (backStack.lastOrNull() != destination) backStack.add(destination)
    }
    val openChat: (Uuid) -> Unit = { id -> push(WearDestination.Chat(id.toString())) }
    val newChat: () -> Unit = { openChat(Uuid.random()) }

    // 通知点击等外部入口带进来的目标页
    LaunchedEffect(pendingDestination) {
        if (pendingDestination != null) {
            push(pendingDestination)
            onPendingConsumed()
        }
    }

    AppScaffold(
        // 聊天页有自己固定的顶部标题栏，避免 TimeText 叠在标题上
        timeText = {
            if (backStack.lastOrNull() !is WearDestination.Chat) {
                TimeText()
            }
        },
    ) {
        BackHandler(enabled = backStack.size > 1) { goBack() }
        val transition = fadeIn(tween(120)) togetherWith fadeOut(tween(120))
        NavDisplay(
            backStack = backStack,
            onBack = goBack,
            modifier = Modifier.fillMaxSize(),
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            transitionSpec = { transition },
            popTransitionSpec = { transition },
            predictivePopTransitionSpec = { transition },
            entryProvider = entryProvider {
                entry<WearDestination.Home> {
                    WearHomePage(
                        onNewChat = newChat,
                        onOpenChat = openChat,
                        onOpenHistory = { push(WearDestination.History) },
                        onOpenSettings = { push(WearDestination.Settings) },
                        onOpenQuickStart = { push(WearDestination.QuickStart) },
                        onOpenFavorites = { push(WearDestination.Favorites) },
                    )
                }
                entry<WearDestination.Chat> { key ->
                    WearChatPage(
                        conversationId = Uuid.parse(key.id),
                        onBack = goBack,
                        onForked = { newId -> push(WearDestination.Chat(newId.toString())) },
                        onNewChat = newChat,
                    )
                }
                entry<WearDestination.History> {
                    WearHistoryPage(
                        onBack = goBack,
                        onOpenChat = openChat,
                        onNewChat = newChat,
                    )
                }
                entry<WearDestination.Settings> {
                    WearSettingsPage(onBack = goBack, onNavigate = push)
                }
                entry<WearDestination.ScreenShapeSettings> {
                    WearScreenShapePage(onBack = goBack)
                }
                entry<WearDestination.QuickStart> {
                    WearQuickStartPage(onBack = goBack)
                }
                entry<WearDestination.Providers> {
                    WearProvidersPage(
                        onBack = goBack,
                        onOpenProvider = { id -> push(WearDestination.ProviderDetail(id.toString())) },
                    )
                }
                entry<WearDestination.ProviderDetail> { key ->
                    WearProviderDetailPage(providerId = Uuid.parse(key.id), onBack = goBack)
                }
                entry<WearDestination.Models> { WearModelsPage(onBack = goBack) }
                entry<WearDestination.Assistants> {
                    WearAssistantsPage(
                        onBack = goBack,
                        onOpenAssistant = { id -> push(WearDestination.AssistantDetail(id.toString())) },
                    )
                }
                entry<WearDestination.AssistantDetail> { key ->
                    WearAssistantDetailPage(
                        assistantId = Uuid.parse(key.id),
                        onBack = goBack,
                        onNavigate = push,
                    )
                }
                entry<WearDestination.AssistantPrompt> { key ->
                    WearAssistantPromptPage(assistantId = Uuid.parse(key.id), onBack = goBack)
                }
                entry<WearDestination.AssistantMemory> { key ->
                    WearAssistantMemoryPage(
                        assistantId = Uuid.parse(key.id),
                        onBack = goBack,
                        onOpenFile = { memoryId, path ->
                            push(WearDestination.MemoryFile(memoryId, path))
                        },
                    )
                }
                entry<WearDestination.MemoryFile> { key ->
                    WearMemoryFilePage(
                        memoryId = key.memoryId,
                        path = key.path,
                        onBack = goBack,
                    )
                }
                entry<WearDestination.AssistantMcp> { key ->
                    WearAssistantMcpPage(assistantId = Uuid.parse(key.id), onBack = goBack)
                }
                entry<WearDestination.AssistantTools> { key ->
                    WearAssistantToolsPage(assistantId = Uuid.parse(key.id), onBack = goBack)
                }
                entry<WearDestination.AssistantInjections> { key ->
                    WearAssistantInjectionsPage(assistantId = Uuid.parse(key.id), onBack = goBack)
                }
                entry<WearDestination.ThemeSettings> { WearAppearancePage(onBack = goBack) }
                entry<WearDestination.SearchSettings> { WearSearchSettingsPage(onBack = goBack) }
                entry<WearDestination.McpSettings> { WearMcpSettingsPage(onBack = goBack) }
                entry<WearDestination.SpeechSettings> { WearSpeechSettingsPage(onBack = goBack) }
                entry<WearDestination.MediaSettings> { WearMediaSettingsPage(onBack = goBack) }
                entry<WearDestination.WebServerSettings> { WearWebServerPage(onBack = goBack) }
                entry<WearDestination.Backup> { WearBackupPage(onBack = goBack) }
                entry<WearDestination.Extensions> {
                    WearExtensionsPage(
                        onBack = goBack,
                        onOpenPrompts = { push(WearDestination.Prompts) },
                        onOpenQuickMessages = { push(WearDestination.QuickMessages) },
                        onOpenSkills = { push(WearDestination.Skills) },
                    )
                }
                entry<WearDestination.Prompts> { WearPromptsPage(onBack = goBack) }
                entry<WearDestination.QuickMessages> { WearQuickMessagesPage(onBack = goBack) }
                entry<WearDestination.Skills> { WearSkillsPage(onBack = goBack) }
                entry<WearDestination.Workspaces> {
                    WearWorkspacesPage(
                        onBack = goBack,
                        onOpen = { id -> push(WearDestination.WorkspaceDetail(id.toString())) },
                    )
                }
                entry<WearDestination.WorkspaceDetail> { key ->
                    WearWorkspaceDetailPage(
                        workspaceId = Uuid.parse(key.id),
                        onBack = goBack,
                        onOpenTerminal = { id -> push(WearDestination.WorkspaceTerminal(id.toString())) },
                    )
                }
                entry<WearDestination.WorkspaceTerminal> { key ->
                    WearWorkspaceTerminalPage(workspaceId = Uuid.parse(key.id), onBack = goBack)
                }
                entry<WearDestination.Stats> { WearStatsPage(onBack = goBack) }
                entry<WearDestination.Logs> { WearLogsPage(onBack = goBack) }
                entry<WearDestination.Debug> { WearDebugPage(onBack = goBack) }
                entry<WearDestination.About> { WearAboutPage(onBack = goBack) }
                entry<WearDestination.Favorites> {
                    WearFavoritesPage(onBack = goBack, onOpenChat = openChat)
                }
                entry<WearDestination.Translator> { WearTranslatorPage(onBack = goBack) }
                entry<WearDestination.ImageGen> { WearImageGenPage(onBack = goBack) }
                entry<WearDestination.MediaCreationSessions> {
                    WearMediaCreationSessionsPage(
                        onBack = goBack,
                        onOpenSession = { id -> push(WearDestination.MediaCreation(id)) },
                    )
                }
                entry<WearDestination.MediaCreation> { key ->
                    WearMediaCreationPage(sessionId = key.id, onBack = goBack)
                }
                entry<WearDestination.WebView> { key ->
                    WearWebViewPage(url = key.url, contentId = key.contentId, onBack = goBack)
                }
            },
        )
    }
}
