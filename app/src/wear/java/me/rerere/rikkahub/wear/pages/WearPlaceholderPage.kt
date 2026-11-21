package me.rerere.rikkahub.wear.pages

import androidx.compose.runtime.Composable
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
    WearScreen(title = title, onBack = onBack) {
        textItem(description)
        infoItem("Status", "Not ported yet")
    }
}
