package me.rerere.rikkahub.wear.pages.assistant

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.CheckboxButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import kotlinx.coroutines.launch
import me.rerere.rikkahub.R
import me.rerere.rikkahub.ui.context.LocalSettings
import me.rerere.rikkahub.ui.pages.assistant.detail.AssistantDetailVM
import me.rerere.rikkahub.wear.components.WearListScope
import me.rerere.rikkahub.wear.components.WearScreen
import me.rerere.rikkahub.wear.components.infoItem
import me.rerere.rikkahub.wear.components.section
import me.rerere.rikkahub.wear.components.textItem
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * 把模式注入 / Lorebook / 快捷消息 / 技能分配给助手。
 *
 * 与手机版 AssistantExtensionsPage 使用同一组字段：modeInjectionIds、lorebookIds、
 * quickMessageIds、enabledSkills。
 */
@Composable
fun WearAssistantInjectionsPage(
    assistantId: kotlin.uuid.Uuid,
    onBack: () -> Unit,
) {
    val settings = LocalSettings.current
    val scope = rememberCoroutineScope()
    val vm: AssistantDetailVM = koinViewModel(parameters = { parametersOf(assistantId.toString()) })
    val assistant = settings.assistants.find { it.id == assistantId }
    val skills by vm.skills.collectAsStateWithLifecycle()

    // WearScreen 的 content 是普通 DSL builder（非 @Composable），字符串要先在外部取好。
    val injectionsTitle = stringResource(R.string.wear_asst_injections)
    val missingAssistant = stringResource(R.string.wear_asst_assistant_missing)

    if (assistant == null) {
        WearScreen(title = injectionsTitle, onBack = onBack) {
            textItem(missingAssistant)
        }
        return
    }

    val promptInjectionsTitle = stringResource(R.string.wear_asst_prompt_injections)
    val injectionsDesc = stringResource(R.string.wear_asst_injections_desc)
    val modeInjectionsLabel = stringResource(R.string.wear_asst_mode_injections)
    val noModeInjectionsLabel = stringResource(R.string.wear_asst_no_mode_injections)
    val modeInjectionFallback = stringResource(R.string.wear_asst_mode_injection)
    val lorebooksLabel = stringResource(R.string.wear_asst_lorebooks)
    val noLorebooksLabel = stringResource(R.string.wear_asst_no_lorebooks)
    val lorebookFallback = stringResource(R.string.wear_asst_lorebook)
    val quickMessagesLabel = stringResource(R.string.wear_asst_quick_messages)
    val noQuickMessagesLabel = stringResource(R.string.wear_asst_no_quick_messages)
    val untitledLabel = stringResource(R.string.wear_asst_untitled)
    val skillsLabel = stringResource(R.string.wear_asst_skills)
    val noSkillsLabel = stringResource(R.string.wear_asst_no_skills)
    val assignedLabel = stringResource(R.string.wear_asst_assigned)
    val assignedSummary = stringResource(
        R.string.wear_asst_injections_assigned_summary,
        assistant.modeInjectionIds.size,
        assistant.lorebookIds.size,
        assistant.quickMessageIds.size,
        assistant.enabledSkills.size,
    )
    val lorebookEntryLabels = mutableMapOf<kotlin.uuid.Uuid, String>()
    for (lorebook in settings.lorebooks) {
        lorebookEntryLabels[lorebook.id] = stringResource(
            R.string.wear_asst_lorebook_entries,
            lorebook.entries.size,
        )
    }

    WearScreen(title = promptInjectionsTitle, onBack = onBack) {
        textItem(
            injectionsDesc,
            small = true,
            muted = true,
        )

        section(modeInjectionsLabel)
        if (settings.modeInjections.isEmpty()) {
            textItem(noModeInjectionsLabel, small = true, muted = true)
        } else {
            settings.modeInjections.forEach { injection ->
                selectionToggle(
                    key = "mode:${injection.id}",
                    title = injection.name.ifBlank { modeInjectionFallback },
                    subtitle = injection.content
                        .replace('\n', ' ')
                        .trim()
                        .take(80)
                        .ifBlank { null },
                    checked = injection.id in assistant.modeInjectionIds,
                    onCheckedChange = { checked ->
                        val newIds = if (checked) {
                            assistant.modeInjectionIds + injection.id
                        } else {
                            assistant.modeInjectionIds - injection.id
                        }
                        scope.launch { vm.update(assistant.copy(modeInjectionIds = newIds)) }
                    },
                )
            }
        }

        section(lorebooksLabel)
        if (settings.lorebooks.isEmpty()) {
            textItem(noLorebooksLabel, small = true, muted = true)
        } else {
            settings.lorebooks.forEach { lorebook ->
                selectionToggle(
                    key = "lorebook:${lorebook.id}",
                    title = lorebook.name.ifBlank { lorebookFallback },
                    subtitle = lorebookEntryLabels.getValue(lorebook.id) +
                        lorebook.description.take(60).takeIf { it.isNotBlank() }
                            ?.let { " · $it" }.orEmpty(),
                    checked = lorebook.id in assistant.lorebookIds,
                    onCheckedChange = { checked ->
                        val newIds = if (checked) {
                            assistant.lorebookIds + lorebook.id
                        } else {
                            assistant.lorebookIds - lorebook.id
                        }
                        scope.launch { vm.update(assistant.copy(lorebookIds = newIds)) }
                    },
                )
            }
        }

        section(quickMessagesLabel)
        if (settings.quickMessages.isEmpty()) {
            textItem(noQuickMessagesLabel, small = true, muted = true)
        } else {
            settings.quickMessages.forEach { quickMessage ->
                selectionToggle(
                    key = "quick-message:${quickMessage.id}",
                    title = quickMessage.title.ifBlank { untitledLabel },
                    subtitle = quickMessage.content.replace('\n', ' ').trim().take(80)
                        .ifBlank { null },
                    checked = quickMessage.id in assistant.quickMessageIds,
                    onCheckedChange = { checked ->
                        val newIds = if (checked) {
                            assistant.quickMessageIds + quickMessage.id
                        } else {
                            assistant.quickMessageIds - quickMessage.id
                        }
                        scope.launch { vm.update(assistant.copy(quickMessageIds = newIds)) }
                    },
                )
            }
        }

        section(skillsLabel)
        if (skills.isEmpty()) {
            textItem(noSkillsLabel, small = true, muted = true)
        } else {
            skills.forEach { skill ->
                selectionToggle(
                    key = "skill:${skill.name}",
                    title = skill.name,
                    subtitle = skill.description.replace('\n', ' ').trim().take(80)
                        .ifBlank { null },
                    checked = skill.name in assistant.enabledSkills,
                    onCheckedChange = { checked ->
                        val newSkills = if (checked) {
                            assistant.enabledSkills + skill.name
                        } else {
                            assistant.enabledSkills - skill.name
                        }
                        scope.launch { vm.update(assistant.copy(enabledSkills = newSkills)) }
                    },
                )
            }
        }

        infoItem(
            assignedLabel,
            assignedSummary,
        )
    }
}

/** 动态列表里的多选行：key 用实体 id，避免同名条目触发 LazyColumn 的重复 key。 */
private fun WearListScope.selectionToggle(
    key: String,
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    item(key = key) {
        CheckboxButton(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.fillMaxWidth(),
            secondaryLabel = if (subtitle != null) {
                {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else {
                null
            },
            label = {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
        )
    }
}
