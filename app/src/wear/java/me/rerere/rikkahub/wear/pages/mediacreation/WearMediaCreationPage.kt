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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import coil3.compose.AsyncImage
import me.rerere.rikkahub.data.model.MediaCreationAsset
import me.rerere.rikkahub.data.model.MediaCreationNode
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

    LaunchedEffect(vm) {
        vm.events.collect { event ->
            when (event) {
                is MediaCreationEvent.Error -> toaster.show(event.message, ToastType.Error)
                MediaCreationEvent.UploadNotConfigured -> toaster.show(
                    "Remote upload is not configured, so attachments cannot be sent.",
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

    WearScreen(title = session?.title?.ifBlank { null } ?: "Media creation", onBack = onBack) {
        section("Compose")
        item(key = "prompt") {
            WearTextField(
                value = promptText,
                onValueChange = { vm.promptState.setTextAndPlaceCursorAtEnd(it) },
                label = "Prompt",
                placeholder = "Describe what to create",
                singleLine = false,
                minLines = 2,
            )
        }
        infoItem(
            "Model",
            selection?.let { "${it.model.modelId} · ${it.model.kind.name.lowercase()}" } ?: "Not selected",
        )
        infoItem("Session", session?.let { "${it.nodeCount} items" } ?: "Loading…")
        if (providersEmpty) {
            textItem(
                "No media generation providers configured. Add one in Media settings on the phone.",
                small = true,
                muted = true,
            )
        } else if (selection == null) {
            textItem(
                "No model selected for this session. Open it on the phone to choose one.",
                small = true,
                muted = true,
            )
        }
        if (draft.assets.isNotEmpty()) {
            section("Attachments")
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
            label = "Generate",
            icon = WearIcons.Ai,
            enabled = canSubmit,
        ) { vm.generate() }
        buttonItem(
            label = "Clear input",
            icon = WearIcons.Delete,
            tonal = true,
            enabled = promptText.isNotEmpty() || draft.assets.isNotEmpty(),
        ) { vm.clearDraft() }

        section("Timeline")
        if (nodes.itemCount == 0) {
            textItem("No generations yet.", small = true, muted = true)
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
                text = "${asset.role.name.lowercase()} · ${asset.type.name.lowercase()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = WearIcons.Delete,
                contentDescription = "Remove attachment",
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
                    append(record.status.name.lowercase())
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
                    text = "Working…",
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
                    Text(
                        text = "Video · ${output.width ?: "?"}x${output.height ?: "?"}",
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
