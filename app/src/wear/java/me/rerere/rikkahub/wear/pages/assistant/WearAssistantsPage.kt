package me.rerere.rikkahub.wear.pages.assistant

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.datastore.findModelById
import me.rerere.rikkahub.ui.context.LocalSettings
import me.rerere.rikkahub.wear.WearIcons
import me.rerere.rikkahub.wear.components.WearScreen
import me.rerere.rikkahub.wear.components.infoItem
import me.rerere.rikkahub.wear.components.navItem
import me.rerere.rikkahub.wear.components.radioItem
import me.rerere.rikkahub.wear.components.section
import me.rerere.rikkahub.wear.components.textItem
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.uuid.Uuid

@Composable
fun WearAssistantsPage(
    onBack: () -> Unit,
    onOpenAssistant: (Uuid) -> Unit,
) {
    val settings = LocalSettings.current
    val store: SettingsStore = koinInject()
    val scope = rememberCoroutineScope()
    val currentAssistant = settings.assistants.find { it.id == settings.assistantId }
        ?: settings.assistants.firstOrNull()

    // WearScreen 的 content 是普通 DSL builder（非 @Composable），字符串要先在外部取好。
    val pageTitle = stringResource(R.string.wear_asst_title)
    val pageDesc = stringResource(R.string.wear_asst_page_desc)
    val switchAssistantLabel = stringResource(R.string.wear_asst_switch_assistant)
    val assistantFallback = stringResource(R.string.wear_asst_assistant)
    val noModelLabel = stringResource(R.string.wear_asst_no_model)
    val totalLabel = stringResource(R.string.wear_asst_total)
    val configureSubtitle = stringResource(R.string.wear_asst_configure_subtitle)
    val configureName = currentAssistant?.name?.ifBlank { assistantFallback }
    val configureTitle = if (configureName != null) {
        stringResource(R.string.wear_asst_configure_assistant, configureName)
    } else {
        ""
    }

    WearScreen(title = pageTitle, onBack = onBack) {
        textItem(pageDesc)
        if (currentAssistant != null) {
            buttonItemForAssistant(
                id = currentAssistant.id,
                title = configureTitle,
                subtitle = configureSubtitle,
                onOpen = onOpenAssistant,
            )
        }
        section(switchAssistantLabel)
        settings.assistants.forEach { assistant ->
            val model = settings.findModelById(assistant.chatModelId ?: settings.chatModelId)
            radioItem(
                title = assistant.name.ifBlank { assistantFallback },
                subtitle = model?.displayName?.ifBlank { model.modelId } ?: noModelLabel,
                selected = assistant.id == settings.assistantId,
                onSelect = {
                    scope.launch { store.selectAssistant(assistant.id) }
                },
            )
        }
        infoItem(totalLabel, "${settings.assistants.size}")
    }
}

private fun me.rerere.rikkahub.wear.components.WearListScope.buttonItemForAssistant(
    id: Uuid,
    title: String,
    subtitle: String,
    onOpen: (Uuid) -> Unit,
) {
    navItem(
        title = title,
        subtitle = subtitle,
        icon = WearIcons.Settings,
        onClick = { onOpen(id) },
    )
}
