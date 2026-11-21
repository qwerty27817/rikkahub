package me.rerere.rikkahub.wear.pages.search

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.launch
import me.rerere.rikkahub.R
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
    val activeUpdated = stringResource(R.string.wear_svc_search_active_updated)
    val deletedMessage = stringResource(R.string.wear_svc_search_deleted)
    val addProviderLabel = stringResource(R.string.wear_svc_search_add)

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
                toaster.show(activeUpdated, ToastType.Success)
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
                toaster.show(deletedMessage, ToastType.Success)
            },
        )
        return
    }

    val titles = numberedTitles(services.map { it.displayName })
    val activeTitle = services.getOrNull(settings.searchServiceSelected)?.displayName

    val noneLabel = stringResource(R.string.wear_svc_none)
    WearScreen(title = stringResource(R.string.wear_svc_search_title), onBack = onBack) {
        if (services.isEmpty()) {
            textItem(stringResource(R.string.wear_svc_search_empty))
        }
        services.forEachIndexed { index, service ->
            navItem(
                title = titles[index],
                subtitle = buildProviderSubtitle(service, index == settings.searchServiceSelected),
                icon = WearIcons.Search,
                trailingText = if (index == settings.searchServiceSelected) {
                    stringResource(R.string.wear_svc_active)
                } else {
                    null
                },
                onClick = { editingId = service.id },
            )
        }
        buttonItem(label = addProviderLabel, icon = WearIcons.Add, tonal = true) {
            showAddProvider = true
        }

        section(stringResource(R.string.wear_svc_search_common_options))
        infoItem(
            stringResource(R.string.wear_svc_search_result_size),
            stringResource(R.string.wear_svc_search_results_per_search, settings.searchCommonOptions.resultSize),
        )
        infoItem(stringResource(R.string.wear_svc_search_active_provider), activeTitle ?: noneLabel)
        buttonItem(label = stringResource(R.string.wear_svc_search_change_result_size), icon = WearIcons.Settings, tonal = true) {
            editResultSize = true
        }
    }

    WearOptionDialog(
        visible = showAddProvider,
        title = addProviderLabel,
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
        val resultSizeError = stringResource(R.string.wear_svc_search_result_size_error)
        WearInputDialog(
            visible = true,
            title = stringResource(R.string.wear_svc_search_results_per_search_title),
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
                    toaster.show(resultSizeError, ToastType.Error)
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

    val searchProviderLabel = stringResource(R.string.wear_svc_search_provider)
    val providerTitle = service.displayName.ifBlank { searchProviderLabel }
    val notSetLabel = stringResource(R.string.wear_svc_not_set)
    val currentlyActive = stringResource(R.string.wear_svc_currently_active)
    val tapToActivate = stringResource(R.string.wear_svc_tap_to_activate)
    val defaultLabel = stringResource(R.string.wear_svc_default)
    val autoLabel = stringResource(R.string.wear_svc_auto)
    val customJsLabel = stringResource(R.string.wear_svc_custom_js)
    WearScreen(title = providerTitle, onBack = onBack) {
        infoItem(stringResource(R.string.wear_svc_type), service.displayName)
        radioItem(
            title = stringResource(R.string.wear_svc_search_use_for_web),
            subtitle = if (active) currentlyActive else tapToActivate,
            selected = active,
            onSelect = onSetActive,
        )

        val apiKey = service.apiKeyOrNull()
        if (apiKey != null) {
            navItem(
                title = stringResource(R.string.wear_svc_api_key),
                subtitle = if (apiKey.isBlank()) notSetLabel else maskSecret(apiKey),
                icon = WearIcons.Settings,
                onClick = { editField = SearchField.API_KEY },
            )
        }
        val endpoint = service.endpointOrNull()
        if (endpoint != null) {
            navItem(
                title = service.endpointLabel(),
                subtitle = endpoint.ifBlank { notSetLabel },
                icon = WearIcons.Web,
                onClick = { editField = SearchField.ENDPOINT },
            )
        }

        when (service) {
            is SearchServiceOptions.TavilyOptions -> {
                toggleItem(
                    title = stringResource(R.string.wear_svc_tavily_include_answer),
                    checked = service.includeAnswer,
                    subtitle = stringResource(R.string.wear_svc_tavily_include_answer_desc),
                    onCheckedChange = { onUpdate(service.copy(includeAnswer = it)) },
                )
                navItem(
                    title = stringResource(R.string.wear_svc_search_depth),
                    subtitle = service.depth,
                    icon = WearIcons.Search,
                    onClick = { choice = SearchChoice.TAVILY_DEPTH },
                )
            }

            is SearchServiceOptions.BochaOptions -> {
                toggleItem(
                    title = stringResource(R.string.wear_svc_summary),
                    checked = service.summary,
                    onCheckedChange = { onUpdate(service.copy(summary = it)) },
                )
            }

            is SearchServiceOptions.LinkUpOptions -> {
                navItem(stringResource(R.string.wear_svc_depth), service.depth, WearIcons.Search) {
                    choice = SearchChoice.LINKUP_DEPTH
                }
            }

            is SearchServiceOptions.RikkaHubOptions -> {
                navItem(stringResource(R.string.wear_svc_depth), service.depth, WearIcons.Search) {
                    choice = SearchChoice.RIKKAHUB_DEPTH
                }
            }

            is SearchServiceOptions.DoubaoOptions -> {
                navItem(
                    title = stringResource(R.string.wear_svc_mode),
                    subtitle = service.mode.name.lowercase(),
                    icon = WearIcons.Search,
                    onClick = { choice = SearchChoice.DOUBAO_MODE },
                )
            }

            is SearchServiceOptions.SearXNGOptions -> {
                navItem(
                    stringResource(R.string.wear_svc_engines),
                    service.engines.ifBlank { defaultLabel },
                    WearIcons.Search,
                ) {
                    editField = SearchField.ENGINES
                }
                navItem(
                    stringResource(R.string.wear_svc_language),
                    service.language.ifBlank { autoLabel },
                    WearIcons.Translate,
                ) {
                    editField = SearchField.LANGUAGE
                }
                navItem(
                    stringResource(R.string.wear_svc_username),
                    service.username.ifBlank { notSetLabel },
                    WearIcons.Settings,
                ) {
                    editField = SearchField.USERNAME
                }
                navItem(
                    stringResource(R.string.wear_svc_password),
                    maskSecret(service.password),
                    WearIcons.Settings,
                ) {
                    editField = SearchField.PASSWORD
                }
            }

            is SearchServiceOptions.JinaOptions -> {
                navItem(stringResource(R.string.wear_svc_scrape_url), service.scrapeUrl, WearIcons.Web) {
                    editField = SearchField.SCRAPE_URL
                }
            }

            is SearchServiceOptions.GrokOptions -> {
                navItem(stringResource(R.string.wear_svc_model), service.model, WearIcons.Cpu) {
                    editField = SearchField.GROK_MODEL
                }
                textItem(stringResource(R.string.wear_svc_grok_prompt_hint), small = true, muted = true)
            }

            is SearchServiceOptions.PerplexityOptions -> {
                navItem(
                    title = stringResource(R.string.wear_svc_max_tokens),
                    subtitle = service.maxTokens?.toString() ?: defaultLabel,
                    icon = WearIcons.Settings,
                    onClick = { editField = SearchField.MAX_TOKENS },
                )
                navItem(
                    title = stringResource(R.string.wear_svc_max_tokens_per_page),
                    subtitle = service.maxTokensPerPage?.toString() ?: defaultLabel,
                    icon = WearIcons.Settings,
                    onClick = { editField = SearchField.MAX_TOKENS_PER_PAGE },
                )
            }

            is SearchServiceOptions.CustomJsOptions -> {
                navItem(
                    stringResource(R.string.wear_svc_name),
                    service.name.ifBlank { customJsLabel },
                    WearIcons.Settings,
                ) {
                    editField = SearchField.CUSTOM_NAME
                }
                textItem(stringResource(R.string.wear_svc_custom_js_hint), small = true, muted = true)
            }

            is SearchServiceOptions.BingLocalOptions -> {
                textItem(
                    stringResource(R.string.wear_svc_bing_local_hint),
                    maxLines = 2,
                    small = true,
                    muted = true,
                )
            }

            else -> Unit
        }

        if (canDelete) {
            buttonItem(label = stringResource(R.string.wear_svc_delete_provider), icon = WearIcons.Delete, tonal = true) {
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
        title = stringResource(R.string.wear_svc_delete_provider_title),
        message = stringResource(R.string.wear_svc_search_delete_message),
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
private fun buildProviderSubtitle(service: SearchServiceOptions, active: Boolean): String {
    val apiKey = service.apiKeyOrNull()
    val state = when {
        apiKey == null -> stringResource(R.string.wear_svc_no_credentials_required)
        apiKey.isBlank() -> stringResource(R.string.wear_svc_api_key_missing)
        else -> stringResource(R.string.wear_svc_configured)
    }
    return if (active) stringResource(R.string.wear_svc_active_state, state) else state
}

@Composable
private fun maskSecret(value: String): String = when {
    value.isBlank() -> stringResource(R.string.wear_svc_not_set)
    value.length <= 4 -> "••••"
    else -> "••••${value.takeLast(4)}"
}

@Composable
private fun numberedTitles(names: List<String>): List<String> {
    val unnamed = stringResource(R.string.wear_svc_unnamed_provider)
    val seen = mutableMapOf<String, Int>()
    return names.map { name ->
        val label = name.ifBlank { unnamed }
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

@Composable
private fun SearchServiceOptions.endpointLabel(): String = when (this) {
    is SearchServiceOptions.SearXNGOptions -> stringResource(R.string.wear_svc_searxng_url)
    is SearchServiceOptions.JinaOptions -> stringResource(R.string.wear_svc_search_url)
    is SearchServiceOptions.GrokOptions -> stringResource(R.string.wear_svc_responses_url)
    else -> stringResource(R.string.wear_svc_endpoint)
}

private fun SearchServiceOptions.withEndpoint(value: String): SearchServiceOptions = when (this) {
    is SearchServiceOptions.SearXNGOptions -> copy(url = value)
    is SearchServiceOptions.JinaOptions -> copy(searchUrl = value)
    is SearchServiceOptions.GrokOptions -> copy(customUrl = value)
    else -> this
}

@Composable
private fun fieldTitle(field: SearchField): String = when (field) {
    SearchField.API_KEY -> stringResource(R.string.wear_svc_api_key)
    SearchField.ENDPOINT -> stringResource(R.string.wear_svc_endpoint)
    SearchField.SEARCH_URL -> stringResource(R.string.wear_svc_search_url)
    SearchField.SCRAPE_URL -> stringResource(R.string.wear_svc_scrape_url)
    SearchField.ENGINES -> stringResource(R.string.wear_svc_engines)
    SearchField.LANGUAGE -> stringResource(R.string.wear_svc_language)
    SearchField.USERNAME -> stringResource(R.string.wear_svc_username)
    SearchField.PASSWORD -> stringResource(R.string.wear_svc_password)
    SearchField.GROK_MODEL -> stringResource(R.string.wear_svc_model)
    SearchField.MAX_TOKENS -> stringResource(R.string.wear_svc_max_tokens)
    SearchField.MAX_TOKENS_PER_PAGE -> stringResource(R.string.wear_svc_max_tokens_per_page)
    SearchField.CUSTOM_NAME -> stringResource(R.string.wear_svc_provider_name)
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

@Composable
private fun choiceTitle(choice: SearchChoice): String = when (choice) {
    SearchChoice.TAVILY_DEPTH -> stringResource(R.string.wear_svc_search_depth)
    SearchChoice.LINKUP_DEPTH -> stringResource(R.string.wear_svc_depth)
    SearchChoice.RIKKAHUB_DEPTH -> stringResource(R.string.wear_svc_depth)
    SearchChoice.DOUBAO_MODE -> stringResource(R.string.wear_svc_mode)
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
