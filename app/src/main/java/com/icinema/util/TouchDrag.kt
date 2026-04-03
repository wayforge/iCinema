package com.zainchat.compose.utils

import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.chat.zain.page.activity.ZcBasisActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 使用示例：
 * ```
 * val drag = ZcTouchDrag(activity)
 *
 * // 拖动到指定位置
 * drag.drag(endX = 500f, endY = 800f)
 *
 * // 垂直拖动：从中心向下拖动 100px
 * drag.dragVertical(distance = 100f)
 *
 * // 垂直拖动：从中心向上拖动 100px
 * drag.dragVertical(distance = -100f)
 *
 * // 水平拖动：从中心向右拖动 100px
 * drag.dragHorizontal(distance = 100f)
 *
 * // 水平拖动：从中心向左拖动 100px
 * drag.dragHorizontal(distance = -100f)
 *
 * // 自定义起始点拖动
 * drag.drag(startX = 100f, startY = 100f, endX = 500f, endY = 800f)
 *
 * // 中断拖动
 * drag.stopDrag()
 * ```
 */
class TouchDrag(activity: ZcBasisActivity) : DefaultLifecycleObserver {

    private val targetView: View = activity.window.decorView
    private val lifecycle: Lifecycle = activity.lifecycle
    private var dragScope: CoroutineScope? = null
    private var dragJob: Job? = null

    init {
        lifecycle.addObserver(this)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        stopDrag()
    }

    private val centerX: Float get() = targetView.width.toFloat() / 2
    private val centerY: Float get() = targetView.height.toFloat() / 2

    fun drag(
        startX: Float = centerX,
        startY: Float = centerY,
        endX: Float = startX,
        endY: Float = startY,
        durationMs: Long = 500
    ) {
        performDrag(startX, startY, endX, endY, durationMs)
    }

    fun dragVertical(
        startY: Float = centerY,
        distance: Float,
        durationMs: Long = 500
    ) {
        drag(startY = startY, endY = startY + distance, durationMs = durationMs)
    }

    fun dragHorizontal(
        startX: Float = centerX,
        distance: Float,
        durationMs: Long = 500
    ) {
        drag(startX = startX, endX = startX + distance, durationMs = durationMs)
    }

    private fun stopDrag() {
        dragJob?.cancel()
        dragScope?.coroutineContext?.cancelChildren()
        dragScope = null
        dragJob = null
        targetView.handler?.removeCallbacksAndMessages(null)
    }

    private fun dispatchEvent(
        downTime: Long,
        eventTime: Long,
        action: Int,
        x: Float,
        y: Float
    ) {
        val event = MotionEvent.obtain(downTime, eventTime, action, x, y, 0)
        targetView.dispatchTouchEvent(event)
        event.recycle()
    }

    private fun performDrag(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long
    ) {
        dragJob?.cancel()
        dragScope?.coroutineContext?.cancelChildren()

        val downTime = SystemClock.uptimeMillis()
        dispatchEvent(downTime, downTime, MotionEvent.ACTION_DOWN, startX, startY)

        val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        dragScope = scope

        dragJob = scope.launch {
            try {
                val steps = 20
                val stepDelay = durationMs / steps

                for (i in 1..steps) {
                    delay(stepDelay)
                    val progress = i.toFloat() / steps
                    val x = startX + (endX - startX) * progress
                    val y = startY + (endY - startY) * progress
                    val eventTime = SystemClock.uptimeMillis()

                    dispatchEvent(downTime, eventTime, MotionEvent.ACTION_MOVE, x, y)
                }

                delay(stepDelay)
                dispatchEvent(
                    downTime,
                    SystemClock.uptimeMillis(),
                    MotionEvent.ACTION_UP,
                    endX,
                    endY
                )
            } finally {
                dragJob = null
                dragScope = null
            }
        }
    }
}
