package me.rerere.rikkahub.wear.pages.assistant

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.CheckboxButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import kotlinx.coroutines.launch
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

    if (assistant == null) {
        WearScreen(title = "Injections", onBack = onBack) {
            textItem("This assistant no longer exists.")
        }
        return
    }

    WearScreen(title = "Prompt injections", onBack = onBack) {
        textItem(
            "Pick the prompt extensions this assistant may use. Create them in Settings → Extensions.",
            small = true,
            muted = true,
        )

        section("Mode injections")
        if (settings.modeInjections.isEmpty()) {
            textItem("No mode injections yet.", small = true, muted = true)
        } else {
            settings.modeInjections.forEach { injection ->
                selectionToggle(
                    key = "mode:${injection.id}",
                    title = injection.name.ifBlank { "Mode injection" },
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

        section("Lorebooks")
        if (settings.lorebooks.isEmpty()) {
            textItem("No lorebooks yet.", small = true, muted = true)
        } else {
            settings.lorebooks.forEach { lorebook ->
                selectionToggle(
                    key = "lorebook:${lorebook.id}",
                    title = lorebook.name.ifBlank { "Lorebook" },
                    subtitle = "${lorebook.entries.size} entries" +
                        lorebook.description.take(60).takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty(),
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

        section("Quick messages")
        if (settings.quickMessages.isEmpty()) {
            textItem("No quick messages yet.", small = true, muted = true)
        } else {
            settings.quickMessages.forEach { quickMessage ->
                selectionToggle(
                    key = "quick-message:${quickMessage.id}",
                    title = quickMessage.title.ifBlank { "Untitled" },
                    subtitle = quickMessage.content.replace('\n', ' ').trim().take(80).ifBlank { null },
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

        section("Skills")
        if (skills.isEmpty()) {
            textItem("No skills installed.", small = true, muted = true)
        } else {
            skills.forEach { skill ->
                selectionToggle(
                    key = "skill:${skill.name}",
                    title = skill.name,
                    subtitle = skill.description.replace('\n', ' ').trim().take(80).ifBlank { null },
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
            "Assigned",
            "${assistant.modeInjectionIds.size} modes · ${assistant.lorebookIds.size} lorebooks · " +
                "${assistant.quickMessageIds.size} messages · ${assistant.enabledSkills.size} skills",
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
