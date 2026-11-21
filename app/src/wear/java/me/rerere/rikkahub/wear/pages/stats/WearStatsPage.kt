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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import me.rerere.rikkahub.R
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

    // WearScreen 的 content 是普通 DSL builder（非 @Composable），先算好所有字符串。
    val title = stringResource(R.string.wear_set_statistics)
    val loadingStatistics = stringResource(R.string.wear_set_loading_statistics)
    val overviewSection = stringResource(R.string.wear_set_section_overview)
    val conversationsLabel = stringResource(R.string.wear_set_conversations)
    val messagesLabel = stringResource(R.string.wear_set_messages)
    val launchesLabel = stringResource(R.string.wear_set_launches)
    val activeDaysLabel = stringResource(R.string.wear_set_active_days)
    val tokenUsageSection = stringResource(R.string.wear_set_token_usage)
    val rangeTitle = stringResource(R.string.wear_set_range)
    val loadingUsage = stringResource(R.string.wear_set_loading_usage)
    val inputTokens = stringResource(R.string.wear_set_input_tokens)
    val outputTokens = stringResource(R.string.wear_set_output_tokens)
    val cachedTokens = stringResource(R.string.wear_set_cached_tokens)
    val cacheHit = stringResource(R.string.wear_set_cache_hit)
    val totalTokens = stringResource(R.string.wear_set_total_tokens)
    val bucketsLabel = stringResource(R.string.wear_set_buckets)
    val monthlyLabel = stringResource(R.string.wear_set_monthly)
    val dailyLabel = stringResource(R.string.wear_set_daily)
    val byModelSection = stringResource(R.string.wear_set_by_model)
    val bySourceSection = stringResource(R.string.wear_set_by_source)
    val monthlyTotalsSection = stringResource(R.string.wear_set_monthly_totals)
    val dailyTotalsSection = stringResource(R.string.wear_set_daily_totals)
    val usageRangeTitle = stringResource(R.string.wear_set_usage_range)
    val rangeLabels = listOf(
        stringResource(R.string.wear_set_range_week),
        stringResource(R.string.wear_set_range_month),
        stringResource(R.string.wear_set_range_quarter),
        stringResource(R.string.wear_set_range_all),
    )

    WearScreen(title = title, onBack = onBack) {
        if (stats.isLoading) {
            textItem(loadingStatistics, small = true, muted = true)
        } else {
            section(overviewSection)
            infoItem(conversationsLabel, formatTokens(stats.totalConversations.toLong()))
            infoItem(messagesLabel, formatTokens(stats.totalMessages.toLong()))
            infoItem(launchesLabel, formatTokens(stats.launchCount.toLong()))
            infoItem(activeDaysLabel, stats.requestsPerDay.count { it.value > 0 }.toString())
        }

        section(tokenUsageSection)
        navItem(
            title = rangeTitle,
            subtitle = rangeLabels[range.ordinal],
            icon = WearIcons.Stats,
        ) { rangePicker = true }

        val currentUsage = usage
        if (currentUsage == null) {
            textItem(loadingUsage, small = true, muted = true)
        } else {
            val totals = currentUsage.totals
            infoItem(inputTokens, formatTokens(totals.promptTokens))
            infoItem(outputTokens, formatTokens(totals.completionTokens))
            infoItem(cachedTokens, formatTokens(totals.cachedTokens))
            infoItem(
                cacheHit,
                if (totals.promptTokens > 0) {
                    "%.1f%%".format(totals.cachedTokens * 100.0 / totals.promptTokens)
                } else {
                    "-"
                },
            )
            infoItem(totalTokens, formatTokens(totals.promptTokens + totals.completionTokens))
            infoItem(bucketsLabel, if (currentUsage.byMonth) monthlyLabel else dailyLabel)

            if (currentUsage.models.isNotEmpty()) {
                section(byModelSection)
                currentUsage.models
                    .sortedByDescending { it.totalTokens }
                    .take(8)
                    .forEachIndexed { index, model ->
                        item(key = "model:$index") {
                            UsageRow(
                                label = model.name ?: stringResource(R.string.wear_set_unknown_model),
                                value = formatTokens(model.totalTokens),
                            )
                        }
                    }
            }

            if (currentUsage.sources.isNotEmpty()) {
                section(bySourceSection)
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
                section(if (currentUsage.byMonth) monthlyTotalsSection else dailyTotalsSection)
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
            title = usageRangeTitle,
            options = rangeLabels,
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

@Composable
private fun sourceLabel(source: String): String {
    val usageSource = UsageSource.entries.find { it.name == source }
    return when (usageSource) {
        UsageSource.CHAT -> stringResource(R.string.wear_set_source_chat)
        UsageSource.COMPRESSION -> stringResource(R.string.wear_set_source_compression)
        UsageSource.TITLE -> stringResource(R.string.wear_set_source_title)
        UsageSource.SUGGESTION -> stringResource(R.string.wear_set_source_suggestion)
        UsageSource.TRANSLATION -> stringResource(R.string.wear_set_source_translation)
        UsageSource.MEMORY -> stringResource(R.string.wear_set_source_memory)
        UsageSource.OCR -> stringResource(R.string.wear_set_source_ocr)
        null -> source
    }
}

private fun formatTokens(count: Long): String = when {
    count >= 1_000_000_000 -> "%.2fB".format(count / 1_000_000_000.0)
    count >= 1_000_000 -> "%.2fM".format(count / 1_000_000.0)
    count >= 1_000 -> "%.1fK".format(count / 1_000.0)
    else -> count.toString()
}
