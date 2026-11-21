package me.rerere.rikkahub.wear.pages.extensions

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.CheckboxButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import me.rerere.rikkahub.data.model.Lorebook
import me.rerere.rikkahub.data.model.PromptInjection
import me.rerere.rikkahub.ui.pages.extensions.PromptVM
import me.rerere.rikkahub.wear.WearIcons
import me.rerere.rikkahub.wear.components.WearConfirmDialog
import me.rerere.rikkahub.wear.components.WearInputDialog
import me.rerere.rikkahub.wear.components.WearOptionDialog
import me.rerere.rikkahub.wear.components.WearScreen
import me.rerere.rikkahub.wear.components.buttonItem
import me.rerere.rikkahub.wear.components.section
import me.rerere.rikkahub.wear.components.textItem
import org.koin.androidx.compose.koinViewModel

/**
 * 全局提示词注入管理：模式注入与 Lorebook 的启停、改名、改内容、增删。
 *
 * 手机版还有注入位置/角色/优先级和 Lorebook 条目的完整编辑，手表上保留最常用的几项。
 */
@Composable
fun WearPromptsPage(
    onBack: () -> Unit,
) {
    val vm: PromptVM = koinViewModel()
    val settings by vm.settings.collectAsStateWithLifecycle()

    var modeMenu by remember { mutableStateOf<PromptInjection.ModeInjection?>(null) }
    var renameMode by remember { mutableStateOf<PromptInjection.ModeInjection?>(null) }
    var contentMode by remember { mutableStateOf<PromptInjection.ModeInjection?>(null) }
    var deleteMode by remember { mutableStateOf<PromptInjection.ModeInjection?>(null) }
    var addMode by remember { mutableStateOf(false) }

    var lorebookMenu by remember { mutableStateOf<Lorebook?>(null) }
    var renameLorebook by remember { mutableStateOf<Lorebook?>(null) }
    var descLorebook by remember { mutableStateOf<Lorebook?>(null) }
    var deleteLorebook by remember { mutableStateOf<Lorebook?>(null) }
    var addLorebook by remember { mutableStateOf(false) }

    WearScreen(title = "Prompt injections", onBack = onBack) {
        textItem(
            "Mode injections apply when the matching mode is on. Lorebooks inject entries when their keywords match the chat.",
            small = true,
            muted = true,
        )

        section("Mode injections")
        if (settings.modeInjections.isEmpty()) {
            textItem("No mode injections yet.")
        } else {
            settings.modeInjections.forEach { injection ->
                item(key = "mode:${injection.id}") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CheckboxButton(
                            checked = injection.enabled,
                            onCheckedChange = { checked ->
                                vm.updateSettings { s ->
                                    s.copy(
                                        modeInjections = s.modeInjections.map {
                                            if (it.id == injection.id) it.copy(enabled = checked) else it
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                            label = {
                                Text(
                                    text = injection.name.ifBlank { "Mode injection" },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            secondaryLabel = {
                                Text(
                                    text = modeInjectionSubtitle(injection),
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                        )
                        IconButton(onClick = { modeMenu = injection }) {
                            Icon(imageVector = WearIcons.More, contentDescription = "Manage")
                        }
                    }
                }
            }
        }
        buttonItem(label = "Add mode injection", icon = WearIcons.Add, tonal = true) {
            addMode = true
        }

        section("Lorebooks")
        if (settings.lorebooks.isEmpty()) {
            textItem("No lorebooks yet. Lorebook entries are edited on the phone for now.")
        } else {
            settings.lorebooks.forEach { lorebook ->
                item(key = "lorebook:${lorebook.id}") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CheckboxButton(
                            checked = lorebook.enabled,
                            onCheckedChange = { checked ->
                                vm.updateSettings { s ->
                                    s.copy(
                                        lorebooks = s.lorebooks.map {
                                            if (it.id == lorebook.id) it.copy(enabled = checked) else it
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                            label = {
                                Text(
                                    text = lorebook.name.ifBlank { "Lorebook" },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            secondaryLabel = {
                                Text(
                                    text = lorebookSubtitle(lorebook),
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                        )
                        IconButton(onClick = { lorebookMenu = lorebook }) {
                            Icon(imageVector = WearIcons.More, contentDescription = "Manage")
                        }
                    }
                }
            }
        }
        buttonItem(label = "Add lorebook", icon = WearIcons.Add, tonal = true) {
            addLorebook = true
        }
    }

    modeMenu?.let { target ->
        WearOptionDialog(
            visible = true,
            title = target.name.ifBlank { "Mode injection" },
            options = listOf("Rename", "Edit content", "Delete"),
            onSelect = { index ->
                modeMenu = null
                when (index) {
                    0 -> renameMode = target
                    1 -> contentMode = target
                    else -> deleteMode = target
                }
            },
            onDismiss = { modeMenu = null },
        )
    }

    renameMode?.let { target ->
        WearInputDialog(
            visible = true,
            title = "Rename mode injection",
            initialValue = target.name,
            label = "Name",
            singleLine = true,
            onConfirm = { value ->
                renameMode = null
                val name = value.trim()
                if (name.isNotEmpty()) {
                    vm.updateSettings { s ->
                        s.copy(
                            modeInjections = s.modeInjections.map {
                                if (it.id == target.id) it.copy(name = name) else it
                            }
                        )
                    }
                }
            },
            onDismiss = { renameMode = null },
        )
    }

    contentMode?.let { target ->
        WearInputDialog(
            visible = true,
            title = "Edit content",
            initialValue = target.content,
            label = "Injected text",
            singleLine = false,
            onConfirm = { value ->
                contentMode = null
                vm.updateSettings { s ->
                    s.copy(
                        modeInjections = s.modeInjections.map {
                            if (it.id == target.id) it.copy(content = value) else it
                        }
                    )
                }
            },
            onDismiss = { contentMode = null },
        )
    }

    deleteMode?.let { target ->
        WearConfirmDialog(
            visible = true,
            title = "Delete mode injection?",
            message = target.name.ifBlank { "Mode injection" },
            confirmLabel = "Delete",
            danger = true,
            onConfirm = {
                deleteMode = null
                vm.updateSettings { s ->
                    s.copy(modeInjections = s.modeInjections.filterNot { it.id == target.id })
                }
            },
            onDismiss = { deleteMode = null },
        )
    }

    if (addMode) {
        WearInputDialog(
            visible = true,
            title = "New mode injection",
            initialValue = "",
            label = "Name",
            singleLine = true,
            onConfirm = { value ->
                addMode = false
                val name = value.trim()
                if (name.isNotEmpty()) {
                    vm.updateSettings { s ->
                        s.copy(modeInjections = s.modeInjections + PromptInjection.ModeInjection(name = name))
                    }
                }
            },
            onDismiss = { addMode = false },
        )
    }

    lorebookMenu?.let { target ->
        WearOptionDialog(
            visible = true,
            title = target.name.ifBlank { "Lorebook" },
            options = listOf("Rename", "Edit description", "Delete"),
            onSelect = { index ->
                lorebookMenu = null
                when (index) {
                    0 -> renameLorebook = target
                    1 -> descLorebook = target
                    else -> deleteLorebook = target
                }
            },
            onDismiss = { lorebookMenu = null },
        )
    }

    renameLorebook?.let { target ->
        WearInputDialog(
            visible = true,
            title = "Rename lorebook",
            initialValue = target.name,
            label = "Name",
            singleLine = true,
            onConfirm = { value ->
                renameLorebook = null
                val name = value.trim()
                if (name.isNotEmpty()) {
                    vm.updateSettings { s ->
                        s.copy(lorebooks = s.lorebooks.map { if (it.id == target.id) it.copy(name = name) else it })
                    }
                }
            },
            onDismiss = { renameLorebook = null },
        )
    }

    descLorebook?.let { target ->
        WearInputDialog(
            visible = true,
            title = "Edit description",
            initialValue = target.description,
            label = "Description",
            singleLine = false,
            onConfirm = { value ->
                descLorebook = null
                vm.updateSettings { s ->
                    s.copy(lorebooks = s.lorebooks.map { if (it.id == target.id) it.copy(description = value) else it })
                }
            },
            onDismiss = { descLorebook = null },
        )
    }

    deleteLorebook?.let { target ->
        WearConfirmDialog(
            visible = true,
            title = "Delete lorebook?",
            message = target.name.ifBlank { "Lorebook" },
            confirmLabel = "Delete",
            danger = true,
            onConfirm = {
                deleteLorebook = null
                vm.updateSettings { s -> s.copy(lorebooks = s.lorebooks.filterNot { it.id == target.id }) }
            },
            onDismiss = { deleteLorebook = null },
        )
    }

    if (addLorebook) {
        WearInputDialog(
            visible = true,
            title = "New lorebook",
            initialValue = "",
            label = "Name",
            singleLine = true,
            onConfirm = { value ->
                addLorebook = false
                val name = value.trim()
                if (name.isNotEmpty()) {
                    vm.updateSettings { s -> s.copy(lorebooks = s.lorebooks + Lorebook(name = name)) }
                }
            },
            onDismiss = { addLorebook = false },
        )
    }
}

private fun modeInjectionSubtitle(injection: PromptInjection.ModeInjection): String {
    val position = injection.position.name.lowercase().replace('_', ' ')
    val content = injection.content.replace('\n', ' ').trim().take(60)
    return listOf(position, content).filter { it.isNotBlank() }.joinToString(" · ")
}

private fun lorebookSubtitle(lorebook: Lorebook): String {
    val description = lorebook.description.replace('\n', ' ').trim().take(60)
    return listOf("${lorebook.entries.size} entries", description)
        .filter { it.isNotBlank() }
        .joinToString(" · ")
}
