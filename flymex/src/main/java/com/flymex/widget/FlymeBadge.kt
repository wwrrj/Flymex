package com.flymex.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import com.flymex.R
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Flyme badge / tip dot (`MeizuCommon.TipDrawable`, tab dot `mz_tab_view_dot_color`).
 *
 * Dot: 6dp (`mz_tool_bar_tab_view_dot_width_height`) in #ef2828. Count: 16dp pill, 10sp bold
 * white digits, 99+ cap.
 */
open class FlymeBadge @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.flymexBadgeStyle,
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = -0x1
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 10f, resources.displayMetrics)
    }
    private val rect = RectF()
    private val dotSize = resources.getDimension(R.dimen.mz_tool_bar_tab_view_dot_width_height)
    private val pillHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, resources.displayMetrics)
    private val padH = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5f, resources.displayMetrics)

    var color: Int
        set(value) {
            paint.color = value
            invalidate()
        }
        get() = paint.color

    /** 0 shows nothing, >0 shows the number (capped at 99+), unless [dot] is set. */
    var count: Int = 0
        set(value) {
            field = value.coerceAtLeast(0)
            requestLayout()
            invalidate()
        }

    var dot: Boolean = false
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    var maxCount: Int = 99

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.FlymeBadge, defStyleAttr, 0)
        paint.color = a.getColor(R.styleable.FlymeBadge_mcTipColor, context.getColor(R.color.mz_tab_view_dot_color))
        count = a.getInt(R.styleable.FlymeBadge_mcBadgeCount, 0)
        dot = a.getBoolean(R.styleable.FlymeBadge_mcBadgeDot, false)
        a.recycle()
    }

    private fun label(): String = if (count > maxCount) "$maxCount+" else count.toString()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val (w, h) = when {
            dot -> dotSize to dotSize
            count > 0 -> max(pillHeight, textPaint.measureText(label()) + padH * 2) to pillHeight
            else -> 0f to 0f
        }
        setMeasuredDimension(resolveSize(w.roundToInt() + paddingLeft + paddingRight, widthMeasureSpec), resolveSize(h.roundToInt() + paddingTop + paddingBottom, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        if (!dot && count <= 0) return
        rect.set(paddingLeft.toFloat(), paddingTop.toFloat(), (width - paddingRight).toFloat(), (height - paddingBottom).toFloat())
        val r = rect.height() / 2f
        canvas.drawRoundRect(rect, r, r, paint)
        if (!dot) {
            val y = rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(label(), rect.centerX(), y, textPaint)
        }
    }
}
