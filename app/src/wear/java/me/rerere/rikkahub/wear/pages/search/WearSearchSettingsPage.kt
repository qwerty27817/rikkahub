package me.rerere.rikkahub.wear.pages.search

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.launch
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
import me.rerere.rikkahub.wear.components.radioItem
import me.rerere.rikkahub.wear.components.section
import me.rerere.rikkahub.wear.components.textItem
import me.rerere.rikkahub.wear.components.toggleItem
import me.rerere.search.DoubaoSearchMode
import me.rerere.search.SearchServiceOptions
import me.rerere.ui.toast.ToastType
import org.koin.compose.koinInject
import kotlin.uuid.Uuid

/** 手表端搜索设置：provider 列表 + 单个 provider 的配置页（都在同一个 WearDestination 内）。 */
@Composable
fun WearSearchSettingsPage(
    onBack: () -> Unit,
) {
    val settings = LocalSettings.current
    val store: SettingsStore = koinInject()
    val scope = rememberCoroutineScope()
    val toaster = LocalToaster.current

    var editingId by remember { mutableStateOf<Uuid?>(null) }
    var showAddProvider by remember { mutableStateOf(false) }
    var editResultSize by remember { mutableStateOf(false) }

    val services = settings.searchServices
    val typeEntries = SearchServiceOptions.TYPES.entries.toList()
    val editingService = services.find { it.id == editingId }

    if (editingService != null) {
        SearchServiceDetail(
            service = editingService,
            active = services.indexOfFirst { it.id == editingService.id } == settings.searchServiceSelected,
            canDelete = services.size > 1,
            onBack = { editingId = null },
            onUpdate = { updated ->
                scope.launch {
                    store.update { latest ->
                        latest.copy(
                            searchServices = latest.searchServices.map {
                                if (it.id == updated.id) updated else it
                            }
                        )
                    }
                }
            },
            onSetActive = {
                scope.launch {
                    store.update { latest ->
                        latest.copy(
                            searchServiceSelected = latest.searchServices
                                .indexOfFirst { it.id == editingService.id }
                                .coerceAtLeast(0)
                        )
                    }
                }
                toaster.show("Active search provider updated", ToastType.Success)
            },
            onDelete = {
                scope.launch {
                    store.update { latest ->
                        val removedIndex = latest.searchServices.indexOfFirst { it.id == editingService.id }
                        val remaining = latest.searchServices.filterNot { it.id == editingService.id }
                        val selected = when {
                            remaining.isEmpty() -> 0
                            removedIndex >= 0 && removedIndex < latest.searchServiceSelected ->
                                latest.searchServiceSelected - 1
                            else -> latest.searchServiceSelected
                        }.coerceIn(0, (remaining.size - 1).coerceAtLeast(0))
                        latest.copy(
                            searchServices = remaining,
                            searchServiceSelected = selected,
                        )
                    }
                }
                editingId = null
                toaster.show("Search provider deleted", ToastType.Success)
            },
        )
        return
    }

    val titles = numberedTitles(services.map { it.displayName })
    val activeTitle = services.getOrNull(settings.searchServiceSelected)?.displayName

    WearScreen(title = "Search", onBack = onBack) {
        if (services.isEmpty()) {
            textItem("No search providers configured. Add one to enable web search.")
        }
        services.forEachIndexed { index, service ->
            navItem(
                title = titles[index],
                subtitle = buildProviderSubtitle(service, index == settings.searchServiceSelected),
                icon = WearIcons.Search,
                trailingText = if (index == settings.searchServiceSelected) "Active" else null,
                onClick = { editingId = service.id },
            )
        }
        buttonItem(label = "Add search provider", icon = WearIcons.Add, tonal = true) {
            showAddProvider = true
        }

        section("Common options")
        infoItem("Result size", "${settings.searchCommonOptions.resultSize} results per search")
        infoItem("Active provider", activeTitle ?: "None")
        buttonItem(label = "Change result size", icon = WearIcons.Settings, tonal = true) {
            editResultSize = true
        }
    }

    WearOptionDialog(
        visible = showAddProvider,
        title = "Add search provider",
        options = typeEntries.map { it.value },
        onSelect = { index ->
            val type = typeEntries.getOrNull(index)
            val factory = type?.let { SearchServiceOptions.FACTORIES[it.key] }
            if (factory != null) {
                val created = factory()
                showAddProvider = false
                scope.launch {
                    store.update { latest ->
                        latest.copy(searchServices = latest.searchServices + created)
                    }
                }
                editingId = created.id
            }
        },
        onDismiss = { showAddProvider = false },
    )

    if (editResultSize) {
        WearInputDialog(
            visible = true,
            title = "Results per search",
            initialValue = settings.searchCommonOptions.resultSize.toString(),
            label = "1 - 50",
            keyboardType = KeyboardType.Number,
            onConfirm = { value ->
                editResultSize = false
                val size = value.trim().toIntOrNull()
                if (size != null && size in 1..50) {
                    scope.launch {
                        store.update { latest ->
                            latest.copy(searchCommonOptions = latest.searchCommonOptions.copy(resultSize = size))
                        }
                    }
                } else {
                    toaster.show("Result size must be 1 - 50", ToastType.Error)
                }
            },
            onDismiss = { editResultSize = false },
        )
    }
}

private enum class SearchField {
    API_KEY,
    ENDPOINT,
    SEARCH_URL,
    SCRAPE_URL,
    ENGINES,
    LANGUAGE,
    USERNAME,
    PASSWORD,
    GROK_MODEL,
    MAX_TOKENS,
    MAX_TOKENS_PER_PAGE,
    CUSTOM_NAME,
}

private enum class SearchChoice {
    TAVILY_DEPTH,
    LINKUP_DEPTH,
    RIKKAHUB_DEPTH,
    DOUBAO_MODE,
}

@Composable
private fun SearchServiceDetail(
    service: SearchServiceOptions,
    active: Boolean,
    canDelete: Boolean,
    onBack: () -> Unit,
    onUpdate: (SearchServiceOptions) -> Unit,
    onSetActive: () -> Unit,
    onDelete: () -> Unit,
) {
    var editField by remember(service.id) { mutableStateOf<SearchField?>(null) }
    var choice by remember(service.id) { mutableStateOf<SearchChoice?>(null) }
    var confirmDelete by remember(service.id) { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    WearScreen(title = service.displayName.ifBlank { "Search provider" }, onBack = onBack) {
        infoItem("Type", service.displayName)
        radioItem(
            title = "Use for web search",
            subtitle = if (active) "Currently active" else "Tap to make active",
            selected = active,
            onSelect = onSetActive,
        )

        val apiKey = service.apiKeyOrNull()
        if (apiKey != null) {
            navItem(
                title = "API key",
                subtitle = if (apiKey.isBlank()) "Not set" else maskSecret(apiKey),
                icon = WearIcons.Settings,
                onClick = { editField = SearchField.API_KEY },
            )
        }
        val endpoint = service.endpointOrNull()
        if (endpoint != null) {
            navItem(
                title = service.endpointLabel(),
                subtitle = endpoint.ifBlank { "Not set" },
                icon = WearIcons.Web,
                onClick = { editField = SearchField.ENDPOINT },
            )
        }

        when (service) {
            is SearchServiceOptions.TavilyOptions -> {
                toggleItem(
                    title = "Include answer",
                    checked = service.includeAnswer,
                    subtitle = "Ask Tavily for an answer summary",
                    onCheckedChange = { onUpdate(service.copy(includeAnswer = it)) },
                )
                navItem(
                    title = "Search depth",
                    subtitle = service.depth,
                    icon = WearIcons.Search,
                    onClick = { choice = SearchChoice.TAVILY_DEPTH },
                )
            }

            is SearchServiceOptions.BochaOptions -> {
                toggleItem(
                    title = "Summary",
                    checked = service.summary,
                    onCheckedChange = { onUpdate(service.copy(summary = it)) },
                )
            }

            is SearchServiceOptions.LinkUpOptions -> {
                navItem("Depth", service.depth, WearIcons.Search) { choice = SearchChoice.LINKUP_DEPTH }
            }

            is SearchServiceOptions.RikkaHubOptions -> {
                navItem("Depth", service.depth, WearIcons.Search) { choice = SearchChoice.RIKKAHUB_DEPTH }
            }

            is SearchServiceOptions.DoubaoOptions -> {
                navItem(
                    title = "Mode",
                    subtitle = service.mode.name.lowercase(),
                    icon = WearIcons.Search,
                    onClick = { choice = SearchChoice.DOUBAO_MODE },
                )
            }

            is SearchServiceOptions.SearXNGOptions -> {
                navItem("Engines", service.engines.ifBlank { "Default" }, WearIcons.Search) {
                    editField = SearchField.ENGINES
                }
                navItem("Language", service.language.ifBlank { "Auto" }, WearIcons.Translate) {
                    editField = SearchField.LANGUAGE
                }
                navItem("Username", service.username.ifBlank { "Not set" }, WearIcons.Settings) {
                    editField = SearchField.USERNAME
                }
                navItem("Password", maskSecret(service.password), WearIcons.Settings) {
                    editField = SearchField.PASSWORD
                }
            }

            is SearchServiceOptions.JinaOptions -> {
                navItem("Scrape URL", service.scrapeUrl, WearIcons.Web) {
                    editField = SearchField.SCRAPE_URL
                }
            }

            is SearchServiceOptions.GrokOptions -> {
                navItem("Model", service.model, WearIcons.Cpu) {
                    editField = SearchField.GROK_MODEL
                }
                textItem("System prompt is edited on the phone app.", small = true, muted = true)
            }

            is SearchServiceOptions.PerplexityOptions -> {
                navItem(
                    title = "Max tokens",
                    subtitle = service.maxTokens?.toString() ?: "Default",
                    icon = WearIcons.Settings,
                    onClick = { editField = SearchField.MAX_TOKENS },
                )
                navItem(
                    title = "Max tokens per page",
                    subtitle = service.maxTokensPerPage?.toString() ?: "Default",
                    icon = WearIcons.Settings,
                    onClick = { editField = SearchField.MAX_TOKENS_PER_PAGE },
                )
            }

            is SearchServiceOptions.CustomJsOptions -> {
                navItem("Name", service.name.ifBlank { "Custom JS" }, WearIcons.Settings) {
                    editField = SearchField.CUSTOM_NAME
                }
                textItem("Search / scrape scripts are edited on the phone app.", small = true, muted = true)
            }

            is SearchServiceOptions.BingLocalOptions -> {
                textItem("Bing (local) needs no configuration.", maxLines = 2, small = true, muted = true)
            }

            else -> Unit
        }

        if (canDelete) {
            buttonItem(label = "Delete provider", icon = WearIcons.Delete, tonal = true) {
                confirmDelete = true
            }
        }
    }

    val field = editField
    if (field != null) {
        WearInputDialog(
            visible = true,
            title = fieldTitle(field),
            initialValue = service.fieldValue(field),
            singleLine = true,
            keyboardType = if (field == SearchField.MAX_TOKENS || field == SearchField.MAX_TOKENS_PER_PAGE) {
                KeyboardType.Number
            } else {
                KeyboardType.Text
            },
            onConfirm = { value ->
                editField = null
                onUpdate(service.applyField(field, value.trim()))
            },
            onDismiss = { editField = null },
        )
    }

    val currentChoice = choice
    if (currentChoice != null) {
        val options = choiceOptions(currentChoice)
        WearOptionDialog(
            visible = true,
            title = choiceTitle(currentChoice),
            options = options,
            selectedIndex = choiceSelectedIndex(service, currentChoice),
            onSelect = { index ->
                val value = options.getOrNull(index)
                choice = null
                if (value != null) {
                    onUpdate(service.applyChoice(currentChoice, value))
                }
            },
            onDismiss = { choice = null },
        )
    }

    WearConfirmDialog(
        visible = confirmDelete,
        title = "Delete provider?",
        message = "The provider configuration will be removed.",
        confirmLabel = "Delete",
        danger = true,
        onConfirm = {
            confirmDelete = false
            onDelete()
        },
        onDismiss = { confirmDelete = false },
    )
}

private fun buildProviderSubtitle(service: SearchServiceOptions, active: Boolean): String {
    val apiKey = service.apiKeyOrNull()
    val state = when {
        apiKey == null -> "No credentials required"
        apiKey.isBlank() -> "API key missing"
        else -> "Configured"
    }
    return if (active) "$state · active" else state
}

private fun maskSecret(value: String): String = when {
    value.isBlank() -> "Not set"
    value.length <= 4 -> "••••"
    else -> "••••${value.takeLast(4)}"
}

private fun numberedTitles(names: List<String>): List<String> {
    val seen = mutableMapOf<String, Int>()
    return names.map { name ->
        val label = name.ifBlank { "Unnamed provider" }
        val count = (seen[label] ?: 0) + 1
        seen[label] = count
        if (count == 1) label else "$label #$count"
    }
}

private fun SearchServiceOptions.apiKeyOrNull(): String? = when (this) {
    is SearchServiceOptions.TavilyOptions -> apiKey
    is SearchServiceOptions.ExaOptions -> apiKey
    is SearchServiceOptions.ZhipuOptions -> apiKey
    is SearchServiceOptions.DoubaoOptions -> apiKey
    is SearchServiceOptions.LinkUpOptions -> apiKey
    is SearchServiceOptions.BraveOptions -> apiKey
    is SearchServiceOptions.MetasoOptions -> apiKey
    is SearchServiceOptions.OllamaOptions -> apiKey
    is SearchServiceOptions.PerplexityOptions -> apiKey
    is SearchServiceOptions.FirecrawlOptions -> apiKey
    is SearchServiceOptions.JinaOptions -> apiKey
    is SearchServiceOptions.BochaOptions -> apiKey
    is SearchServiceOptions.RikkaHubOptions -> apiKey
    is SearchServiceOptions.GrokOptions -> apiKey
    is SearchServiceOptions.TinyfishOptions -> apiKey
    is SearchServiceOptions.SerperOptions -> apiKey
    is SearchServiceOptions.DeepSeekOptions -> apiKey
    is SearchServiceOptions.SearXNGOptions -> null
    is SearchServiceOptions.BingLocalOptions -> null
    is SearchServiceOptions.CustomJsOptions -> null
}

private fun SearchServiceOptions.withApiKey(value: String): SearchServiceOptions = when (this) {
    is SearchServiceOptions.TavilyOptions -> copy(apiKey = value)
    is SearchServiceOptions.ExaOptions -> copy(apiKey = value)
    is SearchServiceOptions.ZhipuOptions -> copy(apiKey = value)
    is SearchServiceOptions.DoubaoOptions -> copy(apiKey = value)
    is SearchServiceOptions.LinkUpOptions -> copy(apiKey = value)
    is SearchServiceOptions.BraveOptions -> copy(apiKey = value)
    is SearchServiceOptions.MetasoOptions -> copy(apiKey = value)
    is SearchServiceOptions.OllamaOptions -> copy(apiKey = value)
    is SearchServiceOptions.PerplexityOptions -> copy(apiKey = value)
    is SearchServiceOptions.FirecrawlOptions -> copy(apiKey = value)
    is SearchServiceOptions.JinaOptions -> copy(apiKey = value)
    is SearchServiceOptions.BochaOptions -> copy(apiKey = value)
    is SearchServiceOptions.RikkaHubOptions -> copy(apiKey = value)
    is SearchServiceOptions.GrokOptions -> copy(apiKey = value)
    is SearchServiceOptions.TinyfishOptions -> copy(apiKey = value)
    is SearchServiceOptions.SerperOptions -> copy(apiKey = value)
    is SearchServiceOptions.DeepSeekOptions -> copy(apiKey = value)
    else -> this
}

private fun SearchServiceOptions.endpointOrNull(): String? = when (this) {
    is SearchServiceOptions.SearXNGOptions -> url
    is SearchServiceOptions.JinaOptions -> searchUrl
    is SearchServiceOptions.GrokOptions -> customUrl
    else -> null
}

private fun SearchServiceOptions.endpointLabel(): String = when (this) {
    is SearchServiceOptions.SearXNGOptions -> "SearXNG URL"
    is SearchServiceOptions.JinaOptions -> "Search URL"
    is SearchServiceOptions.GrokOptions -> "Responses URL"
    else -> "Endpoint"
}

private fun SearchServiceOptions.withEndpoint(value: String): SearchServiceOptions = when (this) {
    is SearchServiceOptions.SearXNGOptions -> copy(url = value)
    is SearchServiceOptions.JinaOptions -> copy(searchUrl = value)
    is SearchServiceOptions.GrokOptions -> copy(customUrl = value)
    else -> this
}

private fun fieldTitle(field: SearchField): String = when (field) {
    SearchField.API_KEY -> "API key"
    SearchField.ENDPOINT -> "Endpoint"
    SearchField.SEARCH_URL -> "Search URL"
    SearchField.SCRAPE_URL -> "Scrape URL"
    SearchField.ENGINES -> "Engines"
    SearchField.LANGUAGE -> "Language"
    SearchField.USERNAME -> "Username"
    SearchField.PASSWORD -> "Password"
    SearchField.GROK_MODEL -> "Model"
    SearchField.MAX_TOKENS -> "Max tokens"
    SearchField.MAX_TOKENS_PER_PAGE -> "Max tokens per page"
    SearchField.CUSTOM_NAME -> "Provider name"
}

private fun SearchServiceOptions.fieldValue(field: SearchField): String = when (field) {
    SearchField.API_KEY -> apiKeyOrNull().orEmpty()
    SearchField.ENDPOINT -> endpointOrNull().orEmpty()
    SearchField.SEARCH_URL -> if (this is SearchServiceOptions.JinaOptions) searchUrl else ""
    SearchField.SCRAPE_URL -> if (this is SearchServiceOptions.JinaOptions) scrapeUrl else ""
    SearchField.ENGINES -> if (this is SearchServiceOptions.SearXNGOptions) engines else ""
    SearchField.LANGUAGE -> if (this is SearchServiceOptions.SearXNGOptions) language else ""
    SearchField.USERNAME -> if (this is SearchServiceOptions.SearXNGOptions) username else ""
    SearchField.PASSWORD -> if (this is SearchServiceOptions.SearXNGOptions) password else ""
    SearchField.GROK_MODEL -> if (this is SearchServiceOptions.GrokOptions) model else ""
    SearchField.MAX_TOKENS -> if (this is SearchServiceOptions.PerplexityOptions) maxTokens?.toString().orEmpty() else ""
    SearchField.MAX_TOKENS_PER_PAGE -> if (this is SearchServiceOptions.PerplexityOptions) {
        maxTokensPerPage?.toString().orEmpty()
    } else {
        ""
    }

    SearchField.CUSTOM_NAME -> if (this is SearchServiceOptions.CustomJsOptions) name else ""
}

private fun SearchServiceOptions.applyField(field: SearchField, value: String): SearchServiceOptions = when (field) {
    SearchField.API_KEY -> withApiKey(value)
    SearchField.ENDPOINT -> withEndpoint(value)
    SearchField.SEARCH_URL -> if (this is SearchServiceOptions.JinaOptions) copy(searchUrl = value) else this
    SearchField.SCRAPE_URL -> if (this is SearchServiceOptions.JinaOptions) copy(scrapeUrl = value) else this
    SearchField.ENGINES -> if (this is SearchServiceOptions.SearXNGOptions) copy(engines = value) else this
    SearchField.LANGUAGE -> if (this is SearchServiceOptions.SearXNGOptions) copy(language = value) else this
    SearchField.USERNAME -> if (this is SearchServiceOptions.SearXNGOptions) copy(username = value) else this
    SearchField.PASSWORD -> if (this is SearchServiceOptions.SearXNGOptions) copy(password = value) else this
    SearchField.GROK_MODEL -> if (this is SearchServiceOptions.GrokOptions) copy(model = value) else this
    SearchField.MAX_TOKENS -> if (this is SearchServiceOptions.PerplexityOptions) {
        copy(maxTokens = value.toIntOrNull())
    } else {
        this
    }

    SearchField.MAX_TOKENS_PER_PAGE -> if (this is SearchServiceOptions.PerplexityOptions) {
        copy(maxTokensPerPage = value.toIntOrNull())
    } else {
        this
    }

    SearchField.CUSTOM_NAME -> if (this is SearchServiceOptions.CustomJsOptions) copy(name = value) else this
}

private fun choiceTitle(choice: SearchChoice): String = when (choice) {
    SearchChoice.TAVILY_DEPTH -> "Search depth"
    SearchChoice.LINKUP_DEPTH -> "Depth"
    SearchChoice.RIKKAHUB_DEPTH -> "Depth"
    SearchChoice.DOUBAO_MODE -> "Mode"
}

private fun choiceOptions(choice: SearchChoice): List<String> = when (choice) {
    SearchChoice.TAVILY_DEPTH -> listOf("basic", "advanced")
    SearchChoice.LINKUP_DEPTH -> listOf("standard", "deep")
    SearchChoice.RIKKAHUB_DEPTH -> listOf("standard", "deep")
    SearchChoice.DOUBAO_MODE -> DoubaoSearchMode.entries.map { it.name.lowercase() }
}

private fun choiceSelectedIndex(service: SearchServiceOptions, choice: SearchChoice): Int = when (choice) {
    SearchChoice.TAVILY_DEPTH -> {
        val current = (service as? SearchServiceOptions.TavilyOptions)?.depth
        choiceOptions(choice).indexOfFirst { it == current }
    }

    SearchChoice.LINKUP_DEPTH -> {
        val current = (service as? SearchServiceOptions.LinkUpOptions)?.depth
        choiceOptions(choice).indexOfFirst { it == current }
    }

    SearchChoice.RIKKAHUB_DEPTH -> {
        val current = (service as? SearchServiceOptions.RikkaHubOptions)?.depth
        choiceOptions(choice).indexOfFirst { it == current }
    }

    SearchChoice.DOUBAO_MODE -> {
        val current = (service as? SearchServiceOptions.DoubaoOptions)?.mode
        if (current == null) -1 else DoubaoSearchMode.entries.indexOf(current)
    }
}

private fun SearchServiceOptions.applyChoice(choice: SearchChoice, value: String): SearchServiceOptions = when (choice) {
    SearchChoice.TAVILY_DEPTH -> if (this is SearchServiceOptions.TavilyOptions) copy(depth = value) else this
    SearchChoice.LINKUP_DEPTH -> if (this is SearchServiceOptions.LinkUpOptions) copy(depth = value) else this
    SearchChoice.RIKKAHUB_DEPTH -> if (this is SearchServiceOptions.RikkaHubOptions) copy(depth = value) else this
    SearchChoice.DOUBAO_MODE -> {
        val mode = DoubaoSearchMode.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
        if (this is SearchServiceOptions.DoubaoOptions && mode != null) copy(mode = mode) else this
    }
}
