package me.rerere.rikkahub.wear.pages.assistant

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import me.rerere.rikkahub.R
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
    val titleRes: Int,
    val descriptionRes: Int,
)

// 与手机版 AssistantLocalToolPage 里的清单保持一致；手表上不做权限申请，只改配置
private val localToolInfos = listOf(
    LocalToolInfo(
        option = LocalToolOption.JavascriptEngine,
        titleRes = R.string.wear_asst_tool_javascript_title,
        descriptionRes = R.string.wear_asst_tool_javascript_desc,
    ),
    LocalToolInfo(
        option = LocalToolOption.TimeInfo,
        titleRes = R.string.wear_asst_tool_time_info_title,
        descriptionRes = R.string.wear_asst_tool_time_info_desc,
    ),
    LocalToolInfo(
        option = LocalToolOption.Clipboard,
        titleRes = R.string.wear_asst_tool_clipboard_title,
        descriptionRes = R.string.wear_asst_tool_clipboard_desc,
    ),
    LocalToolInfo(
        option = LocalToolOption.Tts,
        titleRes = R.string.wear_asst_tool_tts_title,
        descriptionRes = R.string.wear_asst_tool_tts_desc,
    ),
    LocalToolInfo(
        option = LocalToolOption.AskUser,
        titleRes = R.string.wear_asst_tool_ask_user_title,
        descriptionRes = R.string.wear_asst_tool_ask_user_desc,
    ),
    LocalToolInfo(
        option = LocalToolOption.ScreenTime,
        titleRes = R.string.wear_asst_tool_screen_time_title,
        descriptionRes = R.string.wear_asst_tool_screen_time_desc,
    ),
    LocalToolInfo(
        option = LocalToolOption.Calendar,
        titleRes = R.string.wear_asst_tool_calendar_title,
        descriptionRes = R.string.wear_asst_tool_calendar_desc,
    ),
    LocalToolInfo(
        option = LocalToolOption.ChartDisplay,
        titleRes = R.string.wear_asst_tool_chart_title,
        descriptionRes = R.string.wear_asst_tool_chart_desc,
    ),
    LocalToolInfo(
        option = LocalToolOption.PhoneUse,
        titleRes = R.string.wear_asst_tool_phone_use_title,
        descriptionRes = R.string.wear_asst_tool_phone_use_desc,
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

    // WearScreen 的 content 是普通 DSL builder（非 @Composable），字符串要先在外部取好。
    val localToolsTitle = stringResource(R.string.wear_asst_local_tools)
    val missingAssistant = stringResource(R.string.wear_asst_assistant_missing)

    if (assistant == null) {
        WearScreen(title = localToolsTitle, onBack = onBack) {
            textItem(missingAssistant)
        }
        return
    }

    val toolsDesc = stringResource(R.string.wear_asst_local_tools_desc)
    val toolsSectionLabel = stringResource(R.string.wear_asst_tools)
    val toolTexts = mutableMapOf<LocalToolOption, Pair<String, String>>()
    for (info in localToolInfos) {
        toolTexts[info.option] = stringResource(info.titleRes) to
            stringResource(info.descriptionRes)
    }

    WearScreen(title = localToolsTitle, onBack = onBack) {
        textItem(
            toolsDesc,
            small = true,
            muted = true,
        )
        section(toolsSectionLabel)
        localToolInfos.forEach { info ->
            val checked = assistant.localTools.contains(info.option)
            val (title, description) = toolTexts.getValue(info.option)
            toggleItem(
                title = title,
                subtitle = description,
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
