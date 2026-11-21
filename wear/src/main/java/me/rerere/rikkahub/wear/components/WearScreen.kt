package me.rerere.rikkahub.wear.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumnDefaults
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScrollIndicator
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.ChevronLeft
import me.rerere.rikkahub.wear.R
import me.rerere.rikkahub.wear.shape.LocalScreenShapeState

/**
 * 手表端页面骨架：圆屏使用 Wear M3 的 ScreenScaffold（带 TimeText 与 ScrollIndicator），
 * 方屏使用紧凑的顶部标题栏 + 列表。两种形态共用 [WearListScope] 与同一个页面实现。
 */
@Composable
fun WearScreen(
    title: String? = null,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    state: WearListState = rememberWearListState(),
    actions: (@Composable () -> Unit)? = null,
    content: @Composable WearListScope.() -> Unit,
) {
    val shapeState = LocalScreenShapeState.current
    if (shapeState.isRound) {
        ScreenScaffold(
            scrollState = state.scalingState,
            edgeButton = {},
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
            timeText = null,
        ) { padding ->
            WearList(state = state, contentPadding = padding) {
                if (title != null) header(title, subtitle)
                if (onBack != null) backItem(onBack)
                content()
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            if (title != null || onBack != null) {
                SquareTopBar(title = title, subtitle = subtitle, onBack = onBack, actions = actions)
            }
            Box(modifier = Modifier.weight(1f)) {
                WearList(
                    state = state,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    content()
                }
                ScrollIndicator(
                    state = state.lazyState,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun SquareTopBar(
    title: String?,
    subtitle: String?,
    onBack: (() -> Unit)?,
    actions: (@Composable () -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 8.dp, top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = HugeIcons.ChevronLeft,
                    contentDescription = stringResource(R.string.wear_back),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        } else {
            Spacer(modifier = Modifier.width(4.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (actions != null) actions()
    }
}

/** 圆屏上用列表项返回；方屏走顶部栏，所以只在圆屏分支调用。 */
internal fun WearListScope.backItem(onBack: () -> Unit) {
    item(key = "nav:back") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalIconButton(
                onClick = onBack,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = HugeIcons.ChevronLeft,
                    contentDescription = stringResource(R.string.wear_back),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/** 供页面在没内容时占满剩余空间使用。 */
@Composable
fun WearBox(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}
