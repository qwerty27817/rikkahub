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
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.rikkahub.R
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

    // WearScreen 的 content 是普通 DSL builder（非 @Composable），先算好所有字符串。
    val title = stringResource(R.string.wear_misc_translator_title)
    val translationFailed = stringResource(R.string.wear_misc_translator_error)
    val textLabel = stringResource(R.string.wear_misc_translator_input_label)
    val textPlaceholder = stringResource(R.string.wear_misc_translator_input_placeholder)
    val sourceLabel = stringResource(R.string.wear_misc_source)
    val autoDetect = stringResource(R.string.wear_misc_auto_detect)
    val targetLabel = stringResource(R.string.wear_misc_target)
    val modelMissingHint = stringResource(R.string.wear_misc_translator_model_missing)
    val cancelTranslation = stringResource(R.string.wear_misc_translator_cancel)
    val translate = stringResource(R.string.wear_misc_translator_translate)
    val clearInput = stringResource(R.string.wear_misc_clear_input)
    val resultSection = stringResource(R.string.wear_misc_translator_result_section)
    val translatingLabel = stringResource(R.string.wear_misc_translator_translating)
    val resultPlaceholder = stringResource(R.string.wear_misc_translator_result_placeholder)
    val copyResult = stringResource(R.string.wear_misc_translator_copy_result)
    val copied = stringResource(R.string.wear_misc_copied)
    val copyFailed = stringResource(R.string.wear_misc_copy_failed)
    val useAsInput = stringResource(R.string.wear_misc_translator_use_as_input)
    val targetLanguageTitle = stringResource(R.string.wear_misc_translator_target_language)

    LaunchedEffect(vm) {
        vm.errorFlow.collect { error ->
            toaster.show(error.message ?: translationFailed, ToastType.Error)
        }
    }

    val modelMissing = remember(settings) {
        !settings.init && settings.providers.findModelById(settings.translateModeId) == null
    }
    val languageNames = TranslationLanguages.map { translationLanguageName(it) }
    val targetLanguageName = translationLanguageName(targetLanguage)

    WearScreen(title = title, onBack = onBack) {
        item(key = "input") {
            WearTextField(
                value = inputText,
                onValueChange = vm::updateInputText,
                label = textLabel,
                placeholder = textPlaceholder,
                singleLine = false,
                minLines = 3,
            )
        }
        infoItem(sourceLabel, autoDetect)
        navItem(
            title = targetLabel,
            subtitle = targetLanguageName,
            icon = WearIcons.Translate,
        ) { languagePicker = true }
        if (modelMissing) {
            textItem(
                modelMissingHint,
                small = true,
                muted = true,
            )
        }
        buttonItem(
            label = if (translating) cancelTranslation else translate,
            icon = if (translating) WearIcons.Stop else WearIcons.Translate,
            tonal = translating,
            enabled = if (translating) true else inputText.isNotBlank() && !modelMissing,
        ) {
            if (translating) vm.cancelTranslation() else vm.translate()
        }
        buttonItem(
            label = clearInput,
            icon = WearIcons.Delete,
            tonal = true,
            enabled = inputText.isNotEmpty(),
        ) { vm.updateInputText("") }

        section(resultSection)
        when {
            translating -> textItem(translatingLabel, small = true, muted = true)
            translatedText.isBlank() -> textItem(resultPlaceholder, small = true, muted = true)
            else -> textItem(translatedText)
        }
        if (translatedText.isNotBlank() && !translating) {
            buttonItem(label = copyResult, icon = WearIcons.Copy, tonal = true) {
                if (copyToClipboard(context, translatedText)) {
                    toaster.show(copied, ToastType.Success)
                } else {
                    toaster.show(copyFailed, ToastType.Error)
                }
            }
            buttonItem(label = useAsInput, icon = WearIcons.Retry, tonal = true) {
                vm.useResultAsInput()
            }
        }
    }

    if (languagePicker) {
        WearOptionDialog(
            visible = true,
            title = targetLanguageTitle,
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
