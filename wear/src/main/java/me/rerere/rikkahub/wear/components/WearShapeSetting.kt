package me.rerere.rikkahub.wear.components

import androidx.compose.runtime.Composable
import me.rerere.rikkahub.wear.shape.ScreenShape
import me.rerere.rikkahub.wear.shape.ScreenShapeController
import me.rerere.rikkahub.wear.shape.ScreenShapeMode
import me.rerere.rikkahub.wear.shape.ScreenShapeState

/**
 * 方/圆三态开关。写回 [ScreenShapeController]，立即生效并持久化到 SharedPreferences。
 * 圆屏与方屏的设置页共用同一份实现。
 */
fun WearListScope.shapeModeSetting(
    controller: ScreenShapeController,
    state: ScreenShapeState,
    title: String = "Screen shape",
) {
    section(title)
    radioItem(
        title = "Auto",
        subtitle = "Detected: ${state.shape.name.lowercase()} · ${state.source.name.lowercase()}",
        selected = state.mode == ScreenShapeMode.AUTO,
        onSelect = { controller.setMode(ScreenShapeMode.AUTO) },
    )
    radioItem(
        title = "Force square",
        selected = state.mode == ScreenShapeMode.SQUARE,
        onSelect = { controller.setMode(ScreenShapeMode.SQUARE) },
    )
    radioItem(
        title = "Force round",
        selected = state.mode == ScreenShapeMode.ROUND,
        onSelect = { controller.setMode(ScreenShapeMode.ROUND) },
    )
    infoItem("Display", state.diagnosis)
}

/** 单独使用的形态设置（没有现成 WearList 上下文的页面）。 */
fun WearListScope.shapeModeSettingCompact(
    controller: ScreenShapeController,
    state: ScreenShapeState,
) {
    item(key = "shape-setting") {
        ShapeModeInline(controller, state)
    }
}

@Composable
private fun ShapeModeInline(controller: ScreenShapeController, state: ScreenShapeState) {
    androidx.compose.foundation.layout.Column {
        radioRow("Auto", state.mode == ScreenShapeMode.AUTO) { controller.setMode(ScreenShapeMode.AUTO) }
        radioRow("Square", state.mode == ScreenShapeMode.SQUARE) { controller.setMode(ScreenShapeMode.SQUARE) }
        radioRow("Round", state.mode == ScreenShapeMode.ROUND) { controller.setMode(ScreenShapeMode.ROUND) }
    }
}

@Composable
private fun radioRow(title: String, selected: Boolean, onSelect: () -> Unit) {
    androidx.wear.compose.material3.RadioButton(
        selected = selected,
        onSelect = onSelect,
        label = { androidx.wear.compose.material3.Text(title) },
    )
}
