package me.rerere.rikkahub.wear.pages.mcp

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import me.rerere.rikkahub.data.ai.mcp.McpServerConfig
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.ui.context.LocalSettings
import me.rerere.rikkahub.ui.context.LocalToaster
import me.rerere.rikkahub.wear.WearIcons
import me.rerere.rikkahub.wear.components.WearConfirmDialog
import me.rerere.rikkahub.wear.components.WearInputDialog
import me.rerere.rikkahub.wear.components.WearOptionDialog
import me.rerere.rikkahub.wear.components.WearScreen
import me.rerere.rikkahub.wear.components.buttonItem
import me.rerere.rikkahub.wear.components.infoItem
import me.rerere.rikkahub.wear.components.navItem
import me.rerere.rikkahub.wear.components.textItem
import me.rerere.rikkahub.wear.components.toggleItem
import me.rerere.ui.toast.ToastType
import org.koin.compose.koinInject
import kotlin.uuid.Uuid

private val mcpJson = Json { ignoreUnknownKeys = true }

/** 手表端 MCP 设置：Server 列表 + 单个 Server 的编辑页。 */
@Composable
fun WearMcpSettingsPage(
    onBack: () -> Unit,
) {
    val settings = LocalSettings.current
    val store: SettingsStore = koinInject()
    val scope = rememberCoroutineScope()
    val toaster = LocalToaster.current

    var editingId by remember { mutableStateOf<Uuid?>(null) }
    var showAdd by remember { mutableStateOf(false) }

    val servers = settings.mcpServers
    val editing = servers.find { it.id == editingId }

    if (editing != null) {
        McpServerDetail(
            server = editing,
            onBack = { editingId = null },
            onUpdate = { updated ->
                scope.launch {
                    store.update { latest ->
                        latest.copy(
                            mcpServers = latest.mcpServers.map { if (it.id == updated.id) updated else it }
                        )
                    }
                }
            },
            onDelete = {
                scope.launch {
                    store.update { latest ->
                        latest.copy(mcpServers = latest.mcpServers.filterNot { it.id == editing.id })
                    }
                }
                editingId = null
                toaster.show("MCP server deleted", ToastType.Success)
            },
        )
        return
    }

    val titles = numberedTitles(servers.map { it.commonOptions.name })
    WearScreen(title = "MCP", onBack = onBack) {
        if (servers.isEmpty()) {
            textItem("No MCP servers configured. Add one to connect tools.")
        }
        servers.forEachIndexed { index, server ->
            navItem(
                title = titles[index],
                subtitle = buildServerSubtitle(server),
                icon = WearIcons.Mcp,
                trailingText = if (server.commonOptions.enable) "On" else "Off",
                onClick = { editingId = server.id },
            )
        }
        buttonItem(label = "Add MCP server", icon = WearIcons.Add, tonal = true) {
            showAdd = true
        }
        infoItem("Servers", "${servers.size}")
        textItem(
            text = "OAuth and tool approval are managed on the phone app.",
            maxLines = 3,
            small = true,
            muted = true,
        )
    }

    if (showAdd) {
        WearOptionDialog(
            visible = true,
            title = "Add MCP server",
            options = listOf("Streamable HTTP", "SSE"),
            onSelect = { index ->
                val config = when (index) {
                    0 -> McpServerConfig.StreamableHTTPServer()
                    else -> McpServerConfig.SseTransportServer()
                }
                showAdd = false
                scope.launch {
                    store.update { latest -> latest.copy(mcpServers = latest.mcpServers + config) }
                }
                editingId = config.id
            },
            onDismiss = { showAdd = false },
        )
    }
}

private enum class McpField { NAME, URL, HEADERS }

@Composable
private fun McpServerDetail(
    server: McpServerConfig,
    onBack: () -> Unit,
    onUpdate: (McpServerConfig) -> Unit,
    onDelete: () -> Unit,
) {
    val toaster = LocalToaster.current
    var editField by remember(server.id) { mutableStateOf<McpField?>(null) }
    var confirmDelete by remember(server.id) { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    val name = server.commonOptions.name
    val headers = server.commonOptions.headers
    val url = server.urlOrNull()

    WearScreen(title = name.ifBlank { "MCP server" }, onBack = onBack) {
        infoItem("Transport", server.transportLabel())
        toggleItem(
            title = "Enabled",
            checked = server.commonOptions.enable,
            subtitle = "Disabled servers are not connected",
            onCheckedChange = { enabled -> onUpdate(server.withEnabled(enabled)) },
        )
        navItem(
            title = "Name",
            subtitle = name.ifBlank { "Not set" },
            icon = WearIcons.Settings,
            onClick = { editField = McpField.NAME },
        )
        navItem(
            title = "Server URL",
            subtitle = url.ifBlank { "Not set" },
            icon = WearIcons.Web,
            onClick = { editField = McpField.URL },
        )
        navItem(
            title = "Headers",
            subtitle = when (headers.size) {
                0 -> "None"
                1 -> "1 header"
                else -> "${headers.size} headers"
            },
            icon = WearIcons.Settings,
            onClick = { editField = McpField.HEADERS },
        )
        textItem(
            text = "Headers use a JSON object, e.g. {\"Authorization\": \"Bearer ...\"}",
            maxLines = 3,
            small = true,
            muted = true,
        )
        if (headers.isNotEmpty()) {
            headers.take(5).forEach { (key, value) ->
                infoItem("Header: $key", maskSecret(value))
            }
        }
        buttonItem(label = "Delete server", icon = WearIcons.Delete, tonal = true) {
            confirmDelete = true
        }
    }

    val field = editField
    if (field != null) {
        WearInputDialog(
            visible = true,
            title = when (field) {
                McpField.NAME -> "Server name"
                McpField.URL -> "Server URL"
                McpField.HEADERS -> "Headers JSON"
            },
            initialValue = when (field) {
                McpField.NAME -> name
                McpField.URL -> url
                McpField.HEADERS -> headersToJson(headers)
            },
            singleLine = field != McpField.HEADERS,
            placeholder = when (field) {
                McpField.NAME -> "My MCP server"
                McpField.URL -> "https://example.com/mcp"
                McpField.HEADERS -> "{\"Authorization\":\"Bearer ...\"}"
            },
            onConfirm = { value ->
                editField = null
                when (field) {
                    McpField.NAME -> onUpdate(server.withName(value.trim()))
                    McpField.URL -> onUpdate(server.withUrl(value.trim()))
                    McpField.HEADERS -> {
                        val parsed = parseHeaders(value)
                        if (parsed == null) {
                            toaster.show("Invalid headers JSON", ToastType.Error)
                        } else {
                            onUpdate(server.withHeaders(parsed))
                        }
                    }
                }
            },
            onDismiss = { editField = null },
        )
    }

    WearConfirmDialog(
        visible = confirmDelete,
        title = "Delete MCP server?",
        message = name.ifBlank { "This server" } + " will be removed.",
        confirmLabel = "Delete",
        danger = true,
        onConfirm = {
            confirmDelete = false
            onDelete()
        },
        onDismiss = { confirmDelete = false },
    )
}

private fun numberedTitles(names: List<String>): List<String> {
    val seen = mutableMapOf<String, Int>()
    return names.map { name ->
        val label = name.ifBlank { "Unnamed server" }
        val count = (seen[label] ?: 0) + 1
        seen[label] = count
        if (count == 1) label else "$label #$count"
    }
}

private fun buildServerSubtitle(server: McpServerConfig): String {
    val state = if (server.commonOptions.enable) "Enabled" else "Disabled"
    val url = server.urlOrNull()
    return if (url.isBlank()) "$state · no URL" else "$state · $url"
}

private fun maskSecret(value: String): String = when {
    value.isBlank() -> "Not set"
    value.length <= 6 -> "••••"
    else -> "••••${value.takeLast(4)}"
}

private fun McpServerConfig.urlOrNull(): String = when (this) {
    is McpServerConfig.SseTransportServer -> url
    is McpServerConfig.StreamableHTTPServer -> url
}

private fun McpServerConfig.withUrl(value: String): McpServerConfig = when (this) {
    is McpServerConfig.SseTransportServer -> copy(url = value)
    is McpServerConfig.StreamableHTTPServer -> copy(url = value)
}

private fun McpServerConfig.withName(value: String): McpServerConfig =
    clone(commonOptions = commonOptions.copy(name = value))

private fun McpServerConfig.withEnabled(value: Boolean): McpServerConfig =
    clone(commonOptions = commonOptions.copy(enable = value))

private fun McpServerConfig.withHeaders(headers: List<Pair<String, String>>): McpServerConfig =
    clone(commonOptions = commonOptions.copy(headers = headers))

private fun McpServerConfig.transportLabel(): String = when (this) {
    is McpServerConfig.StreamableHTTPServer -> "Streamable HTTP"
    is McpServerConfig.SseTransportServer -> "SSE"
}

private fun headersToJson(headers: List<Pair<String, String>>): String = buildJsonObject {
    headers.forEach { (key, value) -> put(key, JsonPrimitive(value)) }
}.toString()

/** 解析 `{"Key": "Value"}`；空字符串表示清空，非法 JSON 返回 null。 */
private fun parseHeaders(text: String): List<Pair<String, String>>? {
    if (text.isBlank()) return emptyList()
    return runCatching {
        val obj = mcpJson.parseToJsonElement(text).jsonObject
        obj.entries.mapNotNull { (key, element) ->
            (element as? JsonPrimitive)?.content?.let { key to it }
        }
    }.getOrNull()
}
