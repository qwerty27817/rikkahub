package me.rerere.rikkahub.wear.pages.settings

import androidx.compose.runtime.Composable
import me.rerere.rikkahub.wear.components.WearScreen
import me.rerere.rikkahub.wear.components.section
import me.rerere.rikkahub.wear.components.shapeModeSetting
import me.rerere.rikkahub.wear.components.textItem
import me.rerere.rikkahub.wear.shape.LocalScreenShapeController
import me.rerere.rikkahub.wear.shape.LocalScreenShapeState

/**
 * 形态页：三态开关 + 判定来源诊断。
 * 手动模式立即生效并持久化；Auto 重新读取系统标志/启发式结果。
 */
@Composable
fun WearScreenShapePage(onBack: () -> Unit) {
    val state = LocalScreenShapeState.current
    val controller = LocalScreenShapeController.current

    WearScreen(title = "Screen shape", onBack = onBack) {
        if (controller != null) {
            shapeModeSetting(controller = controller, state = state, title = "Mode")
        }
        section("Diagnostics")
        textItem(
            text = "Auto uses the system round flag when the device reports one. On non-WearOS watches " +
                "that do not expose it, a square-aspect watch screen at or below 240dp is treated as round. " +
                "Manual force always wins and is stored in SharedPreferences.",
        )
        textItem(
            text = "Detected: ${state.shape.name.lowercase()} · source=${state.source.name.lowercase()} · " +
                "${state.widthDp}x${state.heightDp}dp · watch=${state.isWatch} · " +
                "systemRound=${state.systemRound} · displayRound=${state.displayRound}",
            small = true,
            muted = true,
        )
    }
}
