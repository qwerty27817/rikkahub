package me.rerere.rikkahub.wear.pages.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.rerere.rikkahub.R
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

    // WearScreen 的 content 是普通 DSL builder（非 @Composable），先算好所有字符串。
    val title = stringResource(R.string.wear_common_screen_shape)
    val modeTitle = stringResource(R.string.wear_set_mode)
    val shapeAutoLabel = stringResource(R.string.wear_common_auto)
    val shapeDetectedFormat = stringResource(R.string.wear_common_detected_auto)
    val shapeForceSquareLabel = stringResource(R.string.wear_common_force_square)
    val shapeForceRoundLabel = stringResource(R.string.wear_common_force_round)
    val shapeDisplayLabel = stringResource(R.string.wear_common_display)
    val diagnosticsSection = stringResource(R.string.wear_set_diagnostics)
    val autoDescription = stringResource(R.string.wear_set_shape_auto_desc)
    val debugLine = stringResource(
        R.string.wear_set_shape_debug_line,
        state.shape.name.lowercase(),
        state.source.name.lowercase(),
        state.widthDp,
        state.heightDp,
        state.isWatch,
        state.systemRound,
        state.displayRound,
    )

    WearScreen(title = title, onBack = onBack) {
        if (controller != null) {
            shapeModeSetting(
                controller = controller,
                state = state,
                title = modeTitle,
                autoLabel = shapeAutoLabel,
                autoDetectedFormat = shapeDetectedFormat,
                forceSquareLabel = shapeForceSquareLabel,
                forceRoundLabel = shapeForceRoundLabel,
                displayLabel = shapeDisplayLabel,
            )
        }
        section(diagnosticsSection)
        textItem(text = autoDescription)
        textItem(
            text = debugLine,
            small = true,
            muted = true,
        )
    }
}
