package me.rerere.rikkahub.wear.pages.translator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.rikkahub.data.datastore.findModelById
import me.rerere.rikkahub.ui.components.ai.TranslationLanguages
import me.rerere.rikkahub.ui.components.ai.translationLanguageName
import me.rerere.rikkahub.ui.context.LocalToaster
import me.rerere.rikkahub.ui.pages.translator.TranslatorVM
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

@Composable
fun WearTranslatorPage(
    onBack: () -> Unit,
) {
    val vm: TranslatorVM = koinViewModel()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val inputText by vm.inputText.collectAsStateWithLifecycle()
    val translatedText by vm.translatedText.collectAsStateWithLifecycle()
    val targetLanguage by vm.targetLanguage.collectAsStateWithLifecycle()
    val translating by vm.translating.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val toaster = LocalToaster.current
    var languagePicker by remember { mutableStateOf(false) }

    LaunchedEffect(vm) {
        vm.errorFlow.collect { error ->
            toaster.show(error.message ?: "Translation failed", ToastType.Error)
        }
    }

    val modelMissing = remember(settings) {
        !settings.init && settings.providers.findModelById(settings.translateModeId) == null
    }
    val languageNames = TranslationLanguages.map { translationLanguageName(it) }
    val targetLanguageName = translationLanguageName(targetLanguage)

    WearScreen(title = "Translator", onBack = onBack) {
        item(key = "input") {
            WearTextField(
                value = inputText,
                onValueChange = vm::updateInputText,
                label = "Text",
                placeholder = "Enter text to translate",
                singleLine = false,
                minLines = 3,
            )
        }
        infoItem("Source", "Auto-detect")
        navItem(
            title = "Target",
            subtitle = targetLanguageName,
            icon = WearIcons.Translate,
        ) { languagePicker = true }
        if (modelMissing) {
            textItem(
                "No translation model configured. Select one in settings first.",
                small = true,
                muted = true,
            )
        }
        buttonItem(
            label = if (translating) "Cancel translation" else "Translate",
            icon = if (translating) WearIcons.Stop else WearIcons.Translate,
            tonal = translating,
            enabled = if (translating) true else inputText.isNotBlank() && !modelMissing,
        ) {
            if (translating) vm.cancelTranslation() else vm.translate()
        }
        buttonItem(
            label = "Clear input",
            icon = WearIcons.Delete,
            tonal = true,
            enabled = inputText.isNotEmpty(),
        ) { vm.updateInputText("") }

        section("Result")
        when {
            translating -> textItem("Translating…", small = true, muted = true)
            translatedText.isBlank() -> textItem("Translation result appears here.", small = true, muted = true)
            else -> textItem(translatedText)
        }
        if (translatedText.isNotBlank() && !translating) {
            buttonItem(label = "Copy result", icon = WearIcons.Copy, tonal = true) {
                if (copyToClipboard(context, translatedText)) {
                    toaster.show("Copied", ToastType.Success)
                } else {
                    toaster.show("Copy failed: content too large for the clipboard", ToastType.Error)
                }
            }
            buttonItem(label = "Use as input", icon = WearIcons.Retry, tonal = true) {
                vm.useResultAsInput()
            }
        }
    }

    if (languagePicker) {
        WearOptionDialog(
            visible = true,
            title = "Target language",
            options = languageNames,
            selectedIndex = TranslationLanguages.indexOfFirst { it == targetLanguage },
            onSelect = { index ->
                languagePicker = false
                TranslationLanguages.getOrNull(index)?.let(vm::updateTargetLanguage)
            },
            onDismiss = { languagePicker = false },
        )
    }
}

private fun copyToClipboard(context: Context, text: String): Boolean = runCatching {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    manager.setPrimaryClip(ClipData.newPlainText("translation", text))
}.isSuccess
