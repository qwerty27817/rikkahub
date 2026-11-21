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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.ListSubHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import kotlinx.coroutines.launch
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

    if (assistant == null) {
        WearScreen(title = "Memory", onBack = onBack) {
            textItem("This assistant no longer exists.")
        }
        return
    }

    // 与 AssistantDetailVM.memoryIdOf 一致：使用全局记忆时读写同一个记忆库
    val memoryId = if (assistant.useGlobalMemory) {
        MemoryRepository.GLOBAL_MEMORY_ID
    } else {
        assistantId.toString()
    }

    WearScreen(title = "Memory", onBack = onBack) {
        buttonItem(label = "New memory file", icon = WearIcons.Add, tonal = true) {
            onOpenFile(memoryId, null)
        }
        if (assistant.enableMemory) {
            textItem(consolidationText(consolidation), small = true, muted = true)
            buttonItem(
                label = if (consolidation.running) "Organizing…" else "Organize now",
                icon = WearIcons.Retry,
                tonal = true,
                enabled = !consolidation.running,
                onClick = { vm.organizeMemory() },
            )
        }

        section("Settings")
        toggleItem(
            title = "Memory",
            subtitle = "Let the assistant remember things between chats",
            checked = assistant.enableMemory,
            onCheckedChange = { checked ->
                scope.launch { vm.update(assistant.copy(enableMemory = checked)) }
            },
        )
        toggleItem(
            title = "Global memory",
            subtitle = "Share one memory library across assistants",
            checked = assistant.useGlobalMemory,
            enabled = assistant.enableMemory,
            onCheckedChange = { checked ->
                scope.launch { vm.update(assistant.copy(useGlobalMemory = checked)) }
            },
        )
        toggleItem(
            title = "Auto organize",
            subtitle = "Tidy up memory after a conversation ends",
            checked = assistant.autoConsolidateMemory,
            enabled = assistant.enableMemory,
            onCheckedChange = { checked ->
                scope.launch { vm.update(assistant.copy(autoConsolidateMemory = checked)) }
            },
        )
        toggleItem(
            title = "Recent chats reference",
            subtitle = "Let the assistant read recent conversations",
            checked = assistant.enableRecentChatsReference,
            onCheckedChange = { checked ->
                scope.launch { vm.update(assistant.copy(enableRecentChatsReference = checked)) }
            },
        )
        toggleItem(
            title = "Time reminder",
            subtitle = "Inject the current time every so often",
            checked = assistant.enableTimeReminder,
            onCheckedChange = { checked ->
                scope.launch { vm.update(assistant.copy(enableTimeReminder = checked)) }
            },
        )
        if (assistant.enableTimeReminder) {
            navItem(
                title = "Reminder interval",
                subtitle = "${assistant.timeReminderIntervalMinutes} minutes",
                icon = WearIcons.History,
                onClick = { showIntervalDialog = true },
            )
        }

        section("Files")
        infoItem("Library", if (assistant.useGlobalMemory) "Global" else "Per assistant")
        if (memories.isEmpty()) {
            textItem(
                "No memory files yet. The assistant writes them during chats, or create one manually.",
            )
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
            title = "Reminder interval",
            initialValue = assistant.timeReminderIntervalMinutes.toString(),
            label = "Minutes",
            placeholder = "60",
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
private fun memorySectionTitle(directory: String): String = when (directory) {
    "/" -> "You"
    "/topics" -> "Topics"
    "/areas" -> "Areas"
    "/people" -> "People"
    else -> memoryTitleOf(directory.removePrefix("/"))
}

private fun consolidationText(status: MemoryConsolidationStatus): String {
    val lastRun = status.lastRun
    return when {
        status.running -> "Organizing memory…"
        lastRun?.error != null -> "Last organize failed: ${lastRun.error}"
        lastRun != null && lastRun.hasChanges ->
            "Last organize: ${lastRun.changedPaths.size} updated, ${lastRun.deletedPaths.size} removed"

        lastRun != null -> "Last organize: no changes"
        status.pendingTurns > 0 -> "${status.pendingTurns} turns waiting to be organized"
        else -> "No memory organized yet"
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
