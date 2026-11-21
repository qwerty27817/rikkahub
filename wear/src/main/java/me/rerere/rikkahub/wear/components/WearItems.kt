package me.rerere.rikkahub.wear.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CheckboxButton
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListSubHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.Text

fun WearListScope.header(text: String, subtitle: String? = null) {
    item(key = "header:$text") {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            ListHeader { Text(text = text) }
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

fun WearListScope.section(text: String) {
    item(key = "section:$text") {
        ListSubHeader(label = { Text(text = text) })
    }
}

fun WearListScope.spacer(height: Dp = 6.dp) {
    item { Spacer(modifier = Modifier.height(height)) }
}

fun WearListScope.textItem(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    small: Boolean = false,
    muted: Boolean = false,
) {
    item(key = "text:${text.take(32)}") {
        Text(
            text = text,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            style = if (small) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodyMedium,
            color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 设置项：点击进入下一层。 */
fun WearListScope.navItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    trailingText: String? = null,
    onClick: () -> Unit,
) {
    item(key = "nav:$title") {
        Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.bodyLarge)
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (trailingText != null) {
                    Text(
                        text = trailingText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** 设置项：就地开关。 */
fun WearListScope.toggleItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    item(key = "toggle:$title") {
        CheckboxButton(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            secondaryLabel = if (subtitle != null) {
                { Text(text = subtitle, style = MaterialTheme.typography.labelSmall) }
            } else {
                null
            },
            label = { Text(text = title) },
        )
    }
}

/** 单选行，用于三态开关、模型选择等。 */
fun WearListScope.radioItem(
    title: String,
    selected: Boolean,
    onSelect: () -> Unit,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    item(key = "radio:$title") {
        RadioButton(
            selected = selected,
            onSelect = onSelect,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            secondaryLabel = if (subtitle != null) {
                { Text(text = subtitle, style = MaterialTheme.typography.labelSmall) }
            } else {
                null
            },
            label = { Text(text = title) },
        )
    }
}

fun WearListScope.buttonItem(
    label: String,
    icon: ImageVector? = null,
    tonal: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    item(key = "button:$label") {
        val content: @Composable () -> Unit = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (icon != null) {
                    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(text = label)
            }
        }
        if (tonal) {
            FilledTonalButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) { content() }
        } else {
            Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) { content() }
        }
    }
}

/** 只读信息行（标题 + 值），用于展示当前模型、ABI、版本等。 */
fun WearListScope.infoItem(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    item(key = "info:$title") {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(0.38f),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(0.62f),
                textAlign = TextAlign.End,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
