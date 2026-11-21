package me.rerere.rikkahub.wear.pages.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.rikkahub.data.datastore.findModelById
import me.rerere.rikkahub.data.datastore.getCurrentAssistant
import me.rerere.rikkahub.ui.context.LocalSettings
import me.rerere.rikkahub.wear.WearDestination
import me.rerere.rikkahub.wear.WearIcons
import me.rerere.rikkahub.wear.components.WearScreen
import me.rerere.rikkahub.wear.components.infoItem
import me.rerere.rikkahub.wear.components.navItem
import me.rerere.rikkahub.wear.components.section
import me.rerere.rikkahub.wear.components.shapeModeSetting
import me.rerere.rikkahub.wear.shape.LocalScreenShapeController
import me.rerere.rikkahub.wear.shape.LocalScreenShapeState

@Composable
fun WearSettingsPage(
    onBack: () -> Unit,
    onNavigate: (WearDestination) -> Unit,
) {
    val settings = LocalSettings.current
    val assistant = settings.getCurrentAssistant()
    val chatModel = settings.findModelById(assistant.chatModelId ?: settings.chatModelId)
    val shapeState: me.rerere.rikkahub.wear.shape.ScreenShapeState = LocalScreenShapeState.current
    val shapeController = LocalScreenShapeController.current

    WearScreen(title = "Settings", onBack = onBack) {
        section("Chat")
        navItem(
            title = "Model",
            subtitle = chatModel?.displayName?.ifBlank { chatModel.modelId } ?: "Not configured",
            icon = WearIcons.Cpu,
            onClick = { onNavigate(WearDestination.Models) },
        )
        navItem(
            title = "Providers",
            subtitle = "${settings.providers.count { it.enabled }}/${settings.providers.size} enabled",
            icon = WearIcons.Server,
            onClick = { onNavigate(WearDestination.Providers) },
        )
        navItem(
            title = "Assistants",
            subtitle = "${settings.assistants.size} configured",
            icon = WearIcons.Assistant,
            onClick = { onNavigate(WearDestination.Assistants) },
        )
        navItem(
            title = "Quick start",
            subtitle = "Guided provider setup",
            icon = WearIcons.Add,
            onClick = { onNavigate(WearDestination.QuickStart) },
        )

        section("Display")
        if (shapeController != null) {
            shapeModeSetting(controller = shapeController, state = shapeState, title = "Screen shape")
        }
        infoItem("Detection", shapeState.diagnosis)
        navItem(
            title = "Appearance",
            subtitle = "Theme & dynamic color",
            icon = WearIcons.Theme,
            onClick = { onNavigate(WearDestination.ThemeSettings) },
        )

        section("Features")
        navItem("Search", "Web search providers", WearIcons.Search, onClick = { onNavigate(WearDestination.SearchSettings) })
        navItem("MCP", "Model Context Protocol", WearIcons.Mcp, onClick = { onNavigate(WearDestination.McpSettings) })
        navItem("Speech", "TTS / ASR", WearIcons.Mic, onClick = { onNavigate(WearDestination.SpeechSettings) })
        navItem("Media", "Image / video generation", WearIcons.Image, onClick = { onNavigate(WearDestination.MediaSettings) })
        navItem("Web server", "Local web chat", WearIcons.Web, onClick = { onNavigate(WearDestination.WebServerSettings) })
        navItem("Extensions", "Prompt injections & skills", WearIcons.Skills, onClick = { onNavigate(WearDestination.Extensions) })
        navItem("Workspace", "PRoot sandbox & terminal", WearIcons.Folder, onClick = { onNavigate(WearDestination.Workspaces) })
        navItem("Backup", "Import / export", WearIcons.Backup, onClick = { onNavigate(WearDestination.Backup) })

        section("Tools")
        navItem("Translator", "Translate text", WearIcons.Translate, onClick = { onNavigate(WearDestination.Translator) })
        navItem("Image generation", "Generate images", WearIcons.Image, onClick = { onNavigate(WearDestination.ImageGen) })
        navItem("Media creation", "Sessions & assets", WearIcons.Ai, onClick = { onNavigate(WearDestination.MediaCreationSessions) })
        navItem("Web browser", "Open a web page", WearIcons.Web, onClick = { onNavigate(WearDestination.WebView()) })

        section("About")
        navItem("Statistics", "Token usage", WearIcons.Stats, onClick = { onNavigate(WearDestination.Stats) })
        navItem("Logs", "Runtime logs", WearIcons.File, onClick = { onNavigate(WearDestination.Logs) })
        navItem("Debug", "Developer tools", WearIcons.Debug, onClick = { onNavigate(WearDestination.Debug) })
        navItem("About RikkaHub", "Version & device info", WearIcons.Info, onClick = { onNavigate(WearDestination.About) })
    }
}
