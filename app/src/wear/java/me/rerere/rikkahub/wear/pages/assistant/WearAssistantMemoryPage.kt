package me.rerere.rikkahub.wear.pages.assistant

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.ListSubHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import kotlinx.coroutines.launch
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.ai.MemoryConsolidationStatus
import me.rerere.rikkahub.data.model.MemoryFile
import me.rerere.rikkahub.data.model.groupByDirectory
import me.rerere.rikkahub.data.model.memoryTitleOf
import me.rerere.rikkahub.data.repository.MemoryRepository
import me.rerere.rikkahub.ui.context.LocalSettings
import me.rerere.rikkahub.ui.pages.assistant.detail.AssistantDetailVM
import me.rerere.rikkahub.wear.WearIcons
import me.rerere.rikkahub.wear.components.WearInputDialog
import me.rerere.rikkahub.wear.components.WearListScope
import me.rerere.rikkahub.wear.components.WearScreen
import me.rerere.rikkahub.wear.components.buttonItem
import me.rerere.rikkahub.wear.components.infoItem
import me.rerere.rikkahub.wear.components.navItem
import me.rerere.rikkahub.wear.components.section
import me.rerere.rikkahub.wear.components.textItem
import me.rerere.rikkahub.wear.components.toggleItem
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * 助手记忆库：记忆开关 + 文件列表（新建、打开、删除在文件页里）。
 */
@Composable
fun WearAssistantMemoryPage(
    assistantId: kotlin.uuid.Uuid,
    onBack: () -> Unit,
    onOpenFile: (memoryId: String, path: String?) -> Unit,
) {
    val settings = LocalSettings.current
    val scope = rememberCoroutineScope()
    val vm: AssistantDetailVM = koinViewModel(parameters = { parametersOf(assistantId.toString()) })
    val assistant = settings.assistants.find { it.id == assistantId }
    val memories by vm.memories.collectAsStateWithLifecycle()
    val consolidation by vm.memoryConsolidation.collectAsStateWithLifecycle()

    var showIntervalDialog by remember { mutableStateOf(false) }

    // WearScreen 的 content 是普通 DSL builder（非 @Composable），字符串要先在外部取好。
    val memoryTitle = stringResource(R.string.wear_asst_memory)
    val missingAssistant = stringResource(R.string.wear_asst_assistant_missing)

    if (assistant == null) {
        WearScreen(title = memoryTitle, onBack = onBack) {
            textItem(missingAssistant)
        }
        return
    }

    // 与 AssistantDetailVM.memoryIdOf 一致：使用全局记忆时读写同一个记忆库
    val memoryId = if (assistant.useGlobalMemory) {
        MemoryRepository.GLOBAL_MEMORY_ID
    } else {
        assistantId.toString()
    }

    val newMemoryFileLabel = stringResource(R.string.wear_asst_new_memory_file)
    val organizingLabel = stringResource(R.string.wear_asst_organizing)
    val organizeNowLabel = stringResource(R.string.wear_asst_organize_now)
    val settingsLabel = stringResource(R.string.wear_asst_settings)
    val memoryDesc = stringResource(R.string.wear_asst_memory_desc)
    val globalMemoryLabel = stringResource(R.string.wear_asst_global_memory)
    val globalMemoryDesc = stringResource(R.string.wear_asst_global_memory_desc)
    val autoOrganizeLabel = stringResource(R.string.wear_asst_auto_organize)
    val autoOrganizeDesc = stringResource(R.string.wear_asst_auto_organize_desc)
    val recentChatsLabel = stringResource(R.string.wear_asst_recent_chats_reference)
    val recentChatsDesc = stringResource(R.string.wear_asst_recent_chats_reference_desc)
    val timeReminderLabel = stringResource(R.string.wear_asst_time_reminder)
    val timeReminderDesc = stringResource(R.string.wear_asst_time_reminder_desc)
    val reminderIntervalLabel = stringResource(R.string.wear_asst_reminder_interval)
    val minutesLabel = stringResource(R.string.wear_asst_minutes)
    val minutesPlaceholder = stringResource(R.string.wear_asst_minutes_placeholder)
    val filesLabel = stringResource(R.string.wear_asst_files)
    val libraryLabel = stringResource(R.string.wear_asst_library)
    val globalLabel = stringResource(R.string.wear_asst_global)
    val perAssistantLabel = stringResource(R.string.wear_asst_per_assistant)
    val memoryEmptyLabel = stringResource(R.string.wear_asst_memory_empty)
    val consolidationLabel = consolidationText(consolidation)
    val intervalLabel = stringResource(
        R.string.wear_asst_minutes_value,
        assistant.timeReminderIntervalMinutes,
    )

    WearScreen(title = memoryTitle, onBack = onBack) {
        buttonItem(label = newMemoryFileLabel, icon = WearIcons.Add, tonal = true) {
            onOpenFile(memoryId, null)
        }
        if (assistant.enableMemory) {
            textItem(consolidationLabel, small = true, muted = true)
            buttonItem(
                label = if (consolidation.running) organizingLabel else organizeNowLabel,
                icon = WearIcons.Retry,
                tonal = true,
                enabled = !consolidation.running,
                onClick = { vm.organizeMemory() },
            )
        }

        section(settingsLabel)
        toggleItem(
            title = memoryTitle,
            subtitle = memoryDesc,
            checked = assistant.enableMemory,
            onCheckedChange = { checked ->
                scope.launch { vm.update(assistant.copy(enableMemory = checked)) }
            },
        )
        toggleItem(
            title = globalMemoryLabel,
            subtitle = globalMemoryDesc,
            checked = assistant.useGlobalMemory,
            enabled = assistant.enableMemory,
            onCheckedChange = { checked ->
                scope.launch { vm.update(assistant.copy(useGlobalMemory = checked)) }
            },
        )
        toggleItem(
            title = autoOrganizeLabel,
            subtitle = autoOrganizeDesc,
            checked = assistant.autoConsolidateMemory,
            enabled = assistant.enableMemory,
            onCheckedChange = { checked ->
                scope.launch { vm.update(assistant.copy(autoConsolidateMemory = checked)) }
            },
        )
        toggleItem(
            title = recentChatsLabel,
            subtitle = recentChatsDesc,
            checked = assistant.enableRecentChatsReference,
            onCheckedChange = { checked ->
                scope.launch { vm.update(assistant.copy(enableRecentChatsReference = checked)) }
            },
        )
        toggleItem(
            title = timeReminderLabel,
            subtitle = timeReminderDesc,
            checked = assistant.enableTimeReminder,
            onCheckedChange = { checked ->
                scope.launch { vm.update(assistant.copy(enableTimeReminder = checked)) }
            },
        )
        if (assistant.enableTimeReminder) {
            navItem(
                title = reminderIntervalLabel,
                subtitle = intervalLabel,
                icon = WearIcons.History,
                onClick = { showIntervalDialog = true },
            )
        }

        section(filesLabel)
        infoItem(libraryLabel, if (assistant.useGlobalMemory) globalLabel else perAssistantLabel)
        if (memories.isEmpty()) {
            textItem(memoryEmptyLabel)
        } else {
            memories.groupByDirectory().forEach { (directory, files) ->
                memoryDirectoryHeader(directory)
                files.forEach { memory ->
                    item(key = "memory:${memory.path}") {
                        MemoryFileRow(
                            memory = memory,
                            onClick = { onOpenFile(memoryId, memory.path) },
                        )
                    }
                }
            }
        }
    }

    if (showIntervalDialog) {
        WearInputDialog(
            visible = true,
            title = reminderIntervalLabel,
            initialValue = assistant.timeReminderIntervalMinutes.toString(),
            label = minutesLabel,
            placeholder = minutesPlaceholder,
            confirmLabel = stringResource(R.string.wear_common_ok),
            singleLine = true,
            keyboardType = KeyboardType.Number,
            onConfirm = { value ->
                showIntervalDialog = false
                val minutes = value.toIntOrNull()
                if (minutes != null && minutes > 0) {
                    scope.launch { vm.update(assistant.copy(timeReminderIntervalMinutes = minutes)) }
                }
            },
            onDismiss = { showIntervalDialog = false },
        )
    }
}

/** 目录标题用目录名做 key，避免两个目录显示成同名时触发 LazyColumn 的重复 key。 */
private fun WearListScope.memoryDirectoryHeader(directory: String) {
    item(key = "memory-dir:$directory") {
        ListSubHeader(label = { Text(text = memorySectionTitle(directory)) })
    }
}

/** 目录标题沿用手机记忆页的约定：根目录是用户本人，格式约定的目录有固定译名。 */
@Composable
private fun memorySectionTitle(directory: String): String = when (directory) {
    "/" -> stringResource(R.string.wear_asst_memory_you)
    "/topics" -> stringResource(R.string.wear_asst_memory_topics)
    "/areas" -> stringResource(R.string.wear_asst_memory_areas)
    "/people" -> stringResource(R.string.wear_asst_memory_people)
    else -> memoryTitleOf(directory.removePrefix("/"))
}

@Composable
private fun consolidationText(status: MemoryConsolidationStatus): String {
    val lastRun = status.lastRun
    val error = lastRun?.error
    return when {
        status.running -> stringResource(R.string.wear_asst_organizing_memory)
        error != null -> stringResource(R.string.wear_asst_last_organize_failed, error)
        lastRun != null && lastRun.hasChanges ->
            stringResource(
                R.string.wear_asst_last_organize_changes,
                lastRun.changedPaths.size,
                lastRun.deletedPaths.size,
            )

        lastRun != null -> stringResource(R.string.wear_asst_last_organize_no_changes)
        status.pendingTurns > 0 ->
            stringResource(R.string.wear_asst_pending_turns, status.pendingTurns)

        else -> stringResource(R.string.wear_asst_no_memory_organized)
    }
}

private fun relativeTimeOf(time: Long): String = DateUtils.getRelativeTimeSpanString(
    time,
    System.currentTimeMillis(),
    DateUtils.MINUTE_IN_MILLIS,
    DateUtils.FORMAT_ABBREV_MONTH,
).toString()

@Composable
private fun MemoryFileRow(
    memory: MemoryFile,
    onClick: () -> Unit,
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(
                imageVector = WearIcons.File,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = memory.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (memory.description.isNotBlank()) {
                    Text(
                        text = memory.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = "${memory.path} · ${relativeTimeOf(memory.updatedAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
