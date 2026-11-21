package me.rerere.rikkahub.wear.pages.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.rikkahub.R
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

    // WearScreen 的 content 是普通 DSL builder（非 @Composable），
    // 所以所有 stringResource 都要在进入 DSL 之前算好。
    val settingsTitle = stringResource(R.string.wear_set_settings_title)
    val chatSection = stringResource(R.string.wear_set_section_chat)
    val modelTitle = stringResource(R.string.wear_set_model)
    val notConfigured = stringResource(R.string.wear_set_not_configured)
    val providersTitle = stringResource(R.string.wear_set_providers)
    val providersEnabled = stringResource(
        R.string.wear_set_providers_enabled,
        settings.providers.count { it.enabled },
        settings.providers.size,
    )
    val assistantsTitle = stringResource(R.string.wear_set_assistants)
    val assistantsConfigured = stringResource(
        R.string.wear_set_assistants_configured,
        settings.assistants.size,
    )
    val quickStartTitle = stringResource(R.string.wear_set_quick_start)
    val quickStartDesc = stringResource(R.string.wear_set_quick_start_desc)

    val displaySection = stringResource(R.string.wear_set_section_display)
    val screenShapeTitle = stringResource(R.string.wear_common_screen_shape)
    val shapeAutoLabel = stringResource(R.string.wear_common_auto)
    val shapeDetectedFormat = stringResource(R.string.wear_common_detected_auto)
    val shapeForceSquareLabel = stringResource(R.string.wear_common_force_square)
    val shapeForceRoundLabel = stringResource(R.string.wear_common_force_round)
    val shapeDisplayLabel = stringResource(R.string.wear_common_display)
    val detectionTitle = stringResource(R.string.wear_set_detection)
    val appearanceTitle = stringResource(R.string.wear_set_appearance)
    val appearanceDesc = stringResource(R.string.wear_set_appearance_desc)

    val featuresSection = stringResource(R.string.wear_set_section_features)
    val searchTitle = stringResource(R.string.wear_set_search)
    val searchDesc = stringResource(R.string.wear_set_search_desc)
    val mcpTitle = stringResource(R.string.wear_set_mcp)
    val mcpDesc = stringResource(R.string.wear_set_mcp_desc)
    val speechTitle = stringResource(R.string.wear_set_speech)
    val speechDesc = stringResource(R.string.wear_set_speech_desc)
    val mediaTitle = stringResource(R.string.wear_set_media)
    val mediaDesc = stringResource(R.string.wear_set_media_desc)
    val webServerTitle = stringResource(R.string.wear_set_web_server)
    val webServerDesc = stringResource(R.string.wear_set_web_server_desc)
    val extensionsTitle = stringResource(R.string.wear_set_extensions)
    val extensionsDesc = stringResource(R.string.wear_set_extensions_desc)
    val workspaceTitle = stringResource(R.string.wear_set_workspace)
    val workspaceDesc = stringResource(R.string.wear_set_workspace_desc)
    val backupTitle = stringResource(R.string.wear_set_backup)
    val backupDesc = stringResource(R.string.wear_set_backup_desc)

    val toolsSection = stringResource(R.string.wear_set_section_tools)
    val translatorTitle = stringResource(R.string.wear_set_translator)
    val translatorDesc = stringResource(R.string.wear_set_translator_desc)
    val imageGenerationTitle = stringResource(R.string.wear_set_image_generation)
    val imageGenerationDesc = stringResource(R.string.wear_set_image_generation_desc)
    val mediaCreationTitle = stringResource(R.string.wear_set_media_creation)
    val mediaCreationDesc = stringResource(R.string.wear_set_media_creation_desc)
    val webBrowserTitle = stringResource(R.string.wear_set_web_browser)
    val webBrowserDesc = stringResource(R.string.wear_set_web_browser_desc)

    val aboutSection = stringResource(R.string.wear_set_section_about)
    val statisticsTitle = stringResource(R.string.wear_set_statistics)
    val statisticsDesc = stringResource(R.string.wear_set_token_usage)
    val logsTitle = stringResource(R.string.wear_set_logs)
    val logsDesc = stringResource(R.string.wear_set_logs_desc)
    val debugTitle = stringResource(R.string.wear_set_debug)
    val debugDesc = stringResource(R.string.wear_set_debug_desc)
    val aboutTitle = stringResource(R.string.wear_set_about_rikkahub)
    val aboutDesc = stringResource(R.string.wear_set_about_desc)

    WearScreen(title = settingsTitle, onBack = onBack) {
        section(chatSection)
        navItem(
            title = modelTitle,
            subtitle = chatModel?.displayName?.ifBlank { chatModel.modelId } ?: notConfigured,
            icon = WearIcons.Cpu,
            onClick = { onNavigate(WearDestination.Models) },
        )
        navItem(
            title = providersTitle,
            subtitle = providersEnabled,
            icon = WearIcons.Server,
            onClick = { onNavigate(WearDestination.Providers) },
        )
        navItem(
            title = assistantsTitle,
            subtitle = assistantsConfigured,
            icon = WearIcons.Assistant,
            onClick = { onNavigate(WearDestination.Assistants) },
        )
        navItem(
            title = quickStartTitle,
            subtitle = quickStartDesc,
            icon = WearIcons.Add,
            onClick = { onNavigate(WearDestination.QuickStart) },
        )

        section(displaySection)
        if (shapeController != null) {
            shapeModeSetting(
                controller = shapeController,
                state = shapeState,
                title = screenShapeTitle,
                autoLabel = shapeAutoLabel,
                autoDetectedFormat = shapeDetectedFormat,
                forceSquareLabel = shapeForceSquareLabel,
                forceRoundLabel = shapeForceRoundLabel,
                displayLabel = shapeDisplayLabel,
            )
        }
        infoItem(detectionTitle, shapeState.diagnosis)
        navItem(
            title = appearanceTitle,
            subtitle = appearanceDesc,
            icon = WearIcons.Theme,
            onClick = { onNavigate(WearDestination.ThemeSettings) },
        )

        section(featuresSection)
        navItem(searchTitle, searchDesc, WearIcons.Search, onClick = { onNavigate(WearDestination.SearchSettings) })
        navItem(mcpTitle, mcpDesc, WearIcons.Mcp, onClick = { onNavigate(WearDestination.McpSettings) })
        navItem(speechTitle, speechDesc, WearIcons.Mic, onClick = { onNavigate(WearDestination.SpeechSettings) })
        navItem(mediaTitle, mediaDesc, WearIcons.Image, onClick = { onNavigate(WearDestination.MediaSettings) })
        navItem(webServerTitle, webServerDesc, WearIcons.Web, onClick = { onNavigate(WearDestination.WebServerSettings) })
        navItem(extensionsTitle, extensionsDesc, WearIcons.Skills, onClick = { onNavigate(WearDestination.Extensions) })
        navItem(workspaceTitle, workspaceDesc, WearIcons.Folder, onClick = { onNavigate(WearDestination.Workspaces) })
        navItem(backupTitle, backupDesc, WearIcons.Backup, onClick = { onNavigate(WearDestination.Backup) })

        section(toolsSection)
        navItem(translatorTitle, translatorDesc, WearIcons.Translate, onClick = { onNavigate(WearDestination.Translator) })
        navItem(
            imageGenerationTitle,
            imageGenerationDesc,
            WearIcons.Image,
            onClick = { onNavigate(WearDestination.ImageGen) },
        )
        navItem(
            mediaCreationTitle,
            mediaCreationDesc,
            WearIcons.Ai,
            onClick = { onNavigate(WearDestination.MediaCreationSessions) },
        )
        navItem(webBrowserTitle, webBrowserDesc, WearIcons.Web, onClick = { onNavigate(WearDestination.WebView()) })

        section(aboutSection)
        navItem(statisticsTitle, statisticsDesc, WearIcons.Stats, onClick = { onNavigate(WearDestination.Stats) })
        navItem(logsTitle, logsDesc, WearIcons.File, onClick = { onNavigate(WearDestination.Logs) })
        navItem(debugTitle, debugDesc, WearIcons.Debug, onClick = { onNavigate(WearDestination.Debug) })
        navItem(aboutTitle, aboutDesc, WearIcons.Info, onClick = { onNavigate(WearDestination.About) })
    }
}
