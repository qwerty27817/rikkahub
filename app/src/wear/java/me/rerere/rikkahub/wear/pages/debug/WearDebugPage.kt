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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.rerere.rikkahub.BuildConfig
import me.rerere.rikkahub.R
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

    // WearScreen 的 content 是普通 DSL builder（非 @Composable），字符串要先在外部取好；
    // 非 Composable 回调里的动态 Toast 只预取带占位符的模板。
    val title = stringResource(R.string.wear_set_debug)
    val databaseSection = stringResource(R.string.wear_set_section_database)
    val conversationsLabel = stringResource(R.string.wear_set_conversations)
    val messagesLabel = stringResource(R.string.wear_set_messages)
    val migrationTitle = stringResource(R.string.wear_set_migration)
    val migrationLabel = when (val state = migrationState) {
        MigrationState.Idle -> stringResource(R.string.wear_set_idle)
        is MigrationState.Migrating -> stringResource(R.string.wear_set_migrating, state.from, state.to)
    }
    val settingsSection = stringResource(R.string.wear_set_settings_title)
    val loadedLabel = stringResource(R.string.wear_set_loaded)
    val loadingEllipsis = stringResource(R.string.wear_set_loading_ellipsis)
    val yesLabel = stringResource(R.string.wear_set_yes)
    val providersLabel = stringResource(R.string.wear_set_providers)
    val modelsLabel = stringResource(R.string.wear_set_models)
    val assistantsLabel = stringResource(R.string.wear_set_assistants)
    val missingModelsLabel = stringResource(R.string.wear_set_missing_models)
    val noneLabel = stringResource(R.string.wear_set_none)
    val indexedLabel = stringResource(R.string.wear_set_indexed)
    val assistantsInChatsLabel = stringResource(R.string.wear_set_assistants_in_chats)
    val missingAssistantsLabel = stringResource(R.string.wear_set_missing_assistants)
    val chatsCountFormat = stringResource(R.string.wear_set_chats_count)
    val deviceSection = stringResource(R.string.wear_set_device)
    val versionLabel = stringResource(R.string.wear_set_version)
    val abisLabel = stringResource(R.string.wear_set_abis)
    val screenShapeLabel = stringResource(R.string.wear_common_screen_shape)
    val shapeDiagnosisLabel = stringResource(R.string.wear_set_shape_diagnosis)
    val actionsSection = stringResource(R.string.wear_set_section_actions)
    val launchCountTitle = stringResource(R.string.wear_set_launch_count)
    val refreshDiagnosticsLabel = stringResource(R.string.wear_set_refresh_diagnostics)
    val recoveringLabel = stringResource(R.string.wear_set_recovering)
    val recoverAssistantsLabel = stringResource(R.string.wear_set_recover_assistants)
    val settingsLoadingMessage = stringResource(R.string.wear_set_settings_loading)
    val noMissingAssistantsMessage = stringResource(R.string.wear_set_no_missing_assistants)
    val recoveredAssistantsFormat = stringResource(R.string.wear_set_recovered_assistants)
    val recoveryFailedFormat = stringResource(R.string.wear_set_recovery_failed)
    val rebuildingLabel = stringResource(R.string.wear_set_rebuilding)
    val rebuildIndexLabel = stringResource(R.string.wear_set_rebuild_index)
    val searchIndexRebuiltMessage = stringResource(R.string.wear_set_search_index_rebuilt)
    val rebuildFailedFormat = stringResource(R.string.wear_set_rebuild_failed)
    val indexProgressFormat = stringResource(R.string.wear_set_index_progress)

    val roleChat = stringResource(R.string.wear_set_role_chat)
    val roleFast = stringResource(R.string.wear_set_role_fast)
    val roleTranslation = stringResource(R.string.wear_set_role_translation)
    val roleImageGeneration = stringResource(R.string.wear_set_role_image_generation)
    val roleOcr = stringResource(R.string.wear_set_role_ocr)
    val roleMemory = stringResource(R.string.wear_set_role_memory)

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
                check(roleChat, settings.chatModelId)
                check(roleFast, settings.fastModelId)
                check(roleTranslation, settings.translateModeId)
                check(roleImageGeneration, settings.imageGenerationModelId)
                check(roleOcr, settings.ocrModelId)
                check(roleMemory, settings.memoryModelId)
            }
        }
    }

    WearScreen(title = title, onBack = onBack) {
        section(databaseSection)
        infoItem(conversationsLabel, conversationCountInDb?.toString() ?: "…")
        infoItem(messagesLabel, messageCountInDb?.toString() ?: "…")
        infoItem(migrationTitle, migrationLabel)

        section(settingsSection)
        infoItem(loadedLabel, if (settings.init) loadingEllipsis else yesLabel)
        infoItem(providersLabel, settings.providers.size.toString())
        infoItem(modelsLabel, settings.providers.sumOf { it.models.size }.toString())
        infoItem(assistantsLabel, settings.assistants.size.toString())
        infoItem(
            missingModelsLabel,
            if (missingModels.isEmpty()) noneLabel else missingModels.joinToString(", "),
        )

        section(conversationsLabel)
        infoItem(indexedLabel, conversationCount?.toString() ?: "…")
        infoItem(assistantsInChatsLabel, conversationAssistants?.size?.toString() ?: "…")
        infoItem(missingAssistantsLabel, missingAssistants.size.toString())
        missingAssistants.take(5).forEach { (id, count) ->
            textItem(chatsCountFormat.format(id, count), small = true, muted = true)
        }

        section(deviceSection)
        infoItem(versionLabel, "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        infoItem(abisLabel, Build.SUPPORTED_ABIS.joinToString(", "))
        infoItem(screenShapeLabel, shapeState.shape.name.lowercase())
        infoItem(shapeDiagnosisLabel, shapeState.diagnosis)

        section(actionsSection)
        navItem(
            title = launchCountTitle,
            subtitle = launchCount.toString(),
            icon = WearIcons.Stats,
        ) { showLaunchCount = true }
        buttonItem(label = refreshDiagnosticsLabel, icon = WearIcons.Retry, tonal = true) {
            vm.refreshConversationCount()
            vm.scanConversationAssistants()
            refreshTick++
        }
        buttonItem(
            label = if (recovering) recoveringLabel else recoverAssistantsLabel,
            icon = WearIcons.Retry,
            tonal = true,
            enabled = !recovering && !settings.init && missingAssistants.isNotEmpty(),
        ) {
            recovering = true
            scope.launch {
                try {
                    when (val recovered = vm.recoverAssistantsFromConversations()) {
                        null -> toaster.show(settingsLoadingMessage, ToastType.Error)
                        0 -> toaster.show(noMissingAssistantsMessage)
                        else -> toaster.show(recoveredAssistantsFormat.format(recovered), ToastType.Success)
                    }
                    vm.scanConversationAssistants()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    toaster.show(recoveryFailedFormat.format(e.message), ToastType.Error)
                } finally {
                    recovering = false
                }
            }
        }
        buttonItem(
            label = if (rebuilding) rebuildingLabel else rebuildIndexLabel,
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
                    toaster.show(searchIndexRebuiltMessage, ToastType.Success)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    toaster.show(rebuildFailedFormat.format(e.message), ToastType.Error)
                } finally {
                    rebuilding = false
                    indexProgress = null
                }
            }
        }
        indexProgress?.let { progress ->
            textItem(indexProgressFormat.format(progress), small = true, muted = true)
        }
    }

    if (showLaunchCount) {
        WearInputDialog(
            visible = true,
            title = launchCountTitle,
            initialValue = launchCount.toString(),
            keyboardType = KeyboardType.Number,
            confirmLabel = stringResource(R.string.wear_common_ok),
            onConfirm = { value ->
                showLaunchCount = false
                value.trim().toIntOrNull()?.let(vm::setLaunchCount)
            },
            onDismiss = { showLaunchCount = false },
        )
    }
}
