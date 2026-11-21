package me.rerere.rikkahub.wear.pages.webview

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import me.rerere.rikkahub.R
import me.rerere.rikkahub.wear.WearIcons
import me.rerere.rikkahub.wear.components.WearScreen
import me.rerere.rikkahub.wear.components.WearTextField
import me.rerere.rikkahub.wear.components.buttonItem
import me.rerere.rikkahub.wear.components.textItem
import me.rerere.ui.webview.WEB_VIEW_BASE_URL
import me.rerere.ui.webview.WebView
import me.rerere.ui.webview.WebViewContentCache
import me.rerere.ui.webview.rememberWebViewState

@Composable
fun WearWebViewPage(
    url: String,
    contentId: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val cachedHtml = remember(contentId) {
        if (contentId.isBlank()) null else WebViewContentCache.load(context.cacheDir, contentId)
    }
    var openedUrl by remember(url) { mutableStateOf(url.takeIf { it.isNotBlank() }) }

    val activeUrl = openedUrl
    if (activeUrl == null && cachedHtml == null) {
        WearWebUrlInput(
            initialUrl = url,
            onBack = onBack,
            onOpen = { openedUrl = it },
        )
    } else {
        WearWebContent(
            initialUrl = activeUrl,
            cachedHtml = cachedHtml,
            onBack = onBack,
        )
    }
}

@Composable
private fun WearWebUrlInput(
    initialUrl: String,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
) {
    var input by remember(initialUrl) { mutableStateOf(initialUrl) }

    // WearScreen 的 content 是普通 DSL builder（非 @Composable），先算好所有字符串。
    val title = stringResource(R.string.wear_misc_webview_title)
    val hint = stringResource(R.string.wear_misc_webview_hint)
    val urlLabel = stringResource(R.string.wear_misc_webview_url)
    val urlPlaceholder = stringResource(R.string.wear_misc_webview_url_placeholder)
    val open = stringResource(R.string.wear_misc_open)

    WearScreen(title = title, onBack = onBack) {
        textItem(hint, small = true, muted = true)
        item(key = "url") {
            WearTextField(
                value = input,
                onValueChange = { input = it },
                label = urlLabel,
                placeholder = urlPlaceholder,
                singleLine = true,
                keyboardType = KeyboardType.Uri,
            )
        }
        buttonItem(
            label = open,
            icon = WearIcons.Web,
            enabled = input.isNotBlank(),
        ) {
            normalizeUrl(input)?.let(onOpen)
        }
    }
}

@Composable
private fun WearWebContent(
    initialUrl: String?,
    cachedHtml: String?,
    onBack: () -> Unit,
) {
    val state = if (initialUrl == null) {
        rememberWebViewState(
            data = cachedHtml.orEmpty(),
            baseUrl = WEB_VIEW_BASE_URL,
            mimeType = "text/html",
            settings = {
                builtInZoomControls = true
                displayZoomControls = false
                useWideViewPort = true
                loadWithOverviewMode = true
            },
        )
    } else {
        rememberWebViewState(
            url = initialUrl,
            settings = {
                builtInZoomControls = true
                displayZoomControls = false
                useWideViewPort = true
                loadWithOverviewMode = true
            },
        )
    }
    val handleBack: () -> Unit = {
        if (state.canGoBack) state.goBack() else onBack()
    }
    BackHandler { handleBack() }

    Box(modifier = Modifier.fillMaxSize()) {
        WebView(
            state = state,
            modifier = Modifier.fillMaxSize(),
        )
        FilledTonalIconButton(
            onClick = handleBack,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
                .size(40.dp),
        ) {
            Icon(
                imageVector = WearIcons.Back,
                contentDescription = stringResource(R.string.wear_misc_back),
            )
        }
    }
}

private fun normalizeUrl(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    return if ("://" in trimmed) trimmed else "https://$trimmed"
}
