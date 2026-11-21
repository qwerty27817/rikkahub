package me.rerere.rikkahub.wear.pages.workspace

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import me.rerere.rikkahub.ui.pages.extensions.workspace.WorkspaceDetailVM
import me.rerere.rikkahub.ui.pages.extensions.workspace.WorkspaceTerminalEntry
import me.rerere.rikkahub.wear.WearIcons
import me.rerere.rikkahub.wear.components.WearScreen
import me.rerere.rikkahub.wear.components.WearTextField
import me.rerere.rikkahub.wear.components.buttonItem
import me.rerere.rikkahub.wear.components.section
import me.rerere.rikkahub.wear.components.textItem
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.uuid.Uuid

/**
 * 手表版 PRoot 控制台。
 *
 * 圆表上直接铺终端字符太挤，这里用「命令 + 输出」的流式控制台：
 * 输入一行命令，PRoot 里执行后把 stdout/stderr 与退出码按最新在前展示。
 */
@Composable
fun WearWorkspaceTerminalPage(
    workspaceId: Uuid,
    onBack: () -> Unit,
) {
    val vm: WorkspaceDetailVM = koinViewModel(
        parameters = { parametersOf(workspaceId.toString()) }
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val terminal by vm.terminalState.collectAsStateWithLifecycle()
    var input by remember { mutableStateOf("") }
    val workspace = state.workspace
    val rootfsReady = workspace?.shellStatus == me.rerere.workspace.WorkspaceShellStatus.READY.name

    WearScreen(title = "Console", onBack = onBack) {
        if (!rootfsReady) {
            textItem("Rootfs is not installed. Open the workspace page and install it first.")
        }
        item(key = "input") {
            WearTextField(
                value = input,
                onValueChange = { input = it },
                singleLine = false,
                minLines = 2,
                placeholder = "uname -a",
                label = if (terminal.running) "Running…" else "Command",
            )
        }
        buttonItem(
            label = "Run",
            icon = WearIcons.Play,
            enabled = !terminal.running && input.isNotBlank(),
        ) {
            vm.executeTerminalCommand(input)
            input = ""
        }
        buttonItem(label = "Clear output", icon = WearIcons.Delete, tonal = true) {
            vm.clearTerminal()
        }
        section("Output")
        if (terminal.history.isEmpty()) {
            textItem("Nothing yet. Try `ls /` or `cat /etc/os-release`.", small = true, muted = true)
        }
        terminal.history.asReversed().forEachIndexed { index, entry ->
            item(key = "history:${terminal.history.size - index}") {
                when (entry) {
                    is WorkspaceTerminalEntry.Command -> Text(
                        text = "$ ${entry.command}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    is WorkspaceTerminalEntry.Result -> {
                        val output = buildString {
                            if (entry.result.stdout.isNotBlank()) append(entry.result.stdout.trimEnd())
                            if (entry.result.stderr.isNotBlank()) {
                                if (isNotEmpty()) append('\n')
                                append(entry.result.stderr.trimEnd())
                            }
                            if (isEmpty()) append("(no output)")
                            append("\n")
                            append("[exit ${entry.result.exitCode}]")
                            if (entry.result.timedOut) append(" timed out")
                        }
                        Text(
                            text = output.take(4000),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    is WorkspaceTerminalEntry.Error -> Text(
                        text = entry.message,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
