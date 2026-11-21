package me.rerere.rikkahub.wear.pages.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import me.rerere.rikkahub.data.ai.UsageSource
import me.rerere.rikkahub.ui.pages.stats.StatsVM
import me.rerere.rikkahub.ui.pages.stats.UsageRange
import me.rerere.rikkahub.wear.WearIcons
import me.rerere.rikkahub.wear.components.WearOptionDialog
import me.rerere.rikkahub.wear.components.WearScreen
import me.rerere.rikkahub.wear.components.infoItem
import me.rerere.rikkahub.wear.components.navItem
import me.rerere.rikkahub.wear.components.section
import me.rerere.rikkahub.wear.components.textItem
import org.koin.androidx.compose.koinViewModel

@Composable
fun WearStatsPage(
    onBack: () -> Unit,
) {
    val vm: StatsVM = koinViewModel()
    val stats by vm.stats.collectAsStateWithLifecycle()
    val usage by vm.usage.collectAsStateWithLifecycle()
    val range by vm.range.collectAsStateWithLifecycle()
    var rangePicker by remember { mutableStateOf(false) }

    WearScreen(title = "Statistics", onBack = onBack) {
        if (stats.isLoading) {
            textItem("Loading statistics…", small = true, muted = true)
        } else {
            section("Overview")
            infoItem("Conversations", formatTokens(stats.totalConversations.toLong()))
            infoItem("Messages", formatTokens(stats.totalMessages.toLong()))
            infoItem("Launches", formatTokens(stats.launchCount.toLong()))
            infoItem("Active days", stats.requestsPerDay.count { it.value > 0 }.toString())
        }

        section("Token usage")
        navItem(
            title = "Range",
            subtitle = usageRangeLabel(range),
            icon = WearIcons.Stats,
        ) { rangePicker = true }

        val currentUsage = usage
        if (currentUsage == null) {
            textItem("Loading usage…", small = true, muted = true)
        } else {
            val totals = currentUsage.totals
            infoItem("Input tokens", formatTokens(totals.promptTokens))
            infoItem("Output tokens", formatTokens(totals.completionTokens))
            infoItem("Cached tokens", formatTokens(totals.cachedTokens))
            infoItem(
                "Cache hit",
                if (totals.promptTokens > 0) {
                    "%.1f%%".format(totals.cachedTokens * 100.0 / totals.promptTokens)
                } else {
                    "-"
                },
            )
            infoItem("Total tokens", formatTokens(totals.promptTokens + totals.completionTokens))
            infoItem("Buckets", if (currentUsage.byMonth) "Monthly" else "Daily")

            if (currentUsage.models.isNotEmpty()) {
                section("By model")
                currentUsage.models
                    .sortedByDescending { it.totalTokens }
                    .take(8)
                    .forEachIndexed { index, model ->
                        item(key = "model:$index") {
                            UsageRow(
                                label = model.name ?: "Unknown model",
                                value = formatTokens(model.totalTokens),
                            )
                        }
                    }
            }

            if (currentUsage.sources.isNotEmpty()) {
                section("By source")
                currentUsage.sources
                    .sortedByDescending { it.promptTokens + it.completionTokens }
                    .forEachIndexed { index, source ->
                        item(key = "source:$index") {
                            UsageRow(
                                label = sourceLabel(source.source),
                                value = formatTokens(source.promptTokens + source.completionTokens),
                            )
                        }
                    }
            }

            if (currentUsage.points.isNotEmpty()) {
                section(if (currentUsage.byMonth) "Monthly totals" else "Daily totals")
                currentUsage.points
                    .takeLast(7)
                    .reversed()
                    .forEachIndexed { index, point ->
                        item(key = "point:$index") {
                            UsageRow(
                                label = point.date.toString(),
                                value = formatTokens(point.totalTokens),
                            )
                        }
                    }
            }
        }
    }

    if (rangePicker) {
        WearOptionDialog(
            visible = true,
            title = "Usage range",
            options = UsageRange.entries.map { usageRangeLabel(it) },
            selectedIndex = range.ordinal,
            onSelect = { index ->
                rangePicker = false
                UsageRange.entries.getOrNull(index)?.let(vm::setRange)
            },
            onDismiss = { rangePicker = false },
        )
    }
}

@Composable
private fun UsageRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

private fun usageRangeLabel(range: UsageRange): String = when (range) {
    UsageRange.WEEK -> "Last 7 days"
    UsageRange.MONTH -> "Last 30 days"
    UsageRange.QUARTER -> "Last 90 days"
    UsageRange.ALL -> "All time"
}

private fun sourceLabel(source: String): String {
    val usageSource = UsageSource.entries.find { it.name == source }
    return when (usageSource) {
        UsageSource.CHAT -> "Chat"
        UsageSource.COMPRESSION -> "Compression"
        UsageSource.TITLE -> "Title"
        UsageSource.SUGGESTION -> "Suggestion"
        UsageSource.TRANSLATION -> "Translation"
        UsageSource.MEMORY -> "Memory"
        UsageSource.OCR -> "OCR"
        null -> source
    }
}

private fun formatTokens(count: Long): String = when {
    count >= 1_000_000_000 -> "%.2fB".format(count / 1_000_000_000.0)
    count >= 1_000_000 -> "%.2fM".format(count / 1_000_000.0)
    count >= 1_000 -> "%.1fK".format(count / 1_000.0)
    else -> count.toString()
}
