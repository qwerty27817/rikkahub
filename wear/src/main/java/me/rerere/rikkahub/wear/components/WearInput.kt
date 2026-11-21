package me.rerere.rikkahub.wear.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme as AppMaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text

/**
 * 手表端文本输入框：Wear M3 没有 TextField，这里用 BasicTextField 套一层紧凑容器，
 * 让 IME 弹出时窗口仍可 adjustResize。
 */
@Composable
fun WearTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
    autoFocus: Boolean = false,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(autoFocus) {
        if (autoFocus) runCatching { focusRequester.requestFocus() }
    }
    Column(modifier = modifier.fillMaxWidth()) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .heightIn(min = 40.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = MaterialTheme.shapes.medium,
                )
                .padding(horizontal = 10.dp, vertical = 8.dp),
            textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSurface),
            singleLine = singleLine,
            minLines = minLines,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty() && placeholder != null) {
                        Text(
                            text = placeholder,
                            style = textStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}

@Composable
fun WearConfirmDialog(
    visible: Boolean,
    title: String,
    message: String? = null,
    confirmLabel: String = "OK",
    dismissLabel: String = "Cancel",
    danger: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return
    AlertDialog(
        visible = visible,
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(confirmLabel, color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text(dismissLabel) }
        },
        title = { Text(title) },
        text = message?.let { { Text(it) } },
    )
}

@Composable
fun WearInputDialog(
    visible: Boolean,
    title: String,
    initialValue: String = "",
    label: String? = null,
    placeholder: String? = null,
    confirmLabel: String = "OK",
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return
    var value = androidx.compose.runtime.remember(visible, initialValue) {
        androidx.compose.runtime.mutableStateOf(initialValue)
    }
    AlertDialog(
        visible = visible,
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = { onConfirm(value.value) }) { Text(confirmLabel) }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text(title) },
        text = {
            WearTextField(
                value = value.value,
                onValueChange = { value.value = it },
                label = label,
                placeholder = placeholder,
                singleLine = singleLine,
                keyboardType = keyboardType,
                autoFocus = true,
            )
        },
    )
}

/** 从一组选项里选一个（模型、语言、Provider 等）。 */
@Composable
fun WearOptionDialog(
    visible: Boolean,
    title: String,
    options: List<String>,
    selectedIndex: Int = -1,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return
    AlertDialog(
        visible = visible,
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onDismiss) { Text("Close") }
        },
        title = { Text(title) },
        content = {
            options.forEachIndexed { index, option ->
                item(key = "option:$index") {
                    Button(
                        onClick = { onSelect(index) },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = option,
                            color = if (index == selectedIndex) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                }
            }
        },
    )
}
