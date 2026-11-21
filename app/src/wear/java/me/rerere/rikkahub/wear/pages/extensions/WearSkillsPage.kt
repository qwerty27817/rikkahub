package me.rerere.rikkahub.wear.pages.extensions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.skills.SkillMetadata
import me.rerere.rikkahub.ui.context.LocalSettings
import me.rerere.rikkahub.ui.context.LocalToaster
import me.rerere.rikkahub.ui.pages.extensions.skills.SkillsVM
import me.rerere.rikkahub.wear.WearIcons
import me.rerere.rikkahub.wear.components.WearConfirmDialog
import me.rerere.rikkahub.wear.components.WearScreen
import me.rerere.rikkahub.wear.components.WearTextField
import me.rerere.rikkahub.wear.components.buttonItem
import me.rerere.rikkahub.wear.components.infoItem
import me.rerere.rikkahub.wear.components.navItem
import me.rerere.rikkahub.wear.components.section
import me.rerere.rikkahub.wear.components.textItem
import me.rerere.rikkahub.wear.components.toggleItem
import me.rerere.ui.toast.ToastType
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

private data class SkillDraft(
    val name: String,
    val content: String,
)

/** 技能的安装 / 查看 / 删除，以及为当前助手启用技能（enabledSkills）。 */
@Composable
fun WearSkillsPage(
    onBack: () -> Unit,
) {
    val vm: SkillsVM = koinViewModel()
    val skills by vm.skills.collectAsStateWithLifecycle()
    val settings = LocalSettings.current
    val store: SettingsStore = koinInject()
    val scope = rememberCoroutineScope()
    val toaster = LocalToaster.current
    val assistant = settings.assistants.find { it.id == settings.assistantId }

    var selected by remember { mutableStateOf<SkillMetadata?>(null) }
    var draft by remember { mutableStateOf<SkillDraft?>(null) }
    var pendingDelete by remember { mutableStateOf<SkillMetadata?>(null) }

    fun setEnabled(skillName: String, enabled: Boolean) {
        scope.launch {
            store.update { current ->
                current.copy(
                    assistants = current.assistants.map { assistantToUpdate ->
                        if (assistantToUpdate.id == current.assistantId) {
                            assistantToUpdate.copy(
                                enabledSkills = if (enabled) {
                                    assistantToUpdate.enabledSkills + skillName
                                } else {
                                    assistantToUpdate.enabledSkills - skillName
                                }
                            )
                        } else {
                            assistantToUpdate
                        }
                    }
                )
            }
        }
    }

    val currentDraft = draft
    val currentSelected = selected
    when {
        currentDraft != null -> {
            WearScreen(title = "New skill", onBack = { draft = null }) {
                item(key = "skill-name") {
                    WearTextField(
                        value = currentDraft.name,
                        onValueChange = { value -> draft = currentDraft.copy(name = value) },
                        label = "Name",
                        placeholder = "my-skill",
                        singleLine = true,
                    )
                }
                textItem(
                    "The name becomes the skill folder. SKILL.md needs name and description in its frontmatter.",
                    small = true,
                    muted = true,
                )
                item(key = "skill-content") {
                    WearTextField(
                        value = currentDraft.content,
                        onValueChange = { value -> draft = currentDraft.copy(content = value) },
                        label = "SKILL.md",
                        placeholder = "---\nname: my-skill\ndescription: When to use it\n---\n",
                        singleLine = false,
                        minLines = 10,
                    )
                }
                buttonItem(
                    label = "Save skill",
                    icon = WearIcons.Check,
                    enabled = currentDraft.name.isNotBlank() && currentDraft.content.isNotBlank(),
                    onClick = {
                        val saving = currentDraft
                        draft = null
                        vm.saveSkill(saving.name.trim(), saving.content) { success ->
                            toaster.show(
                                message = if (success) "Skill saved" else "Failed to save skill",
                                type = if (success) ToastType.Success else ToastType.Error,
                            )
                        }
                    },
                )
            }
        }

        currentSelected != null -> {
            val skill = currentSelected
            val enabled = assistant?.enabledSkills?.contains(skill.name) == true
            val content by produceState(initialValue = "", key1 = skill.name) {
                value = withContext(Dispatchers.IO) {
                    runCatching { skill.skillFile.readText() }.getOrDefault("")
                }
            }
            WearScreen(title = skill.name, onBack = { selected = null }) {
                infoItem("Source", if (skill.builtin) "Built-in" else "Local")
                textItem(skill.description.ifBlank { "No description." })
                if (!skill.compatibility.isNullOrBlank()) {
                    infoItem("Compatibility", skill.compatibility.orEmpty())
                }
                section("Assistant")
                toggleItem(
                    title = "Enabled for current assistant",
                    subtitle = assistant?.name?.ifBlank { "Current assistant" } ?: "No active assistant",
                    checked = enabled,
                    enabled = assistant != null,
                    onCheckedChange = { checked -> setEnabled(skill.name, checked) },
                )
                section("SKILL.md")
                textItem(
                    text = content.ifBlank { "Loading…" },
                    maxLines = 120,
                    small = true,
                    muted = true,
                )
                if (!skill.builtin) {
                    section("Danger")
                    buttonItem(label = "Delete skill", icon = WearIcons.Delete, tonal = true) {
                        pendingDelete = skill
                    }
                }
            }
        }

        else -> {
            WearScreen(title = "Skills", onBack = onBack) {
                buttonItem(label = "New skill", icon = WearIcons.Add, tonal = true) {
                    draft = SkillDraft(name = "", content = "")
                }
                if (skills.isEmpty()) {
                    textItem(
                        "No skills installed. Skills are folders with a SKILL.md the assistant can load when relevant.",
                    )
                } else {
                    skills.forEach { skill ->
                        navItem(
                            title = skill.name,
                            subtitle = skill.description
                                .replace('\n', ' ')
                                .trim()
                                .ifBlank { if (skill.builtin) "Built-in skill" else "Local skill" }
                                .take(80),
                            icon = WearIcons.Skills,
                            trailingText = if (assistant?.enabledSkills?.contains(skill.name) == true) "On" else null,
                            onClick = { selected = skill },
                        )
                    }
                }
                textItem(
                    "Tap a skill to read it and enable it for the current assistant.",
                    small = true,
                    muted = true,
                )
            }
        }
    }

    pendingDelete?.let { target ->
        WearConfirmDialog(
            visible = true,
            title = "Delete skill?",
            message = target.name,
            confirmLabel = "Delete",
            danger = true,
            onConfirm = {
                pendingDelete = null
                selected = null
                vm.deleteSkill(target)
                toaster.show("Skill deleted", ToastType.Success)
            },
            onDismiss = { pendingDelete = null },
        )
    }
}
