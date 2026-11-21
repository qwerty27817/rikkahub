package me.rerere.rikkahub.wear.pages.logs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import me.rerere.common.android.LogEntry
import me.rerere.common.android.Logging
import me.rerere.rikkahub.R
import me.rerere.rikkahub.ui.context.LocalToaster
import me.rerere.rikkahub.wear.WearIcons
import me.rerere.rikkahub.wear.components.WearConfirmDialog
import me.rerere.rikkahub.wear.components.WearOptionDialog
import me.rerere.rikkahub.wear.components.WearScreen
import me.rerere.rikkahub.wear.components.buttonItem
import me.rerere.rikkahub.wear.components.infoItem
import me.rerere.rikkahub.wear.components.navItem
import me.rerere.rikkahub.wear.components.textItem
import me.rerere.rikkahub.wear.components.toggleItem
import me.rerere.ui.toast.ToastType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class LogFilter {
    ALL,
    TEXT,
    REQUESTS,
    ERRORS;

    fun matches(entry: LogEntry): Boolean = when (this) {
        ALL -> true
        TEXT -> entry is LogEntry.TextLog
        REQUESTS -> entry is LogEntry.RequestLog
        ERRORS -> entry is LogEntry.RequestLog &&
            (entry.error != null || entry.responseCode?.let { it !in 200..299 } == true)
    }
}

@Composable
fun WearLogsPage(
    onBack: () -> Unit,
) {
    var logs by remember { mutableStateOf(Logging.getRecentLogs()) }
    var requestLoggingEnabled by remember { mutableStateOf(Logging.isRequestLoggingEnabled()) }
    var filter by remember { mutableStateOf(LogFilter.ALL) }
    var filterPicker by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val toaster = LocalToaster.current

    // WearScreen 的 content 是普通 DSL builder（非 @Composable），先算好所有字符串；
    // 点击回调里的动态 Toast 只预取带占位符的模板，回调里再格式化。
    val title = stringResource(R.string.wear_set_logs)
    val filterTitle = stringResource(R.string.wear_set_filter)
    val filterLabels = listOf(
        stringResource(R.string.wear_set_log_filter_all),
        stringResource(R.string.wear_set_log_filter_text),
        stringResource(R.string.wear_set_log_filter_requests),
        stringResource(R.string.wear_set_log_filter_errors),
    )
    val showingTitle = stringResource(R.string.wear_set_showing)
    val requestLoggingTitle = stringResource(R.string.wear_set_request_logging)
    val refreshLabel = stringResource(R.string.wear_set_refresh)
    val copyVisibleLabel = stringResource(R.string.wear_set_copy_visible)
    val copiedLogsFormat = stringResource(R.string.wear_set_copied_logs)
    val copyFailed = stringResource(R.string.wear_set_copy_failed)
    val clearLogsLabel = stringResource(R.string.wear_set_clear_logs)
    val noLogEntries = stringResource(R.string.wear_set_no_log_entries)
    val logCopied = stringResource(R.string.wear_set_log_copied)
    val filterLogsTitle = stringResource(R.string.wear_set_filter_logs)
    val clearLogsTitle = stringResource(R.string.wear_set_clear_logs_title)
    val clearLogsMessage = stringResource(R.string.wear_set_clear_logs_message)
    val clearLabel = stringResource(R.string.wear_set_clear)

    val visibleLogs = remember(logs, filter) {
        logs.sortedByDescending { it.timestamp }.filter { filter.matches(it) }
    }

    WearScreen(title = title, onBack = onBack) {
        navItem(
            title = filterTitle,
            subtitle = filterLabels[filter.ordinal],
            icon = WearIcons.File,
        ) { filterPicker = true }
        infoItem(showingTitle, "${visibleLogs.size} / ${logs.size}")
        toggleItem(
            title = requestLoggingTitle,
            checked = requestLoggingEnabled,
            onCheckedChange = { enabled ->
                requestLoggingEnabled = enabled
                Logging.setRequestLoggingEnabled(enabled)
            },
        )
        buttonItem(label = refreshLabel, icon = WearIcons.Retry, tonal = true) {
            logs = Logging.getRecentLogs()
        }
        buttonItem(
            label = copyVisibleLabel,
            icon = WearIcons.Copy,
            tonal = true,
            enabled = visibleLogs.isNotEmpty(),
        ) {
            val copied = copyToClipboard(
                context,
                visibleLogs.joinToString("\n\n") { it.toClipboardText() },
            )
            if (copied) {
                toaster.show(copiedLogsFormat.format(visibleLogs.size), ToastType.Success)
            } else {
                toaster.show(copyFailed, ToastType.Error)
            }
        }
        buttonItem(
            label = clearLogsLabel,
            icon = WearIcons.Delete,
            tonal = true,
            enabled = logs.isNotEmpty(),
        ) { confirmClear = true }

        if (visibleLogs.isEmpty()) {
            textItem(noLogEntries, small = true, muted = true)
        } else {
            visibleLogs.forEach { entry ->
                item(key = "log:${entry.id}") {
                    LogRow(
                        entry = entry,
                        onClick = {
                            if (copyToClipboard(context, entry.toClipboardText())) {
                                toaster.show(logCopied, ToastType.Success)
                            } else {
                                toaster.show(copyFailed, ToastType.Error)
                            }
                        },
                    )
                }
            }
        }
    }

    if (filterPicker) {
        WearOptionDialog(
            visible = true,
            title = filterLogsTitle,
            options = filterLabels,
            selectedIndex = filter.ordinal,
            onSelect = { index ->
                filterPicker = false
                LogFilter.entries.getOrNull(index)?.let { filter = it }
            },
            onDismiss = { filterPicker = false },
        )
    }

    WearConfirmDialog(
        visible = confirmClear,
        title = clearLogsTitle,
        message = clearLogsMessage,
        confirmLabel = clearLabel,
        dismissLabel = stringResource(R.string.wear_common_cancel),
        danger = true,
        onConfirm = {
            confirmClear = false
            Logging.clear()
            logs = Logging.getRecentLogs()
        },
        onDismiss = { confirmClear = false },
    )
}

@Composable
private fun LogRow(entry: LogEntry, onClick: () -> Unit) {
    val time = remember(entry.timestamp) {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(entry.timestamp))
    }
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = entry.tag,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = time,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            when (entry) {
                is LogEntry.TextLog -> {
                    Text(
                        text = entry.message,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                is LogEntry.RequestLog -> {
                    Text(
                        text = "${entry.method} ${entry.url}",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    entry.responseCode?.let { code ->
                        val status = stringResource(R.string.wear_set_log_status, code)
                        val duration = entry.durationMs?.let {
                            stringResource(R.string.wear_set_log_duration, it)
                        }
                        Text(
                            text = if (duration != null) "$status · $duration" else status,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (code in 200..299) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            maxLines = 1,
                        )
                    }
                    entry.error?.let { error ->
                        Text(
                            text = stringResource(R.string.wear_set_log_error, error),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

private fun LogEntry.toClipboardText(): String {
    val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
    return when (this) {
        is LogEntry.TextLog -> "[$time] $tag: $message"
        is LogEntry.RequestLog -> buildString {
            append("[$time] $method $url")
            responseCode?.let { append(" -> $it") }
            durationMs?.let { append(" (${it}ms)") }
            error?.let { append("\nerror: $it") }
            if (requestHeaders.isNotEmpty()) append("\nheaders: $requestHeaders")
            requestBody?.let { append("\nbody: $it") }
        }
    }
}

private fun copyToClipboard(context: Context, text: String): Boolean = runCatching {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    manager.setPrimaryClip(ClipData.newPlainText("log", text))
}.isSuccess
