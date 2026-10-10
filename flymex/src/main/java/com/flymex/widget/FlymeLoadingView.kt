package com.flymex.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.SystemClock
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import com.flymex.R
import com.flymex.theme.FlymeTheme
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Flyme loading ring (`com.meizu.common.widget.LoadingView`):
 * `mcLoadingRadius` 9.5dp, `mcRingWidth` 3.5dp, track `mc_loading_view_background_light`,
 * arc in the theme colour, optional 14sp caption (`mc_loading_view_text_light`).
 */
open class FlymeLoadingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.flymexLoadingViewStyle,
) : View(context, attrs, defStyleAttr) {

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val arc = RectF()
    private var radius: Float
    private var ringWidth: Float
    private var background: Int
    private var foreground: Int
    private var textGap = 0f
    private var startTime = 0L

    var text: CharSequence? = null
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.FlymeLoadingView, defStyleAttr, 0)
        radius = a.getDimension(R.styleable.FlymeLoadingView_mcLoadingRadius, resources.getDimension(R.dimen.mcLoadingRadius))
        ringWidth = a.getDimension(R.styleable.FlymeLoadingView_mcRingWidth, resources.getDimension(R.dimen.mcRingWidth))
        background = a.getColor(R.styleable.FlymeLoadingView_mcLBackground, context.getColor(R.color.mc_loading_view_background_light))
        foreground = a.getColor(R.styleable.FlymeLoadingView_mcLForeground, FlymeTheme.primary(context))
        text = a.getString(R.styleable.FlymeLoadingView_mcLoadingText)
        textPaint.color = a.getColor(R.styleable.FlymeLoadingView_mcLoadingTextColor, context.getColor(R.color.mc_loading_view_text_light))
        a.recycle()
        textPaint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, resources.displayMetrics)
        textGap = 10f * resources.displayMetrics.density
        ringPaint.strokeWidth = ringWidth
    }

    fun setColors(foreground: Int, background: Int = this.background) {
        this.foreground = foreground
        this.background = background
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val ring = (radius * 2 + ringWidth).roundToInt()
        var w = ring + paddingLeft + paddingRight
        val h = max(ring, textPaint.fontMetricsInt.let { it.bottom - it.top }) + paddingTop + paddingBottom
        val t = text
        if (!t.isNullOrEmpty()) w += (textGap + textPaint.measureText(t.toString())).roundToInt()
        setMeasuredDimension(resolveSize(w, widthMeasureSpec), resolveSize(h, heightMeasureSpec))
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startTime = SystemClock.uptimeMillis()
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        val cx = paddingLeft + ringWidth / 2f + radius
        val cy = paddingTop + (height - paddingTop - paddingBottom) / 2f
        arc.set(cx - radius, cy - radius, cx + radius, cy + radius)
        ringPaint.color = background
        canvas.drawOval(arc, ringPaint)
        val elapsed = SystemClock.uptimeMillis() - startTime
        val angle = (elapsed % 900L) / 900f * 360f
        ringPaint.color = foreground
        canvas.drawArc(arc, angle, 90f, false, ringPaint)
        val t = text
        if (!t.isNullOrEmpty()) {
            val x = cx + radius + ringWidth / 2f + textGap
            val y = cy - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(t.toString(), x, y, textPaint)
        }
        if (isShown) postInvalidateOnAnimation()
    }
}

/**
 * Flyme circular progress (`MeizuCommon.CircleProgressBar`): 33dp ring, 1.75dp stroke,
 * rim `mz_circle_progressbar_rim_color_blue` (#26000000), bar in the theme colour.
 */
open class FlymeCircleProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.flymexCircleProgressBarStyle,
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val arc = RectF()
    private var barWidth: Float
    private var barColor: Int
    private var rimColor: Int
    private var startTime = 0L

    var max: Int = 100
        set(value) {
            field = value.coerceAtLeast(1)
            invalidate()
        }
    var progress: Int = 0
        set(value) {
            field = value.coerceIn(0, max)
            invalidate()
        }
    var indeterminate: Boolean = false
        set(value) {
            field = value
            startTime = SystemClock.uptimeMillis()
            invalidate()
        }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.FlymeCircleProgressBar, defStyleAttr, 0)
        barWidth = a.getDimension(R.styleable.FlymeCircleProgressBar_mcBarWidth, resources.getDimension(R.dimen.mz_circle_progressbar_width))
        barColor = a.getColor(R.styleable.FlymeCircleProgressBar_mcBarColor, FlymeTheme.primary(context))
        rimColor = a.getColor(R.styleable.FlymeCircleProgressBar_mcRimColor, context.getColor(R.color.mz_circle_progressbar_rim_color_blue))
        max = a.getInt(R.styleable.FlymeCircleProgressBar_mcMax, 100)
        progress = a.getInt(R.styleable.FlymeCircleProgressBar_mcProgress, 0)
        indeterminate = a.getBoolean(R.styleable.FlymeCircleProgressBar_mcIndeterminate, false)
        a.recycle()
        paint.strokeWidth = barWidth
    }

    fun setColors(bar: Int, rim: Int = rimColor) {
        barColor = bar
        rimColor = rim
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = resources.getDimensionPixelSize(R.dimen.mz_circle_progressbar_layout_width)
        setMeasuredDimension(
            resolveSize(size + paddingLeft + paddingRight, widthMeasureSpec),
            resolveSize(size + paddingTop + paddingBottom, heightMeasureSpec),
        )
    }

    override fun onDraw(canvas: Canvas) {
        val w = width - paddingLeft - paddingRight
        val h = height - paddingTop - paddingBottom
        val r = minOf(w, h) / 2f - barWidth / 2f
        val cx = paddingLeft + w / 2f
        val cy = paddingTop + h / 2f
        arc.set(cx - r, cy - r, cx + r, cy + r)
        paint.color = rimColor
        canvas.drawOval(arc, paint)
        paint.color = barColor
        if (indeterminate) {
            val elapsed = SystemClock.uptimeMillis() - startTime
            val angle = (elapsed % 1500L) / 1500f * 360f
            canvas.drawArc(arc, angle - 90f, 90f, false, paint)
            if (isShown) postInvalidateOnAnimation()
        } else {
            canvas.drawArc(arc, -90f, 360f * progress / max, false, paint)
        }
    }
}
