package me.rerere.rikkahub.wear.pages.workspace

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import me.rerere.rikkahub.R
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

    // WearScreen 的 content 是普通 DSL builder（非 @Composable），字符串要先在外部取好。
    val consoleTitle = stringResource(R.string.wear_ws_console_title)
    val rootfsMissing = stringResource(R.string.wear_ws_console_rootfs_missing)
    val commandPlaceholder = stringResource(R.string.wear_ws_console_placeholder)
    val runningLabel = stringResource(R.string.wear_ws_console_running)
    val commandLabel = stringResource(R.string.wear_ws_console_command)
    val runLabel = stringResource(R.string.wear_ws_console_run)
    val clearLabel = stringResource(R.string.wear_ws_console_clear)
    val outputSection = stringResource(R.string.wear_ws_section_output)
    val emptyHint = stringResource(R.string.wear_ws_console_empty)
    val noOutputLabel = stringResource(R.string.wear_ws_console_no_output)

    WearScreen(title = consoleTitle, onBack = onBack) {
        if (!rootfsReady) {
            textItem(rootfsMissing)
        }
        item(key = "input") {
            WearTextField(
                value = input,
                onValueChange = { input = it },
                singleLine = false,
                minLines = 2,
                placeholder = commandPlaceholder,
                label = if (terminal.running) runningLabel else commandLabel,
            )
        }
        buttonItem(
            label = runLabel,
            icon = WearIcons.Play,
            enabled = !terminal.running && input.isNotBlank(),
        ) {
            vm.executeTerminalCommand(input)
            input = ""
        }
        buttonItem(label = clearLabel, icon = WearIcons.Delete, tonal = true) {
            vm.clearTerminal()
        }
        section(outputSection)
        if (terminal.history.isEmpty()) {
            textItem(emptyHint, small = true, muted = true)
        }
        terminal.history.asReversed().forEachIndexed { index, entry ->
            item(key = "history:${terminal.history.size - index}") {
                when (entry) {
                    is WorkspaceTerminalEntry.Command -> Text(
                        text = stringResource(R.string.wear_ws_console_prompt, entry.command),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    is WorkspaceTerminalEntry.Result -> {
                        val exitLine = if (entry.result.timedOut) {
                            stringResource(R.string.wear_ws_console_exit_timed_out, entry.result.exitCode)
                        } else {
                            stringResource(R.string.wear_ws_console_exit, entry.result.exitCode)
                        }
                        val output = buildString {
                            if (entry.result.stdout.isNotBlank()) append(entry.result.stdout.trimEnd())
                            if (entry.result.stderr.isNotBlank()) {
                                if (isNotEmpty()) append('\n')
                                append(entry.result.stderr.trimEnd())
                            }
                            if (isEmpty()) append(noOutputLabel)
                            append('\n')
                            append(exitLine)
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
