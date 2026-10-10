package com.flymex.widget

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.core.graphics.ColorUtils
import com.flymex.R
import com.flymex.theme.FlymeTheme

/**
 * Flyme seek bar — `com.meizu.common.widget.MzSeekBar`, measured on Flyme 12.6 (meizu 21 Pro):
 *
 * - track: 22dp tall (`mz_seekbar_height`), 8dp corners (`?cornerMini`), `?colorSurfaceContainerLow`
 * - fill: `?colorBrand`, same corners, wraps the thumb with a 4dp margin (`mz_switch_thumb_padding_left`)
 * - thumb: 14dp white circle (`mz_switch_thumb_to_width`), vertically centred — the same knob as the switch
 * - disabled: fill `mz_seekbar_progress_disable`, track `mz_seekbar_background_color_disable`
 * - `showNumber`: 26dp value bubble (`mcLargeCircleRadis` 13dp, `mcTextNumberSize` 14sp) while dragging
 * - `Widget.Flymex.SeekBar.Step` / [stepItemsCount]: `StepSeekBar` with 3dp spots (`mc_enhance_seekbar_spot_radius`)
 *
 * Drawn in code like MeizuCommon does; the AppCompat touch handling, `progress`/`max` and
 * `OnSeekBarChangeListener` API are unchanged.
 */
open class FlymeSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.flymexSeekBarStyle,
) : AppCompatSeekBar(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val rect = RectF()

    private val trackHeight = resources.getDimension(R.dimen.mz_seekbar_height)
    private val trackRadius = FlymeTheme.dimension(context, R.attr.cornerMini, dp(8f))
    private val thumbSize = resources.getDimension(R.dimen.mz_switch_thumb_to_width)
    private val thumbInset = resources.getDimension(R.dimen.mz_switch_thumb_padding_left)
    private val spotRadius = resources.getDimension(R.dimen.mc_enhance_seekbar_spot_radius)

    private var trackColor = FlymeTheme.color(context, R.attr.colorSurfaceContainerLow)
    private var trackDisabled = context.getColor(R.color.mz_seekbar_background_color_disable)
    private var fillColor = FlymeTheme.brand(context)
    private var fillDisabled = context.getColor(R.color.mz_seekbar_progress_disable)
    private var thumbColor = Color.WHITE

    private var bubbleRadius: Float
    private var bubbleDistance: Float

    /** Show the value bubble while dragging. */
    var showNumber: Boolean
    /** Format the bubble text; default is the raw progress. */
    var numberFormatter: (Int) -> String = { it.toString() }
    /** Number of discrete steps (`mcStepItemsCount`); draws spots and snaps `max` to `steps - 1`. 0 = continuous. */
    var stepItemsCount: Int = 0
        set(value) {
            field = value.coerceAtLeast(0)
            if (field > 1) max = field - 1
            invalidate()
        }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.FlymeSeekBar, defStyleAttr, 0)
        bubbleRadius = a.getDimension(R.styleable.FlymeSeekBar_mcLargeCircleRadis, dp(13f))
        bubbleDistance = a.getDimension(R.styleable.FlymeSeekBar_mcDistanceToCircle, dp(2f))
        textPaint.color = a.getColor(R.styleable.FlymeSeekBar_mcTextNumberColor, Color.WHITE)
        textPaint.textSize = a.getDimension(R.styleable.FlymeSeekBar_mcTextNumberSize, sp(14f))
        showNumber = a.getBoolean(R.styleable.FlymeSeekBar_mcShowNumber, false)
        stepItemsCount = a.getInt(R.styleable.FlymeSeekBar_mcStepItemsCount, 0)
        a.recycle()
        textPaint.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)

        // The platform draws nothing; a transparent 22dp thumb keeps AbsSeekBar's touch→progress
        // mapping aligned with the geometry drawn below (travel = width − 22dp).
        progressDrawable = ColorDrawable(Color.TRANSPARENT)
        background = null
        thumb = GradientDrawable().apply {
            setColor(Color.TRANSPARENT)
            val s = trackHeight.toInt()
            setSize(s, s)
        }
        thumbOffset = 0
        splitTrack = false
        setPadding(0, 0, 0, 0)
        minimumHeight = trackHeight.toInt()
    }

    private fun dp(v: Float) = v * resources.displayMetrics.density
    private fun sp(v: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, resources.displayMetrics)

    /** Override the fill (progress) colour; defaults to `?colorBrand`. */
    fun setColors(fill: Int, track: Int = trackColor, thumb: Int = thumbColor) {
        fillColor = fill
        trackColor = track
        thumbColor = thumb
        invalidate()
    }

    override fun setProgressTintList(tint: ColorStateList?) {
        tint?.let { fillColor = it.defaultColor }
        invalidate()
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val h = trackHeight.toInt() + paddingTop + paddingBottom
        setMeasuredDimension(getDefaultSize(suggestedMinimumWidth, widthMeasureSpec), resolveSize(h, heightMeasureSpec))
    }

    // ---- value bubble: a floating PopupWindow above the thumb, like AnimSeekBar's mcLargeCircleDrawble ----
    private var bubble: android.widget.PopupWindow? = null
    private val bubbleView by lazy {
        object : android.view.View(context) {
            override fun onDraw(c: Canvas) {
                val r = bubbleRadius
                paint.color = fillColor
                c.drawCircle(r, r, r, paint)
                textPaint.alpha = 255
                c.drawText(numberFormatter(progress), r, r - (textPaint.descent() + textPaint.ascent()) / 2f, textPaint)
            }
        }
    }

    private fun thumbCenterX(): Float {
        val left = paddingLeft.toFloat()
        val w = (width - paddingLeft - paddingRight).toFloat()
        val ratio = if (max > 0) progress.toFloat() / max else 0f
        val thumbR = thumbSize / 2f
        return left + thumbInset + thumbR + ratio * (w - 2f * (thumbInset + thumbR))
    }

    private fun updateBubble(show: Boolean) {
        if (!showNumber) return
        val size = (bubbleRadius * 2).toInt()
        val loc = IntArray(2).also { getLocationInWindow(it) }
        val x = loc[0] + thumbCenterX().toInt() - size / 2
        val y = loc[1] + (height - paddingBottom - trackHeight).toInt() - bubbleDistance.toInt() - size
        if (show) {
            val p = bubble ?: android.widget.PopupWindow(bubbleView, size, size, false).also {
                it.isTouchable = false
                it.isClippingEnabled = false
                it.animationStyle = R.style.Animation_Flymex_Snackbar
                bubble = it
            }
            if (p.isShowing) p.update(x, y, -1, -1) else p.showAtLocation(this, android.view.Gravity.NO_GRAVITY, x, y)
            bubbleView.invalidate()
        } else {
            bubble?.dismiss()
        }
    }

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        val handled = super.onTouchEvent(event)
        when (event.actionMasked) {
            android.view.MotionEvent.ACTION_DOWN, android.view.MotionEvent.ACTION_MOVE -> updateBubble(true)
            android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> updateBubble(false)
        }
        return handled
    }

    override fun onDetachedFromWindow() {
        bubble?.dismiss()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        val enabled = isEnabled
        val left = paddingLeft.toFloat()
        val w = (width - paddingLeft - paddingRight).toFloat()
        val top = height - paddingBottom - trackHeight
        val cy = top + trackHeight / 2f
        val ratio = if (max > 0) progress.toFloat() / max else 0f

        // track
        paint.color = if (enabled) trackColor else trackDisabled
        rect.set(left, top, left + w, top + trackHeight)
        canvas.drawRoundRect(rect, trackRadius, trackRadius, paint)

        // step spots (StepSeekBar)
        if (stepItemsCount > 1) {
            paint.color = ColorUtils.setAlphaComponent(FlymeTheme.onSurfaceVariant(context), 0x40)
            val travel = w - trackHeight
            for (i in 0 until stepItemsCount) {
                val x = left + trackHeight / 2f + travel * i / (stepItemsCount - 1)
                canvas.drawCircle(x, cy, spotRadius, paint)
            }
        }

        // fill: ends 4dp past the thumb (fill end = thumb centre + 7dp + 4dp)
        val thumbR = thumbSize / 2f
        val cx = left + thumbInset + thumbR + ratio * (w - 2f * (thumbInset + thumbR))
        paint.color = if (enabled) fillColor else fillDisabled
        rect.set(left, top, (cx + thumbR + thumbInset).coerceAtMost(left + w), top + trackHeight)
        canvas.drawRoundRect(rect, trackRadius, trackRadius, paint)

        // thumb
        paint.color = thumbColor
        canvas.drawCircle(cx, cy, thumbR, paint)

    }
}
