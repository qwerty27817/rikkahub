package me.rerere.rikkahub.wear.pages.assistant

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import me.rerere.rikkahub.data.ai.tools.local.LocalToolOption
import me.rerere.rikkahub.ui.context.LocalSettings
import me.rerere.rikkahub.ui.pages.assistant.detail.AssistantDetailVM
import me.rerere.rikkahub.wear.components.WearScreen
import me.rerere.rikkahub.wear.components.section
import me.rerere.rikkahub.wear.components.textItem
import me.rerere.rikkahub.wear.components.toggleItem
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

private data class LocalToolInfo(
    val option: LocalToolOption,
    val title: String,
    val description: String,
)

// 与手机版 AssistantLocalToolPage 里的清单保持一致；手表上不做权限申请，只改配置
private val localToolInfos = listOf(
    LocalToolInfo(
        option = LocalToolOption.JavascriptEngine,
        title = "JavaScript engine",
        description = "Run short scripts inside the chat",
    ),
    LocalToolInfo(
        option = LocalToolOption.TimeInfo,
        title = "Time info",
        description = "Current date, time and time zone",
    ),
    LocalToolInfo(
        option = LocalToolOption.Clipboard,
        title = "Clipboard",
        description = "Read and write the clipboard",
    ),
    LocalToolInfo(
        option = LocalToolOption.Tts,
        title = "Text to speech",
        description = "Speak replies out loud",
    ),
    LocalToolInfo(
        option = LocalToolOption.AskUser,
        title = "Ask user",
        description = "Ask a follow-up question before replying",
    ),
    LocalToolInfo(
        option = LocalToolOption.ScreenTime,
        title = "Screen time",
        description = "App usage on the phone",
    ),
    LocalToolInfo(
        option = LocalToolOption.Calendar,
        title = "Calendar",
        description = "Read and create calendar events (phone)",
    ),
    LocalToolInfo(
        option = LocalToolOption.ChartDisplay,
        title = "Chart display",
        description = "Render charts in replies",
    ),
    LocalToolInfo(
        option = LocalToolOption.PhoneUse,
        title = "Phone use",
        description = "Control the phone (needs accessibility on the phone)",
    ),
)

/** 助手的本地工具开关，字段与手机版一致：assistant.localTools。 */
@Composable
fun WearAssistantToolsPage(
    assistantId: kotlin.uuid.Uuid,
    onBack: () -> Unit,
) {
    val settings = LocalSettings.current
    val scope = rememberCoroutineScope()
    val vm: AssistantDetailVM = koinViewModel(parameters = { parametersOf(assistantId.toString()) })
    val assistant = settings.assistants.find { it.id == assistantId }

    if (assistant == null) {
        WearScreen(title = "Local tools", onBack = onBack) {
            textItem("This assistant no longer exists.")
        }
        return
    }

    WearScreen(title = "Local tools", onBack = onBack) {
        textItem(
            "Tools the assistant may call on this device. Changes apply to the next messages.",
            small = true,
            muted = true,
        )
        section("Tools")
        localToolInfos.forEach { info ->
            val checked = assistant.localTools.contains(info.option)
            toggleItem(
                title = info.title,
                subtitle = info.description,
                checked = checked,
                onCheckedChange = { enabled ->
                    val newTools = if (enabled) {
                        assistant.localTools + info.option
                    } else {
                        assistant.localTools - info.option
                    }
                    scope.launch { vm.update(assistant.copy(localTools = newTools)) }
                },
            )
        }
    }
}
