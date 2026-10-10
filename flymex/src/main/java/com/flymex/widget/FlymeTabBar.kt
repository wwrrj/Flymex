package com.flymex.widget

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import com.flymex.R
import com.flymex.theme.FlymeTheme
import com.flymex.theme.dpInt

/**
 * Flyme tab bar (`flyme.support.v7.widget.TabScrollView` / `Widget.Flyme.AppCompat.ActionBar.TabBar`).
 *
 * - underline style: 38dp tabs (`mz_tab_height`), 19sp medium text (`TextAppearance.Flyme.H8.Medium`),
 *   selected `?colorPrimary` / unselected `?colorOnSurface` (`mz_title_bar_tab_text_state_color`),
 *   14×3dp indicator (`mz_action_bar_tab_indicator_width/height`) 7dp above the bottom, 10dp tab padding
 * - capsule style: 55dp bar (`mz_action_bar_tab_capsule_style_height`) with 8dp insets and a 20dp-radius
 *   theme-colour capsule behind the selected tab (`mz_action_bar_tab_indicator_color_blue`)
 */
open class FlymeTabBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.flymexTabBarStyle,
) : HorizontalScrollView(context, attrs, defStyleAttr) {

    enum class Style { UNDERLINE, CAPSULE }

    fun interface OnTabSelectedListener {
        fun onTabSelected(index: Int, reselected: Boolean)
    }

    private val strip = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private var indicatorColor: Int
    private var indicatorHeight: Float
    private var indicatorWidth: Float
    private var textSelected: Int
    private var textNormal: Int
    private var textSize: Float
    private var indicatorPos = 0f   // fractional tab index
    private var animator: ValueAnimator? = null
    private var listener: OnTabSelectedListener? = null

    var style: Style = Style.UNDERLINE
        set(value) {
            field = value
            applyStyle()
        }

    var selectedIndex: Int = -1
        private set

    val tabCount: Int get() = strip.childCount

    init {
        isHorizontalScrollBarEnabled = false
        isFillViewport = true
        overScrollMode = OVER_SCROLL_NEVER
        setWillNotDraw(false)
        addView(strip, ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT))

        val a = context.obtainStyledAttributes(attrs, R.styleable.FlymeTabBar, defStyleAttr, 0)
        indicatorColor = a.getColor(R.styleable.FlymeTabBar_mzTabBarIndicatorColor, FlymeTheme.primary(context))
        indicatorHeight = a.getDimension(R.styleable.FlymeTabBar_mzTabBarIndicatorHeight, resources.getDimension(R.dimen.mz_action_bar_tab_indicator_height))
        indicatorWidth = a.getDimension(R.styleable.FlymeTabBar_mzTabBarIndicatorWidth, resources.getDimension(R.dimen.mz_action_bar_tab_indicator_width))
        textSelected = a.getColor(R.styleable.FlymeTabBar_mzTabTextSelectedColor, FlymeTheme.primary(context))
        textNormal = a.getColor(R.styleable.FlymeTabBar_mzTabTextColor, FlymeTheme.onSurface(context))
        textSize = a.getDimension(R.styleable.FlymeTabBar_mzTabTextSize, resources.getDimension(R.dimen.mz_text_size_h8))
        val s = a.getInt(R.styleable.FlymeTabBar_mzTabStyle, 0)
        val entries = a.getTextArray(R.styleable.FlymeTabBar_mzTabEntries)
        a.recycle()
        style = if (s == 1) Style.CAPSULE else Style.UNDERLINE
        if (entries != null) setTabs(entries.toList())
    }

    fun setOnTabSelectedListener(l: OnTabSelectedListener?) {
        listener = l
    }

    fun setTabs(titles: List<CharSequence>, selected: Int = 0) {
        strip.removeAllViews()
        titles.forEachIndexed { i, t ->
            val tv = TextView(context).apply {
                text = t
                setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                gravity = Gravity.CENTER
                includeFontPadding = false
                maxLines = 1
                setOnClickListener { select(i, user = true) }
                isClickable = true
                isFocusable = true
            }
            strip.addView(tv, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
        applyStyle()
        selectedIndex = -1
        select(selected.coerceIn(0, titles.lastIndex), user = false, animate = false)
    }

    fun getTabView(index: Int): TextView? = strip.getChildAt(index) as? TextView

    private fun applyStyle() {
        val padH = if (style == Style.CAPSULE) resources.getDimensionPixelSize(R.dimen.mz_tool_bar_tab_view_padding_left_large) else resources.getDimensionPixelSize(R.dimen.mz_tool_bar_tab_view_padding_left_small)
        for (i in 0 until strip.childCount) {
            val tv = strip.getChildAt(i) as TextView
            tv.setPadding(padH, 0, padH, 0)
            if (style == Style.CAPSULE) {
                tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.mz_text_size_medium))
                tv.minWidth = context.dpInt(72f)
            } else {
                tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
                tv.minWidth = resources.getDimensionPixelSize(R.dimen.mz_action_bar_tab_view_mini_width)
            }
        }
        val h = if (style == Style.CAPSULE) resources.getDimensionPixelSize(R.dimen.mz_action_bar_tab_capsule_style_height) else resources.getDimensionPixelSize(R.dimen.mz_tab_height)
        minimumHeight = h
        updateColors()
        requestLayout()
        invalidate()
    }

    private fun updateColors() {
        for (i in 0 until strip.childCount) {
            val tv = strip.getChildAt(i) as TextView
            val sel = i == selectedIndex
            tv.setTextColor(
                if (style == Style.CAPSULE) (if (sel) FlymeTheme.onPrimary(context) else textNormal)
                else (if (sel) textSelected else textNormal),
            )
            tv.alpha = if (isEnabled) 1f else 0.2f
        }
    }

    fun select(index: Int, user: Boolean = false, animate: Boolean = true) {
        if (index < 0 || index >= strip.childCount) return
        val reselected = index == selectedIndex
        selectedIndex = index
        updateColors()
        animator?.cancel()
        if (animate && isLaidOut) {
            animator = ValueAnimator.ofFloat(indicatorPos, index.toFloat()).apply {
                duration = 200
                interpolator = FlymeSwitch.CHECK_INTERPOLATOR
                addUpdateListener { indicatorPos = it.animatedValue as Float; invalidate() }
                start()
            }
        } else {
            indicatorPos = index.toFloat()
            invalidate()
        }
        scrollToTab(index)
        if (user || !reselected) listener?.onTabSelected(index, reselected)
    }

    /** Sync the indicator with a pager: `position` + `offset` in 0..1. */
    fun setScrollPosition(position: Int, offset: Float) {
        animator?.cancel()
        indicatorPos = position + offset
        invalidate()
    }

    private fun scrollToTab(index: Int) {
        val v = strip.getChildAt(index) ?: return
        post { smoothScrollTo(v.left - (width - v.width) / 2, 0) }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val h = resolveSize(minimumHeight, heightMeasureSpec)
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY))
    }

    override fun dispatchDraw(canvas: Canvas) {
        if (style == Style.CAPSULE) drawCapsule(canvas)
        super.dispatchDraw(canvas)
        if (style == Style.UNDERLINE) drawUnderline(canvas)
    }

    private fun tabCenter(pos: Float): Pair<Float, Float> {
        val i = pos.toInt().coerceIn(0, strip.childCount - 1)
        val j = (i + 1).coerceAtMost(strip.childCount - 1)
        val f = pos - i
        val a = strip.getChildAt(i)
        val b = strip.getChildAt(j)
        val left = a.left + (b.left - a.left) * f
        val w = a.width + (b.width - a.width) * f
        return Pair(left + strip.left.toFloat(), w)
    }

    private fun drawUnderline(canvas: Canvas) {
        if (strip.childCount == 0) return
        val (left, w) = tabCenter(indicatorPos)
        val cx = left + w / 2f
        val bottom = height - resources.getDimension(R.dimen.mz_action_bar_tab_indicator_padding_bottom)
        rect.set(cx - indicatorWidth / 2f, bottom - indicatorHeight, cx + indicatorWidth / 2f, bottom)
        paint.color = indicatorColor
        canvas.drawRoundRect(rect, indicatorHeight / 2f, indicatorHeight / 2f, paint)
    }

    private fun drawCapsule(canvas: Canvas) {
        if (strip.childCount == 0) return
        val (left, w) = tabCenter(indicatorPos)
        val inset = resources.getDimension(R.dimen.mz_action_bar_tab_capsule_style_top)
        rect.set(left, inset, left + w, height - inset)
        val r = resources.getDimension(R.dimen.fd_sys_radius_corner_smooth_large)
        paint.color = indicatorColor
        canvas.drawRoundRect(rect, r, r, paint)
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        updateColors()
    }

    /** Small red dot (`mz_tab_view_dot_color`, 6dp) on a tab, e.g. for unread hints. */
    fun setTabDot(index: Int, show: Boolean) {
        val tv = getTabView(index) ?: return
        if (show) {
            val dot = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(context.getColor(R.color.mz_tab_view_dot_color))
                val s = resources.getDimensionPixelSize(R.dimen.mz_tool_bar_tab_view_dot_width_height)
                setSize(s, s)
            }
            tv.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, dot, null)
            tv.compoundDrawablePadding = context.dpInt(2f)
        } else {
            tv.setCompoundDrawablesRelative(null, null, null, null)
        }
    }

    @Suppress("unused")
    private fun colorList(c: Int) = ColorStateList.valueOf(c)

    override fun onInterceptTouchEvent(ev: android.view.MotionEvent): Boolean = strip.width > width && super.onInterceptTouchEvent(ev)

    override fun getAccessibilityClassName(): CharSequence = "android.widget.TabWidget"

    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        if (child === strip) super.addView(child, index, params) else strip.addView(child, index, params)
    }
}
