package me.rerere.rikkahub.wear.pages

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.rerere.rikkahub.R
import me.rerere.rikkahub.wear.components.WearScreen
import me.rerere.rikkahub.wear.components.infoItem
import me.rerere.rikkahub.wear.components.textItem

/** 尚未迁移到手表 UI 的功能页占位，保证导航可达、不会空栈。 */
@Composable
fun WearPlaceholderPage(
    title: String,
    description: String,
    onBack: () -> Unit,
) {
    // WearScreen 的 content 是普通 DSL builder（非 @Composable），先算好所有字符串。
    val statusLabel = stringResource(R.string.wear_misc_placeholder_status)
    val notPorted = stringResource(R.string.wear_misc_placeholder_not_ported)
    WearScreen(title = title, onBack = onBack) {
        textItem(description)
        infoItem(statusLabel, notPorted)
    }
}
