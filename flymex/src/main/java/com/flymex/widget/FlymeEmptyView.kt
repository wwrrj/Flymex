package com.flymex.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import com.flymex.R
import com.flymex.theme.FlymeTheme
import com.flymex.theme.dpInt
import kotlin.math.roundToInt

/**
 * Flyme empty state (`com.meizu.common.widget.EmptyView`, `layout/mc_empty_view.xml`):
 * centred image, 14sp medium title + 14sp summary in `?colorOnSurfaceVariant`
 * (`TextAppearance.Small.EmptyView.*`), content capped at 256dp
 * (`mc_empty_content_panel_max_width`), optional action button.
 */
open class FlymeEmptyView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.flymexEmptyViewStyle,
) : LinearLayout(context, attrs, defStyleAttr) {

    val imageView: ImageView
    val titleView: TextView
    val summaryView: TextView
    val actionButton: FlymeButton

    var image: Drawable?
        get() = imageView.drawable
        set(value) {
            imageView.setImageDrawable(value)
            imageView.isVisible = value != null
        }
    var title: CharSequence?
        get() = titleView.text
        set(value) {
            titleView.text = value
            titleView.isVisible = !value.isNullOrEmpty()
        }
    var summary: CharSequence?
        get() = summaryView.text
        set(value) {
            summaryView.text = value
            summaryView.isVisible = !value.isNullOrEmpty()
        }

    fun setAction(text: CharSequence?, onClick: (() -> Unit)?) {
        actionButton.text = text
        actionButton.isVisible = !text.isNullOrEmpty()
        actionButton.setOnClickListener { onClick?.invoke() }
    }

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        imageView = ImageView(context).apply { isVisible = false }
        addView(imageView, LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        val maxW = resources.getDimensionPixelSize(R.dimen.mc_empty_content_panel_max_width)
        titleView = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(FlymeTheme.onSurfaceVariant(context))
            gravity = Gravity.CENTER
            includeFontPadding = false
            setLineSpacing(resources.getDimension(R.dimen.mc_empty_tip_line_space), 1f)
            maxWidth = maxW
            isVisible = false
        }
        addView(titleView, LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = resources.getDimensionPixelSize(R.dimen.mc_empty_tips_top_port) })
        summaryView = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(FlymeTheme.onSurfaceVariant(context))
            gravity = Gravity.CENTER
            includeFontPadding = false
            maxWidth = maxW
            isVisible = false
        }
        addView(summaryView, LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = context.dpInt(4f)
            bottomMargin = resources.getDimensionPixelSize(R.dimen.mc_empty_summary_margin_bottom)
        })
        actionButton = FlymeButton(context).apply {
            variant = FlymeButton.Variant.TONAL
            isVisible = false
        }
        addView(actionButton, LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = context.dpInt(16f) })

        val a = context.obtainStyledAttributes(attrs, R.styleable.FlymeEmptyView, defStyleAttr, 0)
        image = a.getDrawable(R.styleable.FlymeEmptyView_mcEmptyImage)
        title = a.getText(R.styleable.FlymeEmptyView_mcEmptyTitle)
        summary = a.getText(R.styleable.FlymeEmptyView_mcEmptySummary)
        setAction(a.getText(R.styleable.FlymeEmptyView_mcEmptyActionText), null)
        a.recycle()
    }
}

/**
 * Flyme page indicator (`com.meizu.common.widget.PagerIndicator`): 2dp dots
 * (`mc_pager_indicator_radius`) growing to 2.5dp (`mc_pager_indicator_enlarge_radius`) when
 * current, 8dp apart (`mc_pager_indicator_distance`); fill `mc_pager_indicator_fill_color`,
 * highlight `mc_pager_indicator_highlight_color`.
 */
open class FlymePageIndicator @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.flymexPageIndicatorStyle,
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var radius: Float
    private var enlargeRadius: Float
    private var distance: Float
    private var fillColor: Int
    private var highlightColor: Int
    private var position = 0f
    private var animator: ValueAnimator? = null

    var count: Int = 0
        set(value) {
            field = value.coerceAtLeast(0)
            requestLayout()
            invalidate()
        }

    var current: Int = 0
        set(value) {
            val v = value.coerceIn(0, (count - 1).coerceAtLeast(0))
            field = v
            animator?.cancel()
            if (isLaidOut) {
                animator = ValueAnimator.ofFloat(position, v.toFloat()).apply {
                    duration = 200
                    interpolator = FlymeSwitch.CHECK_INTERPOLATOR
                    addUpdateListener { position = it.animatedValue as Float; invalidate() }
                    start()
                }
            } else {
                position = v.toFloat()
                invalidate()
            }
        }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.FlymePageIndicator, defStyleAttr, 0)
        radius = a.getDimension(R.styleable.FlymePageIndicator_mcRadius, resources.getDimension(R.dimen.mc_pager_indicator_radius))
        enlargeRadius = a.getDimension(R.styleable.FlymePageIndicator_mcEnlargeRadius, resources.getDimension(R.dimen.mc_pager_indicator_enlarge_radius))
        distance = a.getDimension(R.styleable.FlymePageIndicator_mcDistance, resources.getDimension(R.dimen.mc_pager_indicator_distance))
        fillColor = a.getColor(R.styleable.FlymePageIndicator_mcFillColor, context.getColor(R.color.mc_pager_indicator_fill_color))
        highlightColor = a.getColor(R.styleable.FlymePageIndicator_mcHighlightColor, context.getColor(R.color.mc_pager_indicator_highlight_color))
        count = a.getInt(R.styleable.FlymePageIndicator_mcCount, 0)
        current = a.getInt(R.styleable.FlymePageIndicator_mcCurrent, 0)
        a.recycle()
    }

    /** Use on-surface colours for light pages (the Flyme defaults are for dark overlays). */
    fun useOnSurfaceColors() {
        val on = FlymeTheme.onSurface(context)
        fillColor = androidx.core.graphics.ColorUtils.setAlphaComponent(on, 0x4d)
        highlightColor = on
        invalidate()
    }

    fun setColors(fill: Int, highlight: Int) {
        fillColor = fill
        highlightColor = highlight
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = if (count == 0) 0 else (count * enlargeRadius * 2 + (count - 1) * distance).roundToInt()
        val h = (enlargeRadius * 2).roundToInt()
        setMeasuredDimension(resolveSize(w + paddingLeft + paddingRight, widthMeasureSpec), resolveSize(h + paddingTop + paddingBottom, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        if (count == 0) return
        val cy = paddingTop + (height - paddingTop - paddingBottom) / 2f
        val step = enlargeRadius * 2 + distance
        val startX = paddingLeft + (width - paddingLeft - paddingRight - (count * enlargeRadius * 2 + (count - 1) * distance)) / 2f + enlargeRadius
        for (i in 0 until count) {
            val t = (1f - kotlin.math.abs(position - i)).coerceIn(0f, 1f)
            paint.color = androidx.core.graphics.ColorUtils.blendARGB(fillColor, highlightColor, t)
            canvas.drawCircle(startX + i * step, cy, radius + (enlargeRadius - radius) * t, paint)
        }
    }
}
