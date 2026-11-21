package me.rerere.rikkahub.wear.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.wear.compose.material3.ColorScheme as WearColorScheme
import androidx.wear.compose.material3.MaterialTheme as WearMaterialTheme
import androidx.wear.compose.material3.Shapes as WearShapes
import androidx.wear.compose.material3.Typography as WearTypography

/**
 * 把手机的 Material 3 配色映射成 Wear Material 3 的配色。
 *
 * Wear M3 的 [WearColorScheme] 没有 surface 一族里的 `surface`，只有 surfaceContainerLow/Mid/High，
 * 所以这里按容器层级一一对应；primaryDim 等「暗态」色没有等价项，直接用对应主色。
 */
fun ColorScheme.toWearColorScheme(): WearColorScheme = WearColorScheme(
    primary = primary,
    primaryDim = primary,
    primaryContainer = primaryContainer,
    onPrimary = onPrimary,
    onPrimaryContainer = onPrimaryContainer,
    secondary = secondary,
    secondaryDim = secondary,
    secondaryContainer = secondaryContainer,
    onSecondary = onSecondary,
    onSecondaryContainer = onSecondaryContainer,
    tertiary = tertiary,
    tertiaryDim = tertiary,
    tertiaryContainer = tertiaryContainer,
    onTertiary = onTertiary,
    onTertiaryContainer = onTertiaryContainer,
    surfaceContainerLow = surfaceContainerLow,
    surfaceContainer = surfaceContainer,
    surfaceContainerHigh = surfaceContainerHigh,
    onSurface = onSurface,
    onSurfaceVariant = onSurfaceVariant,
    outline = outline,
    outlineVariant = outlineVariant,
    background = background,
    onBackground = onBackground,
    error = error,
    errorDim = error,
    errorContainer = errorContainer,
    onError = onError,
    onErrorContainer = onErrorContainer,
)

/**
 * 手表端主题：在现有的 RikkaHub Material 3 主题之上再提供一套 Wear Material 3 的
 * colorScheme / typography / shapes / motionScheme。
 *
 * 手机构建不会引用本模块，因此手机 APK 不包含 Wear Compose 依赖。
 */
@Composable
fun RikkaWearTheme(
    colorScheme: ColorScheme,
    content: @Composable () -> Unit,
) {
    val wearColorScheme = remember(colorScheme) { colorScheme.toWearColorScheme() }
    val typography = remember { WearTypography() }
    val shapes = remember { WearShapes() }
    WearMaterialTheme(
        colorScheme = wearColorScheme,
        typography = typography,
        shapes = shapes,
        content = content,
    )
}
