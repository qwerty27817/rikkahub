package me.rerere.rikkahub.wear.pages.imggen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import coil3.compose.AsyncImage
import me.rerere.ai.ui.ImageGenSize
import me.rerere.rikkahub.data.datastore.findModelById
import me.rerere.rikkahub.ui.context.LocalToaster
import me.rerere.rikkahub.ui.pages.imggen.ImgGenVM
import me.rerere.rikkahub.wear.WearIcons
import me.rerere.rikkahub.wear.components.WearOptionDialog
import me.rerere.rikkahub.wear.components.WearScreen
import me.rerere.rikkahub.wear.components.WearTextField
import me.rerere.rikkahub.wear.components.buttonItem
import me.rerere.rikkahub.wear.components.infoItem
import me.rerere.rikkahub.wear.components.navItem
import me.rerere.rikkahub.wear.components.section
import me.rerere.rikkahub.wear.components.textItem
import me.rerere.ui.toast.ToastType
import org.koin.androidx.compose.koinViewModel
import java.io.File

@Composable
fun WearImageGenPage(
    onBack: () -> Unit,
) {
    val vm: ImgGenVM = koinViewModel()
    val prompt by vm.prompt.collectAsStateWithLifecycle()
    val numberOfImages by vm.numberOfImages.collectAsStateWithLifecycle()
    val imageSize by vm.size.collectAsStateWithLifecycle()
    val isGenerating by vm.isGenerating.collectAsStateWithLifecycle()
    val currentImages by vm.currentGeneratedImages.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val settings by vm.settingsStore.settingsFlow.collectAsStateWithLifecycle()
    val history = vm.generatedImages.collectAsLazyPagingItems()
    val toaster = LocalToaster.current
    val imageModel = remember(settings) { settings.findModelById(settings.imageGenerationModelId) }
    var countPicker by remember { mutableStateOf(false) }
    var sizePicker by remember { mutableStateOf(false) }

    LaunchedEffect(error) {
        error?.let { message ->
            toaster.show(message, ToastType.Error)
            vm.clearError()
        }
    }

    WearScreen(title = "Image generation", onBack = onBack) {
        item(key = "prompt") {
            WearTextField(
                value = prompt,
                onValueChange = vm::updatePrompt,
                label = "Prompt",
                placeholder = "Describe the image",
                singleLine = false,
                minLines = 2,
            )
        }
        infoItem(
            "Model",
            imageModel?.let { it.displayName.ifBlank { it.modelId } } ?: "Not selected",
        )
        navItem(
            title = "Images per run",
            subtitle = "$numberOfImages",
            icon = WearIcons.Image,
        ) { countPicker = true }
        navItem(
            title = "Size",
            subtitle = if (imageSize == ImageGenSize.AUTO.value) "Auto" else imageSize,
            icon = WearIcons.Image,
        ) { sizePicker = true }
        if (imageModel == null) {
            textItem("No image model selected. Configure one in settings first.", small = true, muted = true)
        }
        buttonItem(
            label = if (isGenerating) "Cancel generation" else "Generate",
            icon = if (isGenerating) WearIcons.Stop else WearIcons.Ai,
            tonal = isGenerating,
            enabled = if (isGenerating) true else prompt.isNotBlank() && imageModel != null,
        ) {
            if (isGenerating) vm.cancelGeneration() else vm.generateImage()
        }

        if (currentImages.isNotEmpty()) {
            section("Current result")
            currentImages.forEachIndexed { index, image ->
                item(key = "current:$index") {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        AsyncImage(
                            model = File(image.filePath),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                        )
                        Text(
                            text = image.prompt,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        section("Recent images")
        if (history.itemCount == 0) {
            textItem("No saved images yet.", small = true, muted = true)
        } else {
            items(count = minOf(history.itemCount, 12)) { index ->
                val image = history[index]
                if (image != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        AsyncImage(
                            model = File(image.filePath),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                        )
                        Text(
                            text = image.prompt,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }

    if (countPicker) {
        WearOptionDialog(
            visible = true,
            title = "Images per run",
            options = (1..4).map { count -> if (count == 1) "1 image" else "$count images" },
            selectedIndex = (numberOfImages - 1).coerceIn(0, 3),
            onSelect = { index ->
                countPicker = false
                vm.updateNumberOfImages(index + 1)
            },
            onDismiss = { countPicker = false },
        )
    }

    if (sizePicker) {
        WearOptionDialog(
            visible = true,
            title = "Image size",
            options = ImageGenSize.entries.map { if (it == ImageGenSize.AUTO) "Auto" else it.value },
            selectedIndex = ImageGenSize.entries.indexOfFirst { it.value == imageSize },
            onSelect = { index ->
                sizePicker = false
                ImageGenSize.entries.getOrNull(index)?.let { vm.updateSize(it.value) }
            },
            onDismiss = { sizePicker = false },
        )
    }
}
