package me.rerere.rikkahub.wear.pages.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.ui.context.LocalSettings
import me.rerere.rikkahub.ui.hooks.rememberAmoledDarkMode
import me.rerere.rikkahub.ui.hooks.rememberColorMode
import me.rerere.rikkahub.ui.theme.ColorMode
import me.rerere.rikkahub.ui.theme.PresetThemes
import me.rerere.rikkahub.wear.WearIcons
import me.rerere.rikkahub.wear.components.WearOptionDialog
import me.rerere.rikkahub.wear.components.WearScreen
import me.rerere.rikkahub.wear.components.infoItem
import me.rerere.rikkahub.wear.components.navItem
import me.rerere.rikkahub.wear.components.radioItem
import me.rerere.rikkahub.wear.components.section
import me.rerere.rikkahub.wear.components.textItem
import me.rerere.rikkahub.wear.components.toggleItem
import org.koin.compose.koinInject

/**
 * 手表端外观设置。
 *
 * 复用手机端的两个偏好来源：
 * - 明暗模式走 `ui/hooks/ColorMode.kt` 的 SharedPreferences（`RikkahubTheme` 读同一个值）；
 * - 动态取色 / 主题 / 自定义主题走 [SettingsStore]（`RikkahubTheme` 读同一份 Settings）。
 *
 * 注意：`WearScreen` 的 content 是普通 DSL builder（非 @Composable），
 * 所以所有 `stringResource` 都要在进入 DSL 之前算好。
 */
@Composable
fun WearAppearancePage(
    onBack: () -> Unit,
) {
    val settings = LocalSettings.current
    val store: SettingsStore = koinInject()
    val scope = rememberCoroutineScope()
    var colorMode by rememberColorMode()
    var amoledDarkMode by rememberAmoledDarkMode()
    var showThemePicker by remember { mutableStateOf(false) }

    val systemLabel = stringResource(R.string.setting_page_color_mode_system)
    val lightLabel = stringResource(R.string.setting_page_color_mode_light)
    val darkLabel = stringResource(R.string.setting_page_color_mode_dark)
    val dynamicColorTitle = stringResource(R.string.setting_page_dynamic_color)
    val dynamicColorDesc = stringResource(R.string.setting_page_dynamic_color_desc)
    val amoledTitle = stringResource(R.string.setting_display_page_amoled_dark_mode_title)
    val amoledDesc = stringResource(R.string.setting_display_page_amoled_dark_mode_desc)
    val presetLabels = presetThemeLabels()
    val customThemeLabels = settings.customThemes.map { theme ->
        theme.id to theme.name.ifBlank { "Custom theme" }
    }
    val themeChoices = presetLabels + customThemeLabels
    val selectedThemeLabel = themeChoices.firstOrNull { it.first == settings.themeId }?.second
        ?: settings.themeId
    val selectedThemeIndex = themeChoices.indexOfFirst { it.first == settings.themeId }

    WearScreen(title = "Appearance", onBack = onBack) {
        section("Color mode")
        radioItem(
            title = systemLabel,
            selected = colorMode == ColorMode.SYSTEM,
            onSelect = { colorMode = ColorMode.SYSTEM },
        )
        radioItem(
            title = lightLabel,
            selected = colorMode == ColorMode.LIGHT,
            onSelect = { colorMode = ColorMode.LIGHT },
        )
        radioItem(
            title = darkLabel,
            selected = colorMode == ColorMode.DARK,
            onSelect = { colorMode = ColorMode.DARK },
        )

        section("Theme")
        toggleItem(
            title = dynamicColorTitle,
            checked = settings.dynamicColor,
            subtitle = dynamicColorDesc,
            onCheckedChange = { enabled ->
                scope.launch { store.update { it.copy(dynamicColor = enabled) } }
            },
        )
        if (settings.dynamicColor) {
            textItem(
                text = "Dynamic color replaces preset colors on Android 12+.",
                maxLines = 3,
                small = true,
                muted = true,
            )
        }
        navItem(
            title = "Theme preset",
            subtitle = selectedThemeLabel,
            icon = WearIcons.Theme,
            onClick = { showThemePicker = true },
        )
        if (settings.customThemes.isEmpty()) {
            textItem(
                text = "Create or import custom themes on the phone app.",
                maxLines = 3,
                small = true,
                muted = true,
            )
        }

        section("Dark")
        toggleItem(
            title = amoledTitle,
            checked = amoledDarkMode,
            subtitle = amoledDesc,
            onCheckedChange = { amoledDarkMode = it },
        )
        infoItem("Selected theme", selectedThemeLabel)
    }

    if (showThemePicker) {
        WearOptionDialog(
            visible = true,
            title = "Theme preset",
            options = themeChoices.map { it.second },
            selectedIndex = selectedThemeIndex,
            onSelect = { index ->
                val themeId = themeChoices.getOrNull(index)?.first
                if (themeId != null) {
                    showThemePicker = false
                    scope.launch { store.update { it.copy(themeId = themeId) } }
                }
            },
            onDismiss = { showThemePicker = false },
        )
    }
}

/** PresetTheme 的名字是 Composable lambda，这里按 id 取同一份字符串资源。 */
@Composable
private fun presetThemeLabels(): List<Pair<String, String>> {
    val sakura = stringResource(R.string.theme_name_sakura)
    val ocean = stringResource(R.string.theme_name_ocean)
    val spring = stringResource(R.string.theme_name_spring)
    val autumn = stringResource(R.string.theme_name_autumn)
    val black = stringResource(R.string.theme_name_black)
    val minimal = stringResource(R.string.theme_name_minimal)
    val claude = stringResource(R.string.theme_name_claude)
    return PresetThemes.map { theme ->
        theme.id to when (theme.id) {
            "sakura" -> sakura
            "ocean" -> ocean
            "spring" -> spring
            "autumn" -> autumn
            "black" -> black
            "minimal" -> minimal
            "claude" -> claude
            else -> theme.id
        }
    }
}
