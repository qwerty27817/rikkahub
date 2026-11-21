package me.rerere.rikkahub.wear.pages.about

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import me.rerere.rikkahub.BuildConfig
import me.rerere.rikkahub.R
import me.rerere.rikkahub.wear.components.WearScreen
import me.rerere.rikkahub.wear.components.buttonItem
import me.rerere.rikkahub.wear.components.infoItem
import me.rerere.rikkahub.wear.components.section
import me.rerere.rikkahub.wear.components.textItem
import me.rerere.rikkahub.wear.shape.LocalScreenShapeState

@Composable
fun WearAboutPage(onBack: () -> Unit) {
    val shapeState = LocalScreenShapeState.current
    val uriHandler = LocalUriHandler.current

    // WearScreen 的 content 是普通 DSL builder（非 @Composable），先算好所有字符串。
    val title = stringResource(R.string.wear_set_about_title)
    val tagline = stringResource(R.string.wear_set_about_tagline)
    val buildSection = stringResource(R.string.wear_set_section_build)
    val version = stringResource(R.string.wear_set_version)
    val abis = stringResource(R.string.wear_set_abis)
    val device = stringResource(R.string.wear_set_device)
    val android = stringResource(R.string.wear_set_android)
    val androidApi = stringResource(R.string.wear_set_android_api, Build.VERSION.SDK_INT)
    val displaySection = stringResource(R.string.wear_set_section_display)
    val screen = stringResource(R.string.wear_set_screen)
    val source = stringResource(R.string.wear_set_source)
    val linksSection = stringResource(R.string.wear_set_section_links)
    val githubRepo = stringResource(R.string.wear_set_github_repo)

    WearScreen(title = title, onBack = onBack) {
        textItem(tagline)
        section(buildSection)
        infoItem(version, "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        infoItem(abis, Build.SUPPORTED_ABIS.joinToString(", "))
        infoItem(device, "${Build.MANUFACTURER} ${Build.MODEL}")
        infoItem(android, androidApi)
        section(displaySection)
        infoItem(screen, shapeState.diagnosis)
        infoItem(source, shapeState.source.name)
        section(linksSection)
        buttonItem(label = githubRepo, tonal = true) {
            uriHandler.openUri("https://github.com/rikkahub/rikkahub")
        }
    }
}
