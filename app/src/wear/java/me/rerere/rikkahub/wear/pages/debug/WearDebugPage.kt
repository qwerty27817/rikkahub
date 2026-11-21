package me.rerere.rikkahub.wear.pages.debug

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.rerere.rikkahub.BuildConfig
import me.rerere.rikkahub.data.datastore.findModelById
import me.rerere.rikkahub.data.db.DatabaseMigrationTracker
import me.rerere.rikkahub.data.db.MigrationState
import me.rerere.rikkahub.data.db.dao.ConversationDAO
import me.rerere.rikkahub.data.db.dao.MessageNodeDAO
import me.rerere.rikkahub.data.repository.ConversationRepository
import me.rerere.rikkahub.ui.context.LocalSettings
import me.rerere.rikkahub.ui.context.LocalToaster
import me.rerere.rikkahub.ui.pages.debug.DebugVM
import me.rerere.rikkahub.wear.WearIcons
import me.rerere.rikkahub.wear.components.WearInputDialog
import me.rerere.rikkahub.wear.components.WearScreen
import me.rerere.rikkahub.wear.components.buttonItem
import me.rerere.rikkahub.wear.components.infoItem
import me.rerere.rikkahub.wear.components.navItem
import me.rerere.rikkahub.wear.components.section
import me.rerere.rikkahub.wear.components.textItem
import me.rerere.rikkahub.wear.shape.LocalScreenShapeState
import me.rerere.ui.toast.ToastType
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import kotlin.uuid.Uuid

@Composable
fun WearDebugPage(
    onBack: () -> Unit,
) {
    val vm: DebugVM = koinViewModel()
    val settings = LocalSettings.current
    val toaster = LocalToaster.current
    val scope = rememberCoroutineScope()
    val conversationDao: ConversationDAO = koinInject()
    val messageNodeDao: MessageNodeDAO = koinInject()
    val conversationRepository: ConversationRepository = koinInject()

    val migrationState by DatabaseMigrationTracker.state.collectAsStateWithLifecycle()
    val conversationCount by vm.conversationCount.collectAsStateWithLifecycle()
    val conversationAssistants by vm.conversationAssistants.collectAsStateWithLifecycle()
    val launchCount by vm.launchCount.collectAsStateWithLifecycle()
    val shapeState = LocalScreenShapeState.current

    var conversationCountInDb by remember { mutableStateOf<Int?>(null) }
    var messageCountInDb by remember { mutableStateOf<Int?>(null) }
    var refreshTick by remember { mutableIntStateOf(0) }
    var showLaunchCount by remember { mutableStateOf(false) }
    var recovering by remember { mutableStateOf(false) }
    var rebuilding by remember { mutableStateOf(false) }
    var indexProgress by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(refreshTick) {
        conversationCountInDb = runCatching { conversationDao.countAll() }.getOrNull()
        messageCountInDb = runCatching { messageNodeDao.countAll() }.getOrNull()
    }

    val existingAssistantIds = remember(settings.assistants) {
        settings.assistants.map { it.id }.toSet()
    }
    val missingAssistants = remember(conversationAssistants, existingAssistantIds) {
        conversationAssistants
            ?.filterKeys { it !in existingAssistantIds }
            ?.entries
            ?.sortedByDescending { it.value }
            .orEmpty()
    }
    val missingModels = remember(settings) {
        if (settings.init) {
            emptyList()
        } else {
            buildList {
                fun check(label: String, id: Uuid?) {
                    if (id != null && settings.findModelById(id) == null) add(label)
                }
                check("chat", settings.chatModelId)
                check("fast", settings.fastModelId)
                check("translation", settings.translateModeId)
                check("image generation", settings.imageGenerationModelId)
                check("OCR", settings.ocrModelId)
                check("memory", settings.memoryModelId)
            }
        }
    }

    WearScreen(title = "Debug", onBack = onBack) {
        section("Database")
        infoItem("Conversations", conversationCountInDb?.toString() ?: "…")
        infoItem("Messages", messageCountInDb?.toString() ?: "…")
        infoItem("Migration", migrationStateLabel(migrationState))

        section("Settings")
        infoItem("Loaded", if (settings.init) "Loading…" else "Yes")
        infoItem("Providers", settings.providers.size.toString())
        infoItem("Models", settings.providers.sumOf { it.models.size }.toString())
        infoItem("Assistants", settings.assistants.size.toString())
        infoItem(
            "Missing models",
            if (missingModels.isEmpty()) "None" else missingModels.joinToString(", "),
        )

        section("Conversations")
        infoItem("Indexed", conversationCount?.toString() ?: "…")
        infoItem("Assistants in chats", conversationAssistants?.size?.toString() ?: "…")
        infoItem("Missing assistants", missingAssistants.size.toString())
        missingAssistants.take(5).forEach { (id, count) ->
            textItem("$id · $count chats", small = true, muted = true)
        }

        section("Device")
        infoItem("Version", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        infoItem("ABIs", Build.SUPPORTED_ABIS.joinToString(", "))
        infoItem("Screen shape", shapeState.shape.name.lowercase())
        infoItem("Shape diagnosis", shapeState.diagnosis)

        section("Actions")
        navItem(
            title = "Launch count",
            subtitle = launchCount.toString(),
            icon = WearIcons.Stats,
        ) { showLaunchCount = true }
        buttonItem(label = "Refresh diagnostics", icon = WearIcons.Retry, tonal = true) {
            vm.refreshConversationCount()
            vm.scanConversationAssistants()
            refreshTick++
        }
        buttonItem(
            label = if (recovering) "Recovering…" else "Recover assistants",
            icon = WearIcons.Retry,
            tonal = true,
            enabled = !recovering && !settings.init && missingAssistants.isNotEmpty(),
        ) {
            recovering = true
            scope.launch {
                try {
                    when (val recovered = vm.recoverAssistantsFromConversations()) {
                        null -> toaster.show("Settings are still loading", ToastType.Error)
                        0 -> toaster.show("No missing assistants to recover")
                        else -> toaster.show("Recovered $recovered assistants", ToastType.Success)
                    }
                    vm.scanConversationAssistants()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    toaster.show("Recovery failed: ${e.message}", ToastType.Error)
                } finally {
                    recovering = false
                }
            }
        }
        buttonItem(
            label = if (rebuilding) "Rebuilding…" else "Rebuild search index",
            icon = WearIcons.Database,
            tonal = true,
            enabled = !rebuilding,
        ) {
            rebuilding = true
            indexProgress = null
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        conversationRepository.rebuildAllIndexes { current, total ->
                            indexProgress = "$current/$total"
                        }
                    }
                    toaster.show("Search index rebuilt", ToastType.Success)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    toaster.show("Rebuild failed: ${e.message}", ToastType.Error)
                } finally {
                    rebuilding = false
                    indexProgress = null
                }
            }
        }
        indexProgress?.let { progress ->
            textItem("Index progress: $progress", small = true, muted = true)
        }
    }

    if (showLaunchCount) {
        WearInputDialog(
            visible = true,
            title = "Launch count",
            initialValue = launchCount.toString(),
            keyboardType = KeyboardType.Number,
            onConfirm = { value ->
                showLaunchCount = false
                value.trim().toIntOrNull()?.let(vm::setLaunchCount)
            },
            onDismiss = { showLaunchCount = false },
        )
    }
}

private fun migrationStateLabel(state: MigrationState): String = when (state) {
    MigrationState.Idle -> "Idle"
    is MigrationState.Migrating -> "Migrating ${state.from} -> ${state.to}"
}
