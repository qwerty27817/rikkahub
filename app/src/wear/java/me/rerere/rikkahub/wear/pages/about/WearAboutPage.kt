package me.rerere.rikkahub.wear.pages.about

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import me.rerere.rikkahub.BuildConfig
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

    WearScreen(title = "About", onBack = onBack) {
        textItem("RikkaHub for Wear OS / Android watches")
        section("Build")
        infoItem("Version", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        infoItem("ABIs", Build.SUPPORTED_ABIS.joinToString(", "))
        infoItem("Device", "${Build.MANUFACTURER} ${Build.MODEL}")
        infoItem("Android", "API ${Build.VERSION.SDK_INT}")
        section("Display")
        infoItem("Screen", shapeState.diagnosis)
        infoItem("Source", shapeState.source.name)
        section("Links")
        buttonItem(label = "GitHub repository", tonal = true) {
            uriHandler.openUri("https://github.com/rikkahub/rikkahub")
        }
    }
}
