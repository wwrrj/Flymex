package com.flymex.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.graphics.ColorUtils
import com.flymex.R
import com.flymex.theme.FlymeTheme
import kotlin.math.roundToInt

/**
 * Flyme download/install button (`com.meizu.common.widget.CircularProgressButton`).
 *
 * Idle: a 56×26dp pill (`mc_cir_progress_button_*`, radius 48dp) with 12sp white text.
 * Progress: collapses to a 26dp ring (3dp stroke) in the theme colour over
 * `?colorSurfaceContainerHigh`. Complete: primary-container pill with primary text.
 */
open class FlymeCircularProgressButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.flymexCircularProgressButtonStyle,
) : View(context, attrs, defStyleAttr) {

    enum class State { IDLE, PROGRESS, COMPLETE, ERROR }

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val rect = RectF()

    private val minW: Int
    private val minH: Int
    private val radius: Float
    private val stroke: Float
    private val colorIdle: Int
    private val colorIndicator: Int
    private val colorIndicatorBg: Int
    private val textColor: Int
    private var textIdle: CharSequence
    private var textComplete: CharSequence
    private var textError: CharSequence

    private var morph = 0f   // 0 = pill, 1 = ring
    private var animator: ValueAnimator? = null

    var state: State = State.IDLE
        set(value) {
            if (field == value) return
            field = value
            animateMorph(if (value == State.PROGRESS) 1f else 0f)
            invalidate()
        }

    /** 0..100 */
    var progress: Int = 0
        set(value) {
            field = value.coerceIn(0, 100)
            invalidate()
        }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.FlymeCircularProgressButton, defStyleAttr, 0)
        val res = resources
        minW = res.getDimensionPixelSize(R.dimen.mc_cir_progress_button_min_width)
        minH = res.getDimensionPixelSize(R.dimen.mc_cir_progress_button_min_height)
        radius = a.getDimension(R.styleable.FlymeCircularProgressButton_mcCirButtonCornerRadius, res.getDimension(R.dimen.mc_cir_progress_button_corner_radius))
        stroke = a.getDimension(R.styleable.FlymeCircularProgressButton_mcCirButtonStrokeWidth, res.getDimension(R.dimen.mc_cir_progress_button_stroke_width))
        colorIdle = a.getColor(R.styleable.FlymeCircularProgressButton_mcCirButtonColorIdle, FlymeTheme.primary(context))
        colorIndicator = a.getColor(R.styleable.FlymeCircularProgressButton_mcCirButtonColorIndicator, FlymeTheme.primary(context))
        colorIndicatorBg = a.getColor(R.styleable.FlymeCircularProgressButton_mcCirButtonColorIndicatorBackground, FlymeTheme.color(context, R.attr.colorSurfaceContainerHigh))
        textIdle = a.getText(R.styleable.FlymeCircularProgressButton_mcCirButtonTextIdle) ?: "安装"
        textComplete = a.getText(R.styleable.FlymeCircularProgressButton_mcCirButtonTextComplete) ?: "打开"
        textError = a.getText(R.styleable.FlymeCircularProgressButton_mcCirButtonTextError) ?: "重试"
        a.recycle()
        textColor = context.getColor(R.color.mc_cir_progress_button_normal_text_color)
        textPaint.textSize = res.getDimension(R.dimen.mc_cir_progress_button_text_size)
        textPaint.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        ring.strokeWidth = stroke
        isClickable = true
    }

    fun setTexts(idle: CharSequence = textIdle, complete: CharSequence = textComplete, error: CharSequence = textError) {
        textIdle = idle; textComplete = complete; textError = error
        requestLayout(); invalidate()
    }

    private fun animateMorph(target: Float) {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(morph, target).apply {
            duration = 200
            interpolator = FlymeSwitch.CHECK_INTERPOLATOR
            addUpdateListener { morph = it.animatedValue as Float; requestLayout(); invalidate() }
            start()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val label = when (state) { State.COMPLETE -> textComplete; State.ERROR -> textError; else -> textIdle }
        val pad = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12f, resources.displayMetrics)
        val pillW = maxOf(minW.toFloat(), textPaint.measureText(label.toString()) + pad * 2)
        val w = (pillW + (minH - pillW) * morph).roundToInt() + paddingLeft + paddingRight
        val h = minH + paddingTop + paddingBottom
        setMeasuredDimension(resolveSize(w, widthMeasureSpec), resolveSize(h, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        val l = paddingLeft.toFloat()
        val t = paddingTop.toFloat()
        val w = (width - paddingLeft - paddingRight).toFloat()
        val h = (height - paddingTop - paddingBottom).toFloat()
        rect.set(l, t, l + w, t + h)
        if (morph < 1f) {
            fill.color = when (state) {
                State.COMPLETE -> FlymeTheme.primaryContainer(context)
                State.ERROR -> FlymeTheme.color(context, R.attr.colorErrorContainer)
                else -> colorIdle
            }
            fill.alpha = (255 * (1f - morph)).toInt()
            canvas.drawRoundRect(rect, radius, radius, fill)
            val label = when (state) { State.COMPLETE -> textComplete; State.ERROR -> textError; else -> textIdle }
            textPaint.color = when (state) {
                State.COMPLETE -> FlymeTheme.primary(context)
                State.ERROR -> FlymeTheme.color(context, androidx.appcompat.R.attr.colorError)
                else -> textColor
            }
            textPaint.alpha = (255 * (1f - morph)).toInt()
            val y = rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(label.toString(), rect.centerX(), y, textPaint)
        }
        if (morph > 0f) {
            val r = minOf(w, h) / 2f - stroke / 2f
            val cx = rect.centerX()
            val cy = rect.centerY()
            rect.set(cx - r, cy - r, cx + r, cy + r)
            ring.color = ColorUtils.setAlphaComponent(colorIndicatorBg, (255 * morph).toInt())
            canvas.drawOval(rect, ring)
            ring.color = ColorUtils.setAlphaComponent(colorIndicator, (255 * morph).toInt())
            canvas.drawArc(rect, -90f, 360f * progress / 100f, false, ring)
        }
    }
}
