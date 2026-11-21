package me.rerere.rikkahub.wear.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import me.rerere.rikkahub.wear.shape.LocalScreenShape
import me.rerere.rikkahub.wear.shape.ScreenShape

internal class WearSlot(
    val key: Any?,
    /** 同一列表内的自增序号：即使页面用了同名标题作为 key，也不会重复。 */
    val sequence: Int,
    val content: @Composable () -> Unit,
)

/**
 * 与形态无关的列表 DSL。
 *
 * 圆屏走 [ScalingLazyColumn]（瀑布式缩放），方屏走普通 [LazyColumn]；
 * 同一个 [WearList] 调用点覆盖两种形态，页面代码不需要分支。
 *
 * 每个 slot 的最终 key 是「显式 key + 自增序号」：设置页里经常出现同名行
 * （例如两条都叫 “Untitled chat” 的会话），只用标题当 key 会触发 Compose 的重复 key 崩溃。
 */
class WearListScope internal constructor() {
    private var nextSequence = 0
    internal val slots = mutableListOf<WearSlot>()

    fun item(key: Any? = null, content: @Composable () -> Unit) {
        slots += WearSlot(key = key, sequence = nextSequence++, content = content)
    }

    fun items(count: Int, key: ((Int) -> Any)? = null, content: @Composable (Int) -> Unit) {
        repeat(count) { index ->
            item(key = key?.invoke(index)) { content(index) }
        }
    }

    fun items(range: IntRange, key: ((Int) -> Any)? = null, content: @Composable (Int) -> Unit) {
        items(range.count(), key) { content(range.first + it) }
    }
}

class WearListState internal constructor(
    val scalingState: ScalingLazyListState,
    val lazyState: LazyListState,
    val isRound: Boolean,
) {
    val firstVisibleItemIndex: Int
        get() = if (isRound) {
            scalingState.layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0
        } else {
            lazyState.firstVisibleItemIndex
        }

    suspend fun scrollToItem(index: Int) {
        if (isRound) scalingState.scrollToItem(index) else lazyState.scrollToItem(index)
    }

    suspend fun animateScrollToItem(index: Int) {
        if (isRound) scalingState.animateScrollToItem(index) else lazyState.animateScrollToItem(index)
    }

    /** 把列表滚到内容末尾：先定位到最后一个 item，再用一个足够大的偏移把它推到底部。 */
    suspend fun scrollToBottom(lastIndex: Int) {
        if (lastIndex < 0) return
        val bottomOffset = 100_000
        if (isRound) {
            scalingState.scrollToItem(lastIndex, bottomOffset)
        } else {
            lazyState.scrollToItem(lastIndex, bottomOffset)
        }
    }
}

@Composable
fun rememberWearListState(initialFirstVisibleItemIndex: Int = 0): WearListState {
    val isRound = LocalScreenShape.current == ScreenShape.ROUND
    val scalingState = rememberScalingLazyListState(initialFirstVisibleItemIndex)
    val lazyState = rememberLazyListState(initialFirstVisibleItemIndex)
    return remember(isRound, scalingState, lazyState) {
        WearListState(scalingState, lazyState, isRound)
    }
}

@Composable
fun WearList(
    modifier: Modifier = Modifier,
    state: WearListState = rememberWearListState(),
    contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
    content: WearListScope.() -> Unit,
) {
    val scope = WearListScope()
    scope.content()
    val slots = scope.slots
    if (state.isRound) {
        ScalingLazyColumn(
            modifier = modifier.fillMaxWidth(),
            state = state.scalingState,
            contentPadding = contentPadding,
        ) {
            slots.forEach { slot ->
                item(key = slot.renderKey()) { slot.content() }
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxWidth(),
            state = state.lazyState,
            contentPadding = contentPadding,
        ) {
            slots.forEach { slot ->
                item(key = slot.renderKey()) { slot.content() }
            }
        }
    }
}

private fun WearSlot.renderKey(): Any = key?.let { "$it#$sequence" } ?: sequence
