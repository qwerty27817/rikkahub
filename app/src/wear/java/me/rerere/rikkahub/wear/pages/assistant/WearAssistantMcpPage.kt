package me.rerere.rikkahub.wear.pages.assistant

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.CheckboxButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import kotlinx.coroutines.launch
import me.rerere.rikkahub.data.ai.mcp.McpServerConfig
import me.rerere.rikkahub.data.ai.mcp.serverUrl
import me.rerere.rikkahub.ui.context.LocalSettings
import me.rerere.rikkahub.ui.pages.assistant.detail.AssistantDetailVM
import me.rerere.rikkahub.wear.components.WearScreen
import me.rerere.rikkahub.wear.components.infoItem
import me.rerere.rikkahub.wear.components.textItem
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * 给当前助手选择 MCP Server。
 *
 * 与手机版 McpPicker 一样，只列出在设置里启用的 Server，勾选结果写进 assistant.mcpServers。
 */
@Composable
fun WearAssistantMcpPage(
    assistantId: kotlin.uuid.Uuid,
    onBack: () -> Unit,
) {
    val settings = LocalSettings.current
    val scope = rememberCoroutineScope()
    val vm: AssistantDetailVM = koinViewModel(parameters = { parametersOf(assistantId.toString()) })
    val assistant = settings.assistants.find { it.id == assistantId }
    val servers by vm.mcpServerConfigs.collectAsStateWithLifecycle()

    if (assistant == null) {
        WearScreen(title = "MCP", onBack = onBack) {
            textItem("This assistant no longer exists.")
        }
        return
    }

    WearScreen(title = "MCP servers", onBack = onBack) {
        val enabledServers = servers.filter { it.commonOptions.enable }
        if (enabledServers.isEmpty()) {
            textItem(
                "No MCP servers configured. Add servers in Settings → MCP, then come back to assign them.",
            )
        } else {
            textItem("Servers this assistant can use in chats.", small = true, muted = true)
            enabledServers.forEach { server ->
                item(key = "mcp:${server.id}") {
                    McpServerToggle(
                        server = server,
                        checked = server.id in assistant.mcpServers,
                        onCheckedChange = { checked ->
                            val newIds = if (checked) {
                                assistant.mcpServers + server.id
                            } else {
                                assistant.mcpServers - server.id
                            }
                            scope.launch { vm.update(assistant.copy(mcpServers = newIds)) }
                        },
                    )
                }
            }
            infoItem("Assigned", "${assistant.mcpServers.count { id -> enabledServers.any { it.id == id } }}")
        }
    }
}

@Composable
private fun McpServerToggle(
    server: McpServerConfig,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    CheckboxButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = Modifier.fillMaxWidth(),
        secondaryLabel = {
            Text(
                text = server.serverUrl,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        label = { Text(server.commonOptions.name.ifBlank { "MCP server" }) },
    )
}
