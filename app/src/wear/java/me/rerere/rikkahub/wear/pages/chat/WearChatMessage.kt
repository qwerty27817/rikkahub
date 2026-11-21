package me.rerere.rikkahub.wear.pages.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme as WearMaterialTheme
import coil3.compose.AsyncImage
import me.rerere.ai.core.MessageRole
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.model.MessageNode
import me.rerere.rikkahub.ui.components.richtext.MarkdownBlock
import me.rerere.rikkahub.wear.WearIcons

/**
 * 手表端消息条目：正文复用手机的 Markdown/代码高亮/LaTeX/Mermaid 渲染，
 * 推理与工具调用折叠成紧凑块，分支切换做成左右箭头。
 */
@Composable
fun WearChatMessage(
    node: MessageNode,
    streaming: Boolean,
    onAction: (UIMessage) -> Unit,
    onBranch: (Int) -> Unit,
    onToolApproval: (toolCallId: String, approved: Boolean) -> Unit,
) {
    val message = node.currentMessage
    val isUser = message.role == MessageRole.USER
    val containerColor = if (isUser) {
        WearMaterialTheme.colorScheme.primaryContainer
    } else {
        WearMaterialTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor = if (isUser) {
        WearMaterialTheme.colorScheme.onPrimaryContainer
    } else {
        WearMaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = roleLabel(message.role),
                style = WearMaterialTheme.typography.labelSmall,
                color = WearMaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (node.messages.size > 1) {
                IconButton(onClick = { onBranch(node.selectIndex - 1) }, enabled = node.selectIndex > 0) {
                    Icon(
                        imageVector = WearIcons.Back,
                        contentDescription = stringResource(R.string.wear_chat_previous_branch),
                        tint = WearMaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "${node.selectIndex + 1}/${node.messages.size}",
                    style = WearMaterialTheme.typography.labelSmall,
                    color = WearMaterialTheme.colorScheme.onSurfaceVariant,
                )
                IconButton(
                    onClick = { onBranch(node.selectIndex + 1) },
                    enabled = node.selectIndex < node.messages.lastIndex,
                ) {
                    Icon(
                        imageVector = WearIcons.Forward,
                        contentDescription = stringResource(R.string.wear_chat_next_branch),
                        tint = WearMaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = { onAction(message) }) {
                Icon(
                    imageVector = WearIcons.More,
                    contentDescription = stringResource(R.string.wear_chat_message_actions),
                    tint = WearMaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Surface(
            color = containerColor,
            contentColor = contentColor,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                message.parts.forEachIndexed { index, part ->
                    when (part) {
                        is UIMessagePart.Text -> {
                            MarkdownBlock(
                                content = part.text,
                                streaming = streaming && index == message.parts.lastIndex,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        is UIMessagePart.Reasoning -> {
                            ReasoningBlock(part.reasoning)
                        }
                        is UIMessagePart.Tool -> {
                            ToolBlock(
                                name = part.toolName,
                                pending = part.isPending,
                                executed = part.isExecuted,
                                onApprove = { onToolApproval(part.toolCallId, true) },
                                onDeny = { onToolApproval(part.toolCallId, false) },
                            )
                        }
                        is UIMessagePart.Image -> {
                            AsyncImage(
                                model = part.url,
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 80.dp, max = 180.dp),
                            )
                        }
                        is UIMessagePart.Document -> {
                            SmallLine("📄 ${part.fileName}")
                        }
                        is UIMessagePart.Video ->
                            SmallLine("🎬 ${stringResource(R.string.wear_chat_video_attachment)}")
                        is UIMessagePart.Audio ->
                            SmallLine("🎵 ${stringResource(R.string.wear_chat_audio_attachment)}")
                        is UIMessagePart.ServerTool -> SmallLine("🔎 ${part.toolName}")
                        is UIMessagePart.ToolCall -> SmallLine("🔧 ${part.toolName}")
                        is UIMessagePart.ToolResult ->
                            SmallLine("🔧 ${stringResource(R.string.wear_chat_tool_result, part.toolName)}")
                        else -> SmallLine("[${part::class.simpleName}]")
                    }
                }
                if (streaming) {
                    Text(
                        text = "●",
                        style = WearMaterialTheme.typography.labelSmall,
                        color = WearMaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReasoningBlock(reasoning: String) {
    if (reasoning.isBlank()) return
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(
            text = if (expanded) {
                "${stringResource(R.string.wear_chat_thinking)} ▾"
            } else {
                "${stringResource(R.string.wear_chat_thinking)} ▸"
            },
            style = WearMaterialTheme.typography.labelSmall,
            color = WearMaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
        )
        if (expanded) {
            Text(
                text = reasoning,
                style = WearMaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic),
                color = WearMaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ToolBlock(
    name: String,
    pending: Boolean,
    executed: Boolean,
    onApprove: () -> Unit,
    onDeny: () -> Unit,
) {
    Column {
        SmallLine("🔧 $name")
        when {
            pending -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                androidx.wear.compose.material3.Button(onClick = onApprove) {
                    Text(stringResource(R.string.wear_chat_allow))
                }
                androidx.wear.compose.material3.Button(onClick = onDeny) {
                    Text(stringResource(R.string.wear_chat_deny))
                }
            }
            executed -> Text(
                text = stringResource(R.string.wear_chat_done),
                style = WearMaterialTheme.typography.labelSmall,
                color = WearMaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SmallLine(text: String) {
    Text(
        text = text,
        style = WearMaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
        color = WearMaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun roleLabel(role: MessageRole): String = when (role) {
    MessageRole.USER -> stringResource(R.string.wear_chat_role_user)
    MessageRole.ASSISTANT -> stringResource(R.string.wear_chat_role_assistant)
    MessageRole.SYSTEM -> stringResource(R.string.wear_chat_role_system)
    MessageRole.TOOL -> stringResource(R.string.wear_chat_role_tool)
}
