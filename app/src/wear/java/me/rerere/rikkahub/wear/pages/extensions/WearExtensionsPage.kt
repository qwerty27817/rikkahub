package me.rerere.rikkahub.wear.pages.extensions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.rikkahub.R
import me.rerere.rikkahub.ui.context.LocalSettings
import me.rerere.rikkahub.ui.pages.extensions.skills.SkillsVM
import me.rerere.rikkahub.wear.WearIcons
import me.rerere.rikkahub.wear.components.WearScreen
import me.rerere.rikkahub.wear.components.navItem
import me.rerere.rikkahub.wear.components.textItem
import org.koin.androidx.compose.koinViewModel

/** 扩展入口：提示词注入 / 快捷消息 / 技能，改动都是全局设置，助手页里再按助手分配。 */
@Composable
fun WearExtensionsPage(
    onBack: () -> Unit,
    onOpenPrompts: () -> Unit,
    onOpenQuickMessages: () -> Unit,
    onOpenSkills: () -> Unit,
) {
    val settings = LocalSettings.current
    val skillsVm: SkillsVM = koinViewModel<SkillsVM>()
    val skills by skillsVm.skills.collectAsStateWithLifecycle()

    // WearScreen 的 content 是普通 DSL builder（非 @Composable），字符串要先在外部取好。
    val title = stringResource(R.string.wear_ext_title)
    val promptsTitle = stringResource(R.string.wear_ext_prompts_title)
    val promptsSubtitle = stringResource(
        R.string.wear_ext_prompts_subtitle,
        settings.modeInjections.size,
        settings.lorebooks.size,
    )
    val quickMessagesTitle = stringResource(R.string.wear_ext_quick_messages_title)
    val quickMessagesSubtitle = stringResource(
        R.string.wear_ext_quick_messages_subtitle,
        settings.quickMessages.size,
    )
    val skillsTitle = stringResource(R.string.wear_ext_skills_title)
    val skillsSubtitle = stringResource(R.string.wear_ext_skills_subtitle, skills.size)
    val sharedNote = stringResource(R.string.wear_ext_shared_note)

    WearScreen(title = title, onBack = onBack) {
        navItem(
            title = promptsTitle,
            subtitle = promptsSubtitle,
            icon = WearIcons.Ai,
            onClick = onOpenPrompts,
        )
        navItem(
            title = quickMessagesTitle,
            subtitle = quickMessagesSubtitle,
            icon = WearIcons.Messages,
            onClick = onOpenQuickMessages,
        )
        navItem(
            title = skillsTitle,
            subtitle = skillsSubtitle,
            icon = WearIcons.Skills,
            onClick = onOpenSkills,
        )
        textItem(
            sharedNote,
            small = true,
            muted = true,
        )
    }
}
