package me.rerere.rikkahub.wear.pages.extensions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.rerere.rikkahub.R
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

    // WearScreen 的 content 是普通 DSL builder（非 @Composable），字符串要先在外部取好。
    val newSkillTitle = stringResource(R.string.wear_ext_new_skill)
    val skillsTitle = stringResource(R.string.wear_ext_skills_title)
    val nameLabel = stringResource(R.string.wear_ext_name)
    val namePlaceholder = stringResource(R.string.wear_ext_skill_name_placeholder)
    val nameHint = stringResource(R.string.wear_ext_skill_name_hint)
    val skillMd = stringResource(R.string.wear_ext_skill_md)
    val contentPlaceholder = stringResource(R.string.wear_ext_skill_content_placeholder)
    val saveSkill = stringResource(R.string.wear_ext_save_skill)
    val skillSavedMessage = stringResource(R.string.wear_ext_skill_saved)
    val skillSaveFailedMessage = stringResource(R.string.wear_ext_skill_save_failed)
    val skillDeletedMessage = stringResource(R.string.wear_ext_skill_deleted)
    val deleteSkillTitle = stringResource(R.string.wear_ext_skill_delete_title)
    val deleteSkill = stringResource(R.string.wear_ext_delete_skill)
    val delete = stringResource(R.string.wear_ext_delete)
    val dangerSection = stringResource(R.string.wear_ext_danger)
    val sourceLabel = stringResource(R.string.wear_ext_skill_source)
    val builtinLabel = stringResource(R.string.wear_ext_builtin)
    val localLabel = stringResource(R.string.wear_ext_local)
    val noDescription = stringResource(R.string.wear_ext_no_description)
    val compatibilityLabel = stringResource(R.string.wear_ext_compatibility)
    val assistantSection = stringResource(R.string.wear_ext_assistant_section)
    val enabledForCurrent = stringResource(R.string.wear_ext_enabled_for_current)
    val currentAssistant = stringResource(R.string.wear_ext_current_assistant)
    val noActiveAssistant = stringResource(R.string.wear_ext_no_active_assistant)
    val loading = stringResource(R.string.wear_ext_loading)
    val builtinSkill = stringResource(R.string.wear_ext_builtin_skill)
    val localSkill = stringResource(R.string.wear_ext_local_skill)
    val onLabel = stringResource(R.string.wear_ext_on)
    val skillsEmpty = stringResource(R.string.wear_ext_skills_empty)
    val skillsHint = stringResource(R.string.wear_ext_skills_hint)

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
            WearScreen(title = newSkillTitle, onBack = { draft = null }) {
                item(key = "skill-name") {
                    WearTextField(
                        value = currentDraft.name,
                        onValueChange = { value -> draft = currentDraft.copy(name = value) },
                        label = nameLabel,
                        placeholder = namePlaceholder,
                        singleLine = true,
                    )
                }
                textItem(
                    nameHint,
                    small = true,
                    muted = true,
                )
                item(key = "skill-content") {
                    WearTextField(
                        value = currentDraft.content,
                        onValueChange = { value -> draft = currentDraft.copy(content = value) },
                        label = skillMd,
                        placeholder = contentPlaceholder,
                        singleLine = false,
                        minLines = 10,
                    )
                }
                buttonItem(
                    label = saveSkill,
                    icon = WearIcons.Check,
                    enabled = currentDraft.name.isNotBlank() && currentDraft.content.isNotBlank(),
                    onClick = {
                        val saving = currentDraft
                        draft = null
                        vm.saveSkill(saving.name.trim(), saving.content) { success ->
                            toaster.show(
                                message = if (success) skillSavedMessage else skillSaveFailedMessage,
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
                infoItem(sourceLabel, if (skill.builtin) builtinLabel else localLabel)
                textItem(skill.description.ifBlank { noDescription })
                if (!skill.compatibility.isNullOrBlank()) {
                    infoItem(compatibilityLabel, skill.compatibility.orEmpty())
                }
                section(assistantSection)
                toggleItem(
                    title = enabledForCurrent,
                    subtitle = assistant?.name?.ifBlank { currentAssistant } ?: noActiveAssistant,
                    checked = enabled,
                    enabled = assistant != null,
                    onCheckedChange = { checked -> setEnabled(skill.name, checked) },
                )
                section(skillMd)
                textItem(
                    text = content.ifBlank { loading },
                    maxLines = 120,
                    small = true,
                    muted = true,
                )
                if (!skill.builtin) {
                    section(dangerSection)
                    buttonItem(label = deleteSkill, icon = WearIcons.Delete, tonal = true) {
                        pendingDelete = skill
                    }
                }
            }
        }

        else -> {
            WearScreen(title = skillsTitle, onBack = onBack) {
                buttonItem(label = newSkillTitle, icon = WearIcons.Add, tonal = true) {
                    draft = SkillDraft(name = "", content = "")
                }
                if (skills.isEmpty()) {
                    textItem(skillsEmpty)
                } else {
                    skills.forEach { skill ->
                        navItem(
                            title = skill.name,
                            subtitle = skill.description
                                .replace('\n', ' ')
                                .trim()
                                .ifBlank { if (skill.builtin) builtinSkill else localSkill }
                                .take(80),
                            icon = WearIcons.Skills,
                            trailingText = if (assistant?.enabledSkills?.contains(skill.name) == true) {
                                onLabel
                            } else {
                                null
                            },
                            onClick = { selected = skill },
                        )
                    }
                }
                textItem(
                    skillsHint,
                    small = true,
                    muted = true,
                )
            }
        }
    }

    pendingDelete?.let { target ->
        WearConfirmDialog(
            visible = true,
            title = deleteSkillTitle,
            message = target.name,
            confirmLabel = delete,
            danger = true,
            onConfirm = {
                pendingDelete = null
                selected = null
                vm.deleteSkill(target)
                toaster.show(skillDeletedMessage, ToastType.Success)
            },
            onDismiss = { pendingDelete = null },
        )
    }
}
