package me.rerere.rikkahub.wear.pages.extensions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

    WearScreen(title = "Extensions", onBack = onBack) {
        navItem(
            title = "Prompt injections",
            subtitle = "${settings.modeInjections.size} modes · ${settings.lorebooks.size} lorebooks",
            icon = WearIcons.Ai,
            onClick = onOpenPrompts,
        )
        navItem(
            title = "Quick messages",
            subtitle = "${settings.quickMessages.size} saved",
            icon = WearIcons.Messages,
            onClick = onOpenQuickMessages,
        )
        navItem(
            title = "Agent skills",
            subtitle = "${skills.size} installed",
            icon = WearIcons.Skills,
            onClick = onOpenSkills,
        )
        textItem(
            "Extensions are shared by all assistants. Assign them to an assistant from Settings → Assistants.",
            small = true,
            muted = true,
        )
    }
}
