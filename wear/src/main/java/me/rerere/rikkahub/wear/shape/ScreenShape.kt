package me.rerere.rikkahub.wear.shape

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.view.Display
import android.view.WindowManager
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.rerere.rikkahub.wear.shape.ScreenShapeMode.AUTO
import me.rerere.rikkahub.wear.shape.ScreenShapeMode.ROUND
import me.rerere.rikkahub.wear.shape.ScreenShapeMode.SQUARE
import kotlin.math.abs
import kotlin.math.min

/** 实际渲染使用的屏幕形态。 */
enum class ScreenShape {
    ROUND,
    SQUARE,
}

/** 用户在设置页选择的三态开关。 */
enum class ScreenShapeMode {
    AUTO,
    SQUARE,
    ROUND;

    val storageValue: String
        get() = when (this) {
            AUTO -> "auto"
            SQUARE -> "square"
            ROUND -> "round"
        }

    companion object {
        fun fromStorage(value: String?): ScreenShapeMode = when (value) {
            "square" -> SQUARE
            "round" -> ROUND
            else -> AUTO
        }
    }
}

/** 形态判定的来源，设置页会展示出来，方便非 WearOS 设备排查误判。 */
enum class ScreenShapeSource {
    /** 强制模式。 */
    FORCED,

    /** 系统 Configuration 的 ROUND 标志（WearOS 与部分 AOSP 手表会置位）。 */
    SYSTEM,

    /** 非 WearOS 取不到系统标志时的启发式判定。 */
    HEURISTIC,

    /** 未命中任何手表特征，按方屏处理。 */
    DEFAULT,
}

data class ScreenShapeState(
    val shape: ScreenShape,
    val mode: ScreenShapeMode,
    val source: ScreenShapeSource,
    val isWatch: Boolean,
    val systemRound: Boolean,
    val displayRound: Boolean,
    val widthDp: Int,
    val heightDp: Int,
) {
    val isRound: Boolean get() = shape == ScreenShape.ROUND

    /** 宽高比（短边/长边），1.0 表示正方形。 */
    val aspectRatio: Float
        get() = if (widthDp <= 0 || heightDp <= 0) 1f else {
            min(widthDp, heightDp).toFloat() / maxOf(widthDp, heightDp)
        }

    val diagnosis: String
        get() = "${widthDp}x${heightDp}dp · watch=$isWatch · systemRound=$systemRound · displayRound=$displayRound · ${source.name.lowercase()}"
}

/**
 * 形态判定与三态开关的全局状态。
 *
 * 判定优先级：手动强制 > 系统圆形标志 > 非 WearOS 启发式 > 方屏。
 * 状态放在 StateFlow 里，Activity 与 Compose 都订阅同一份数据，切换立即生效。
 */
class ScreenShapeController(
    context: Context,
    private val preferences: SharedPreferences,
) {
    private val appContext = context.applicationContext

    private val _state = MutableStateFlow(
        detect(
            context = appContext,
            mode = ScreenShapeMode.fromStorage(preferences.getString(KEY_MODE, null)),
        )
    )
    val state: StateFlow<ScreenShapeState> = _state.asStateFlow()

    private val listener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_MODE) {
                refresh(appContext.resources.configuration)
            }
        }

    init {
        preferences.registerOnSharedPreferenceChangeListener(listener)
    }

    fun setMode(mode: ScreenShapeMode) {
        preferences.edit().putString(KEY_MODE, mode.storageValue).apply()
        // apply() 的监听回调是异步的，这里先同步刷新一次保证 UI 立即切换
        refresh(appContext.resources.configuration, mode)
    }

    /** Activity 在 onConfigurationChanged / onStart 时调用，配置变化会重新判定。 */
    fun refresh(configuration: Configuration = appContext.resources.configuration, mode: ScreenShapeMode? = null) {
        _state.value = detect(
            context = appContext,
            mode = mode ?: ScreenShapeMode.fromStorage(preferences.getString(KEY_MODE, null)),
            configuration = configuration,
        )
    }

    companion object {
        const val KEY_MODE = "watch_screen_shape_mode"
        const val PREFERENCES_NAME = "rikkahub.preferences"

        /** 短边不超过该值时，正方形屏按圆形屏处理（大多数圆表为 1:1 且小于 240dp）。 */
        private const val HEURISTIC_SHORT_SIDE_DP = 240

        /** 宽高比容差：宽高比 ≈ 1 才可能是圆屏。 */
        private const val HEURISTIC_ASPECT_TOLERANCE = 0.08f

        fun isWatch(context: Context): Boolean {
            val config = context.resources.configuration
            val uiModeWatch = config.uiMode and Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_WATCH
            val featureWatch = context.packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH)
            return uiModeWatch || featureWatch
        }

        /**
         * 第二个系统信号：部分非 WearOS 的 AOSP 手表不会置 Configuration 的 ROUND 标志，
         * 但 WindowManager 的 Display 会带 FLAG_ROUND（公开 API）。取不到时按 false 处理。
         */
        fun isDisplayRound(context: Context): Boolean = runCatching {
            val windowManager = context.getSystemService(WindowManager::class.java)
                ?: return@runCatching false
            @Suppress("DEPRECATION")
            val display = windowManager.defaultDisplay ?: return@runCatching false
            display.flags and Display.FLAG_ROUND != 0
        }.getOrDefault(false)

        fun detect(
            context: Context,
            mode: ScreenShapeMode,
            configuration: Configuration = context.resources.configuration,
        ): ScreenShapeState {
            val isWatch = isWatch(context)
            val systemRound = configuration.isScreenRound
            val displayRound = isDisplayRound(context)
            val widthDp = configuration.screenWidthDp
            val heightDp = configuration.screenHeightDp
            val shortSide = min(widthDp, heightDp)
            val longSide = maxOf(widthDp, heightDp)
            val aspect = if (longSide > 0) shortSide.toFloat() / longSide else 1f

            val shape: ScreenShape
            val source: ScreenShapeSource
            when {
                mode == SQUARE -> {
                    shape = ScreenShape.SQUARE
                    source = ScreenShapeSource.FORCED
                }
                mode == ROUND -> {
                    shape = ScreenShape.ROUND
                    source = ScreenShapeSource.FORCED
                }
                systemRound || displayRound -> {
                    shape = ScreenShape.ROUND
                    source = ScreenShapeSource.SYSTEM
                }
                // 非 WearOS 取不到 ROUND 标志时：手表 + 近似正方形 + 小屏按圆屏处理
                isWatch && abs(aspect - 1f) <= HEURISTIC_ASPECT_TOLERANCE && shortSide <= HEURISTIC_SHORT_SIDE_DP -> {
                    shape = ScreenShape.ROUND
                    source = ScreenShapeSource.HEURISTIC
                }
                else -> {
                    shape = ScreenShape.SQUARE
                    source = if (isWatch) ScreenShapeSource.HEURISTIC else ScreenShapeSource.DEFAULT
                }
            }

            return ScreenShapeState(
                shape = shape,
                mode = mode,
                source = source,
                isWatch = isWatch,
                systemRound = systemRound,
                displayRound = displayRound,
                widthDp = widthDp,
                heightDp = heightDp,
            )
        }
    }
}

/** 当前形态，布局分支读它。 */
val LocalScreenShape = staticCompositionLocalOf { ScreenShape.SQUARE }

/** 完整判定状态（含来源与诊断信息），设置页读它。 */
val LocalScreenShapeState = staticCompositionLocalOf<ScreenShapeState> {
    error("No ScreenShapeState provided")
}

/** 控制器，设置页写它。 */
val LocalScreenShapeController = staticCompositionLocalOf<ScreenShapeController?> { null }

/** 圆形屏上靠边内容的安全内边距；方屏用常规模数。 */
val ScreenShapeState.horizontalContentPadding: Int
    get() = if (isRound) 10 else 6
