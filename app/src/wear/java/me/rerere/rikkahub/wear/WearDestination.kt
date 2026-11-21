package me.rerere.rikkahub.wear

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** 手表端导航目的地。手机版 RouteActivity 里的 Screen 不参与手表端构建。 */
@Serializable
sealed interface WearDestination : NavKey {
    @Serializable
    data object Home : WearDestination

    @Serializable
    data class Chat(val id: String) : WearDestination

    @Serializable
    data object History : WearDestination

    @Serializable
    data object Settings : WearDestination

    @Serializable
    data object QuickStart : WearDestination

    @Serializable
    data object Providers : WearDestination

    @Serializable
    data class ProviderDetail(val id: String) : WearDestination

    @Serializable
    data object Models : WearDestination

    @Serializable
    data object Assistants : WearDestination

    @Serializable
    data class AssistantDetail(val id: String) : WearDestination

    @Serializable
    data class AssistantPrompt(val id: String) : WearDestination

    @Serializable
    data class AssistantMemory(val id: String) : WearDestination

    @Serializable
    data class AssistantMcp(val id: String) : WearDestination

    @Serializable
    data class AssistantTools(val id: String) : WearDestination

    @Serializable
    data class AssistantInjections(val id: String) : WearDestination

    @Serializable
    data class MemoryFile(val memoryId: String, val path: String? = null) : WearDestination

    @Serializable
    data object Prompts : WearDestination

    @Serializable
    data object QuickMessages : WearDestination

    @Serializable
    data object Skills : WearDestination

    @Serializable
    data object Translator : WearDestination

    @Serializable
    data object ImageGen : WearDestination

    @Serializable
    data object MediaCreationSessions : WearDestination

    @Serializable
    data class MediaCreation(val id: String) : WearDestination

    @Serializable
    data class WebView(val url: String = "", val contentId: String = "") : WearDestination

    @Serializable
    data class WorkspaceDetail(val id: String) : WearDestination

    @Serializable
    data class WorkspaceTerminal(val id: String) : WearDestination

    @Serializable
    data object ScreenShapeSettings : WearDestination

    @Serializable
    data object ThemeSettings : WearDestination

    @Serializable
    data object About : WearDestination

    @Serializable
    data object SearchSettings : WearDestination

    @Serializable
    data object McpSettings : WearDestination

    @Serializable
    data object SpeechSettings : WearDestination

    @Serializable
    data object MediaSettings : WearDestination

    @Serializable
    data object WebServerSettings : WearDestination

    @Serializable
    data object Backup : WearDestination

    @Serializable
    data object Extensions : WearDestination

    @Serializable
    data object Workspaces : WearDestination

    @Serializable
    data object Stats : WearDestination

    @Serializable
    data object Logs : WearDestination

    @Serializable
    data object Debug : WearDestination

    @Serializable
    data object Favorites : WearDestination
}
