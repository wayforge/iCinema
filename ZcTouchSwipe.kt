package com.zainchat.compose.utils

import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.chat.zain.page.activity.ZcBasisActivity

class ZcTouchSwipe(activity: ZcBasisActivity) : DefaultLifecycleObserver {

    private val targetView: View = activity.window.decorView
    private val lifecycle: Lifecycle = activity.lifecycle

    init {
        lifecycle.addObserver(this)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        cancel()
    }

    val size by lazy { Pair(targetView.width, targetView.height) }

    private val centerX: Float get() = targetView.width.toFloat() / 2
    private val centerY: Float get() = targetView.height.toFloat() / 2

    /**
     * 使用示例：
     * ```
     * val simulator = ZcTouchSimulator(activity)
     *
     * // 垂直滑动：从中心向下滑动 100px
     * simulator.swipeVertical(distance = 100f)
     *
     * // 垂直滑动：从中心向上滑动 100px
     * simulator.swipeVertical(distance = -100f)
     *
     * // 水平滑动：从中心向右滑动 100px
     * simulator.swipeHorizontal(distance = 100f)
     *
     * // 水平滑动：从中心向左滑动 100px
     * simulator.swipeHorizontal(distance = -100f)
     *
     * // 自定义起始点：从 y=500 向下滑动 100px
     * simulator.swipeVertical(startY = 500f, distance = 100f)
     *
     * // 自定义滑动：从 (100,100) 到 (300,500)
     * simulator.swipe(startX = 100f, startY = 100f, endX = 300f, endY = 500f)
     *
     * // 取消所有待处理的滑动事件
     * simulator.cancel()
     * ```
     */
    fun swipe(
        startX: Float = centerX,
        startY: Float = centerY,
        endX: Float = startX,
        endY: Float = startY,
        durationMs: Long = 200
    ) {
        performSwipe(startX, startY, endX, endY, durationMs)
    }


    fun swipeVertical(
        startY: Float = centerY,
        distance: Float,
        durationMs: Long = 200
    ) {
        swipe(startY = startY, endY = startY + distance)
    }


    fun swipeHorizontal(
        startX: Float = centerX,
        distance: Float,
        durationMs: Long = 200
    ) {
        swipe(startX = startX, endX = startX + distance)
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

    private fun cancel() {
        targetView.handler?.removeCallbacksAndMessages(null)
    }

    private fun performSwipe(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long
    ) {
        val downTime = SystemClock.uptimeMillis()
        val steps = 12
        val stepDelay = durationMs / steps

        dispatchEvent(downTime, downTime, MotionEvent.ACTION_DOWN, startX, startY)

        for (i in 1..steps) {
            val progress = i.toFloat() / steps
            val x = startX + (endX - startX) * progress
            val y = startY + (endY - startY) * progress
            val eventTime = downTime + i * stepDelay

            targetView.postDelayed({
                dispatchEvent(downTime, eventTime, MotionEvent.ACTION_MOVE, x, y)
            }, i * stepDelay)
        }

        targetView.postDelayed({
            dispatchEvent(downTime, downTime + durationMs, MotionEvent.ACTION_UP, endX, endY)
        }, durationMs)
    }

    companion object {
        var DEBUG = false
        private const val TAG = "TouchSimulator"
    }
}