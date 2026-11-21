package me.rerere.rikkahub.wear.pages.mediacreation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import coil3.compose.AsyncImage
import me.rerere.mediagen.model.ImageRole
import me.rerere.mediagen.model.MediaKind
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.model.MediaCreationAsset
import me.rerere.rikkahub.data.model.MediaCreationAssetType
import me.rerere.rikkahub.data.model.MediaCreationNode
import me.rerere.rikkahub.data.model.MediaCreationStatus
import me.rerere.rikkahub.data.model.canSubmit
import me.rerere.rikkahub.ui.context.LocalSettings
import me.rerere.rikkahub.ui.context.LocalToaster
import me.rerere.rikkahub.ui.pages.mediacreation.MediaCreationEvent
import me.rerere.rikkahub.ui.pages.mediacreation.MediaCreationVM
import me.rerere.rikkahub.wear.WearIcons
import me.rerere.rikkahub.wear.components.WearScreen
import me.rerere.rikkahub.wear.components.WearTextField
import me.rerere.rikkahub.wear.components.buttonItem
import me.rerere.rikkahub.wear.components.infoItem
import me.rerere.rikkahub.wear.components.section
import me.rerere.rikkahub.wear.components.textItem
import me.rerere.ui.toast.ToastType
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.io.File

@Composable
fun WearMediaCreationPage(
    sessionId: String,
    onBack: () -> Unit,
) {
    val vm: MediaCreationVM = koinViewModel(parameters = { parametersOf(sessionId) })
    val session by vm.session.collectAsStateWithLifecycle()
    val draft by vm.draft.collectAsStateWithLifecycle()
    val selection by vm.selection.collectAsStateWithLifecycle()
    val nodes = vm.nodes.collectAsLazyPagingItems()
    val settings = LocalSettings.current
    val toaster = LocalToaster.current
    val promptText = vm.promptState.text.toString()

    // WearScreen 的 content 是普通 DSL builder（非 @Composable），先算好所有字符串。
    val mediaCreationTitle = stringResource(R.string.wear_misc_media_creation_title)
    val uploadNotConfigured = stringResource(R.string.wear_misc_media_upload_not_configured)
    val composeSection = stringResource(R.string.wear_misc_media_section_compose)
    val promptLabel = stringResource(R.string.wear_misc_prompt)
    val promptPlaceholder = stringResource(R.string.wear_misc_media_prompt_placeholder)
    val modelLabel = stringResource(R.string.wear_misc_model)
    val notSelected = stringResource(R.string.wear_misc_not_selected)
    val sessionLabel = stringResource(R.string.wear_misc_media_session)
    val loadingLabel = stringResource(R.string.wear_misc_loading)
    val noProvidersHint = stringResource(R.string.wear_misc_media_no_providers)
    val noModelHint = stringResource(R.string.wear_misc_media_no_model)
    val attachmentsSection = stringResource(R.string.wear_misc_media_section_attachments)
    val generate = stringResource(R.string.wear_misc_generate)
    val clearInput = stringResource(R.string.wear_misc_clear_input)
    val timelineSection = stringResource(R.string.wear_misc_media_section_timeline)
    val emptyTimeline = stringResource(R.string.wear_misc_media_empty_timeline)
    val modelValue = selection?.let {
        "${it.model.modelId} · ${mediaKindLabel(it.model.kind)}"
    } ?: notSelected
    val sessionValue = session?.let {
        stringResource(R.string.wear_misc_media_record_count, it.nodeCount)
    } ?: loadingLabel

    LaunchedEffect(vm) {
        vm.events.collect { event ->
            when (event) {
                is MediaCreationEvent.Error -> toaster.show(event.message, ToastType.Error)
                MediaCreationEvent.UploadNotConfigured -> toaster.show(
                    uploadNotConfigured,
                    ToastType.Error,
                )

                MediaCreationEvent.SessionGone -> onBack()
            }
        }
    }

    val capabilities = selection?.capabilities
    val canSubmit = remember(draft, promptText, capabilities) {
        capabilities != null && draft.copy(prompt = promptText).canSubmit(capabilities)
    }
    val providersEmpty = settings.mediaGenerationProviders.isEmpty()

    WearScreen(title = session?.title?.ifBlank { null } ?: mediaCreationTitle, onBack = onBack) {
        section(composeSection)
        item(key = "prompt") {
            WearTextField(
                value = promptText,
                onValueChange = { vm.promptState.setTextAndPlaceCursorAtEnd(it) },
                label = promptLabel,
                placeholder = promptPlaceholder,
                singleLine = false,
                minLines = 2,
            )
        }
        infoItem(modelLabel, modelValue)
        infoItem(sessionLabel, sessionValue)
        if (providersEmpty) {
            textItem(
                noProvidersHint,
                small = true,
                muted = true,
            )
        } else if (selection == null) {
            textItem(
                noModelHint,
                small = true,
                muted = true,
            )
        }
        if (draft.assets.isNotEmpty()) {
            section(attachmentsSection)
            draft.assets.forEachIndexed { index, asset ->
                item(key = "asset:$index:${asset.path}") {
                    AssetRow(
                        asset = asset,
                        onRemove = { vm.removeAsset(asset) },
                    )
                }
            }
        }
        buttonItem(
            label = generate,
            icon = WearIcons.Ai,
            enabled = canSubmit,
        ) { vm.generate() }
        buttonItem(
            label = clearInput,
            icon = WearIcons.Delete,
            tonal = true,
            enabled = promptText.isNotEmpty() || draft.assets.isNotEmpty(),
        ) { vm.clearDraft() }

        section(timelineSection)
        if (nodes.itemCount == 0) {
            textItem(emptyTimeline, small = true, muted = true)
        } else {
            items(count = minOf(nodes.itemCount, 30)) { index ->
                val node = nodes[index]
                if (node != null) {
                    NodeCard(node = node, resolve = vm::resolve)
                }
            }
        }
    }
}

@Composable
private fun AssetRow(
    asset: MediaCreationAsset,
    onRemove: () -> Unit,
) {
    val roleLabel = mediaRoleLabel(asset.role)
    val typeLabel = mediaTypeLabel(asset.type)
    val removeAttachment = stringResource(R.string.wear_misc_media_remove_attachment)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = asset.path.substringAfterLast('/'),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "$roleLabel · $typeLabel",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = WearIcons.Delete,
                contentDescription = removeAttachment,
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun NodeCard(
    node: MediaCreationNode,
    resolve: (String) -> File,
) {
    val record = node.record
    val statusLabel = mediaStatusLabel(record.status)
    val working = stringResource(R.string.wear_misc_media_working)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (record.prompt.isNotBlank()) {
                Text(
                    text = record.prompt,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = buildString {
                    append(statusLabel)
                    if (node.versionCount > 1) append(" · v${node.versionIndex + 1}/${node.versionCount}")
                    if (record.modelId.isNotBlank()) append(" · ${record.modelId}")
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (record.status.isActive) {
                Text(
                    text = working,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            record.error?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            record.outputs.take(2).forEach { output ->
                if (output.isVideo) {
                    val videoInfo = stringResource(
                        R.string.wear_misc_media_video_info,
                        output.width?.toString() ?: "?",
                        output.height?.toString() ?: "?",
                    )
                    Text(
                        text = videoInfo,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                } else {
                    AsyncImage(
                        model = resolve(output.path),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun mediaStatusLabel(status: MediaCreationStatus): String = stringResource(
    when (status) {
        MediaCreationStatus.PREPARING -> R.string.wear_misc_status_preparing
        MediaCreationStatus.QUEUED -> R.string.wear_misc_status_queued
        MediaCreationStatus.RUNNING -> R.string.wear_misc_status_running
        MediaCreationStatus.DOWNLOADING -> R.string.wear_misc_status_downloading
        MediaCreationStatus.SUCCEEDED -> R.string.wear_misc_status_succeeded
        MediaCreationStatus.FAILED -> R.string.wear_misc_status_failed
        MediaCreationStatus.CANCELLED -> R.string.wear_misc_status_cancelled
    },
)

@Composable
private fun mediaRoleLabel(role: ImageRole): String = stringResource(
    when (role) {
        ImageRole.FIRST_FRAME -> R.string.wear_misc_role_first_frame
        ImageRole.LAST_FRAME -> R.string.wear_misc_role_last_frame
        ImageRole.REFERENCE -> R.string.wear_misc_role_reference
    },
)

@Composable
private fun mediaTypeLabel(type: MediaCreationAssetType): String = stringResource(
    if (type == MediaCreationAssetType.VIDEO) R.string.wear_misc_kind_video else R.string.wear_misc_kind_image,
)

@Composable
private fun mediaKindLabel(kind: MediaKind): String = stringResource(
    if (kind == MediaKind.VIDEO) R.string.wear_misc_kind_video else R.string.wear_misc_kind_image,
)
