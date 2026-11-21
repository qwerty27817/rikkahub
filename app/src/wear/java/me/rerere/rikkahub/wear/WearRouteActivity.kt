package me.rerere.rikkahub.wear

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.rikkahub.EXTRA_CONVERSATION_ID
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.service.MediaCreationForegroundService
import me.rerere.rikkahub.ui.context.LocalASRState
import me.rerere.rikkahub.ui.context.LocalSettings
import me.rerere.rikkahub.ui.context.LocalTTSState
import me.rerere.rikkahub.ui.context.LocalToaster
import me.rerere.rikkahub.ui.hooks.rememberCustomAsrState
import me.rerere.rikkahub.ui.hooks.rememberCustomTtsState
import me.rerere.rikkahub.ui.theme.RikkahubTheme
import me.rerere.rikkahub.utils.CrashHandler
import me.rerere.rikkahub.wear.shape.LocalScreenShape
import me.rerere.rikkahub.wear.shape.LocalScreenShapeController
import me.rerere.rikkahub.wear.shape.LocalScreenShapeState
import me.rerere.rikkahub.wear.shape.ScreenShapeController
import me.rerere.rikkahub.wear.theme.RikkaWearTheme
import me.rerere.ui.toast.rememberToasterState
import org.koin.android.ext.android.inject

/**
 * 手表端唯一入口：使用 Wear Material 3 UI，不创建任何手机界面。
 *
 * 屏幕形态（圆/方）的判定结果通过 CompositionLocal 下发；Activity 在配置变化时刷新，
 * 设置页改动 SharedPreferences 时由控制器监听回写，二者共用同一份全局状态。
 */
class WearRouteActivity : ComponentActivity() {
    private val settingsStore: SettingsStore by inject()
    private lateinit var shapeController: ScreenShapeController
    private val crashed = mutableStateOf(false)
    private val pendingDestination = mutableStateOf<WearDestination?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // 手表几乎全是 AMOLED：系统「深色模式」经常返回关闭，首次启动直接给深色 + 纯黑背景，
        // 用户仍可在外观设置里改回跟随系统/浅色。
        applyWearThemeDefaults()

        shapeController = ScreenShapeController(
            context = this,
            preferences = getSharedPreferences(ScreenShapeController.PREFERENCES_NAME, Context.MODE_PRIVATE),
        )
        crashed.value = CrashHandler.hasCrashed(this)
        pendingDestination.value = intent.toDestination()

        setContent {
            val settings by settingsStore.settingsFlow.collectAsStateWithLifecycle()
            val shapeState by shapeController.state.collectAsStateWithLifecycle()
            val toasterState = rememberToasterState()
            val ttsState = rememberCustomTtsState()
            val asrState = rememberCustomAsrState()

            RikkahubTheme {
                CompositionLocalProvider(
                    LocalSettings provides settings,
                    LocalToaster provides toasterState,
                    LocalTTSState provides ttsState,
                    LocalASRState provides asrState,
                    LocalScreenShape provides shapeState.shape,
                    LocalScreenShapeState provides shapeState,
                    LocalScreenShapeController provides shapeController,
                ) {
                    RikkaWearTheme(colorScheme = MaterialTheme.colorScheme) {
                        if (crashed.value) {
                            WearCrashRecovery(
                                stackTrace = CrashHandler.getStackTrace(this@WearRouteActivity).orEmpty(),
                                onContinue = {
                                    CrashHandler.clearCrashed(this@WearRouteActivity)
                                    crashed.value = false
                                },
                                onExit = { finish() },
                            )
                        } else {
                            WearApp(
                                pendingDestination = pendingDestination.value,
                                onPendingConsumed = { pendingDestination.value = null },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        shapeController.refresh(newConfig)
    }

    /** 首次启动写入手表端主题默认值，之后完全尊重用户在设置里的选择。 */
    private fun applyWearThemeDefaults() {
        val prefs = getSharedPreferences("rikkahub.preferences", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        var changed = false
        if (!prefs.contains("colorMode")) {
            editor.putString("colorMode", "DARK")
            changed = true
        }
        if (!prefs.contains("amoledDark")) {
            editor.putBoolean("amoledDark", true)
            changed = true
        }
        if (changed) editor.apply()
    }

    override fun onStart() {
        super.onStart()
        shapeController.refresh()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.toDestination()?.let { pendingDestination.value = it }
    }

    /** 通知/深链：会话消息通知 → 聊天页；媒体创作通知 → 对应会话。 */
    private fun Intent.toDestination(): WearDestination? {
        getStringExtra(EXTRA_CONVERSATION_ID)?.let { return WearDestination.Chat(it) }
        getStringExtra(MediaCreationForegroundService.EXTRA_MEDIA_SESSION_ID)?.let {
            return WearDestination.MediaCreation(it)
        }
        return null
    }
}
