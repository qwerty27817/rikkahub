package me.rerere.rikkahub.wear.pages.mcp

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import me.rerere.rikkahub.R
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
    val deletedMessage = stringResource(R.string.wear_svc_mcp_deleted)
    val addServerLabel = stringResource(R.string.wear_svc_mcp_add)

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
                toaster.show(deletedMessage, ToastType.Success)
            },
        )
        return
    }

    val titles = numberedTitles(servers.map { it.commonOptions.name })
    WearScreen(title = stringResource(R.string.wear_svc_mcp_title), onBack = onBack) {
        if (servers.isEmpty()) {
            textItem(stringResource(R.string.wear_svc_mcp_empty))
        }
        servers.forEachIndexed { index, server ->
            navItem(
                title = titles[index],
                subtitle = buildServerSubtitle(server),
                icon = WearIcons.Mcp,
                trailingText = if (server.commonOptions.enable) {
                    stringResource(R.string.wear_svc_on)
                } else {
                    stringResource(R.string.wear_svc_off)
                },
                onClick = { editingId = server.id },
            )
        }
        buttonItem(label = addServerLabel, icon = WearIcons.Add, tonal = true) {
            showAdd = true
        }
        infoItem(stringResource(R.string.wear_svc_servers), "${servers.size}")
        textItem(
            text = stringResource(R.string.wear_svc_mcp_phone_hint),
            maxLines = 3,
            small = true,
            muted = true,
        )
    }

    if (showAdd) {
        WearOptionDialog(
            visible = true,
            title = addServerLabel,
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
    val notSetLabel = stringResource(R.string.wear_svc_not_set)
    val invalidHeaders = stringResource(R.string.wear_svc_mcp_invalid_headers)
    val defaultTitle = stringResource(R.string.wear_svc_mcp_server)
    val thisServerLabel = stringResource(R.string.wear_svc_this_server)
    val deleteMessage = stringResource(
        R.string.wear_svc_mcp_delete_message,
        name.ifBlank { thisServerLabel },
    )

    WearScreen(title = name.ifBlank { defaultTitle }, onBack = onBack) {
        infoItem(stringResource(R.string.wear_svc_transport), server.transportLabel())
        toggleItem(
            title = stringResource(R.string.wear_svc_enable),
            checked = server.commonOptions.enable,
            subtitle = stringResource(R.string.wear_svc_mcp_disabled_hint),
            onCheckedChange = { enabled -> onUpdate(server.withEnabled(enabled)) },
        )
        navItem(
            title = stringResource(R.string.wear_svc_name),
            subtitle = name.ifBlank { notSetLabel },
            icon = WearIcons.Settings,
            onClick = { editField = McpField.NAME },
        )
        navItem(
            title = stringResource(R.string.wear_svc_mcp_server_url),
            subtitle = url.ifBlank { notSetLabel },
            icon = WearIcons.Web,
            onClick = { editField = McpField.URL },
        )
        navItem(
            title = stringResource(R.string.wear_svc_mcp_headers),
            subtitle = when (headers.size) {
                0 -> stringResource(R.string.wear_svc_none)
                1 -> stringResource(R.string.wear_svc_header_count_one, headers.size)
                else -> stringResource(R.string.wear_svc_header_count_many, headers.size)
            },
            icon = WearIcons.Settings,
            onClick = { editField = McpField.HEADERS },
        )
        textItem(
            text = stringResource(R.string.wear_svc_mcp_headers_help),
            maxLines = 3,
            small = true,
            muted = true,
        )
        if (headers.isNotEmpty()) {
            headers.take(5).forEach { (key, value) ->
                infoItem(stringResource(R.string.wear_svc_mcp_header_item, key), maskSecret(value))
            }
        }
        buttonItem(label = stringResource(R.string.wear_svc_mcp_delete), icon = WearIcons.Delete, tonal = true) {
            confirmDelete = true
        }
    }

    val field = editField
    if (field != null) {
        WearInputDialog(
            visible = true,
            title = when (field) {
                McpField.NAME -> stringResource(R.string.wear_svc_mcp_server_name)
                McpField.URL -> stringResource(R.string.wear_svc_mcp_server_url)
                McpField.HEADERS -> stringResource(R.string.wear_svc_mcp_headers_json)
            },
            initialValue = when (field) {
                McpField.NAME -> name
                McpField.URL -> url
                McpField.HEADERS -> headersToJson(headers)
            },
            singleLine = field != McpField.HEADERS,
            placeholder = when (field) {
                McpField.NAME -> stringResource(R.string.wear_svc_mcp_name_placeholder)
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
                            toaster.show(invalidHeaders, ToastType.Error)
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
        title = stringResource(R.string.wear_svc_mcp_delete_title),
        message = deleteMessage,
        confirmLabel = stringResource(R.string.wear_common_delete),
        danger = true,
        onConfirm = {
            confirmDelete = false
            onDelete()
        },
        onDismiss = { confirmDelete = false },
    )
}

@Composable
private fun numberedTitles(names: List<String>): List<String> {
    val unnamed = stringResource(R.string.wear_svc_unnamed_server)
    val seen = mutableMapOf<String, Int>()
    return names.map { name ->
        val label = name.ifBlank { unnamed }
        val count = (seen[label] ?: 0) + 1
        seen[label] = count
        if (count == 1) label else "$label #$count"
    }
}

@Composable
private fun buildServerSubtitle(server: McpServerConfig): String {
    val state = if (server.commonOptions.enable) {
        stringResource(R.string.wear_svc_enabled)
    } else {
        stringResource(R.string.wear_svc_disabled)
    }
    val url = server.urlOrNull()
    val noUrlLabel = stringResource(R.string.wear_svc_no_url)
    val address = url.ifBlank { noUrlLabel }
    return stringResource(R.string.wear_svc_mcp_subtitle, state, address)
}

@Composable
private fun maskSecret(value: String): String = when {
    value.isBlank() -> stringResource(R.string.wear_svc_not_set)
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
