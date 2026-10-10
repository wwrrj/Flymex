package com.flymex.widget

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.RippleDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.Checkable
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import com.flymex.R
import com.flymex.theme.FlymeTheme
import com.flymex.theme.dpInt

/**
 * Flyme list cell — the row of Flyme's immersive preference list
 * (`layout/mz_preference_immersive.xml`, `Preference.Flyme.Light.Immersive`), verified against
 * Flyme 12.6 Settings on a meizu 21 Pro.
 *
 * Layout (dp): rows sit full-bleed and the card is drawn 16dp in (`ImmersiveListItemDrawable`
 * `immersiveItemPadding`), so the text is 32dp from the screen edge (`fd_sys_margin_extra_heavy`)
 * and 16dp from the card edge; vertical padding 16 (`mz_list_item_content_vertical_padding_level4`);
 * icon 46 with 7dp corners (`mz_list_icon_medium_*`, `mz_list_icon_item_icon_corner_radius`) and 16 gap;
 * title 17sp medium; summary 14sp with 4dp line spacing; widget frame 14dp in (`preference_item_padding_inner`),
 * min 43dp wide (`preference_widget_width`). Rows have no dividers; `mzShowDivider` enables the
 * classic 0.67dp `mz_preference.xml` divider.
 */
open class FlymeCell @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.flymexCellStyle,
) : LinearLayout(context, attrs, defStyleAttr), Checkable {

    enum class IconSize(val dimen: Int) {
        SMALL(R.dimen.mz_list_icon_small_width),
        NORMAL(R.dimen.mz_list_icon_normal_width),
        MEDIUM(R.dimen.mz_list_icon_medium_width),
        LARGE(R.dimen.mz_list_icon_large_width),
    }

    enum class Widget { NONE, ARROW, SWITCH, CHECKBOX, RADIO, TEXT, LOADING }

    val iconFrame: FlymeCardView
    val iconView: ImageView
    val titleView: TextView
    val summaryView: TextView
    val widgetFrame: FrameLayout
    private val textLayout: LinearLayout

    private var widgetView: View? = null
    private val dividerPaint = Paint().apply { color = FlymeTheme.color(context, R.attr.colorOutlineVariant) }
    private val dividerHeight = resources.getDimension(R.dimen.mz_list_item_divider_height).coerceAtLeast(1f)
    private val basePadV = resources.getDimensionPixelSize(R.dimen.mz_list_item_content_vertical_padding_level4)
    private val basePadH = resources.getDimensionPixelSize(R.dimen.fd_sys_margin_medium)

    /** Classic divider (`mz_preference.xml`); immersive lists (the Flyme 12 default) draw none. */
    var showDivider: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    /** Start the divider at the text column (after the icon) instead of the row edge. */
    var dividerInside: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    /** Extra top/bottom space for the first/last row of a card (`immersive_mz_preference_head/foot`: +8dp). */
    fun setEdgeInsets(top: Boolean, bottom: Boolean) {
        val extra = context.dpInt(8f)
        setPadding(paddingLeft, basePadV + (if (top) extra else 0), paddingRight, basePadV + (if (bottom) extra else 0))
    }

    var widget: Widget = Widget.NONE
        set(value) {
            if (field == value && widgetView != null) return
            field = value
            buildWidget(value)
        }

    var iconSize: IconSize = IconSize.MEDIUM
        set(value) {
            field = value
            val px = resources.getDimensionPixelSize(value.dimen)
            iconFrame.layoutParams = (iconFrame.layoutParams as LayoutParams).apply { width = px; height = px }
            iconFrame.requestLayout()
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

    /** Trailing value text for [Widget.TEXT]. */
    var value: CharSequence? = null
        set(v) {
            field = v
            (widgetView as? TextView)?.text = v
        }

    var icon: Drawable?
        get() = iconView.drawable
        set(value) {
            iconView.setImageDrawable(value)
            iconFrame.isVisible = value != null
        }

    /** Clip the icon to a circle (avatars). */
    var iconRound: Boolean = false
        set(value) {
            field = value
            iconFrame.cornerRadius = if (value) 9999f else resources.getDimension(R.dimen.mz_list_icon_item_icon_corner_radius)
        }

    private var checkedState = false
    var onCheckedChange: ((FlymeCell, Boolean) -> Unit)? = null

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(basePadH, basePadV, basePadH, basePadV)
        minimumHeight = resources.getDimensionPixelSize(R.dimen.mz_list_item_height_small)
        setWillNotDraw(false)

        iconFrame = FlymeCardView(context).apply {
            cornerRadius = resources.getDimension(R.dimen.mz_list_icon_item_icon_corner_radius)
            cardBackgroundColor = 0
            shadow = FlymeCardView.Shadow.NONE
            isVisible = false
        }
        iconView = ImageView(context).apply { scaleType = ImageView.ScaleType.CENTER_INSIDE }
        iconFrame.addView(iconView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        val iconPx = resources.getDimensionPixelSize(R.dimen.mz_list_icon_medium_width)
        addView(iconFrame, LayoutParams(iconPx, iconPx).apply { marginEnd = resources.getDimensionPixelSize(R.dimen.mz_preference_icon_margin_horizontal) })

        textLayout = LinearLayout(context).apply { orientation = VERTICAL; gravity = Gravity.CENTER_VERTICAL }
        titleView = TextView(context).apply {
            TextViewCompat.setTextAppearance(this, R.style.TextAppearance_Flymex_Cell_Title)
            ellipsize = android.text.TextUtils.TruncateAt.END
            isVisible = false
        }
        summaryView = TextView(context).apply {
            TextViewCompat.setTextAppearance(this, R.style.TextAppearance_Flymex_Cell_Summary)
            ellipsize = android.text.TextUtils.TruncateAt.END
            maxLines = 2
            setLineSpacing(context.dpInt(4f).toFloat(), 1f)
            isVisible = false
        }
        textLayout.addView(titleView, LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        textLayout.addView(summaryView, LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        addView(textLayout, LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        widgetFrame = FrameLayout(context).apply {
            minimumWidth = resources.getDimensionPixelSize(R.dimen.preference_widget_width)
            isVisible = false
        }
        addView(widgetFrame, LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            marginStart = resources.getDimensionPixelSize(R.dimen.preference_item_padding_inner)
        })

        val a = context.obtainStyledAttributes(attrs, R.styleable.FlymeCell, defStyleAttr, 0)
        title = a.getText(R.styleable.FlymeCell_android_title)
        summary = a.getText(R.styleable.FlymeCell_android_summary)
        icon = a.getDrawable(R.styleable.FlymeCell_android_icon)
        iconSize = IconSize.values()[a.getInt(R.styleable.FlymeCell_mzIconSize, 2).coerceIn(0, 3)]
        iconRound = a.getBoolean(R.styleable.FlymeCell_mzIconRound, false)
        showDivider = a.getBoolean(R.styleable.FlymeCell_mzShowDivider, false)
        dividerInside = a.getBoolean(R.styleable.FlymeCell_mzDividerInside, false)
        value = a.getText(R.styleable.FlymeCell_mzValue)
        a.getColorStateList(R.styleable.FlymeCell_mzTitleColor)?.let { titleView.setTextColor(it) }
        a.getColorStateList(R.styleable.FlymeCell_mzSummaryColor)?.let { summaryView.setTextColor(it) }
        checkedState = a.getBoolean(R.styleable.FlymeCell_android_checked, false)
        isEnabled = a.getBoolean(R.styleable.FlymeCell_android_enabled, true)
        widget = Widget.values()[a.getInt(R.styleable.FlymeCell_mzWidget, 0).coerceIn(0, 6)]
        a.recycle()

        // ImmersiveListItemDrawable tint: ?colorStatePressed washed over the row while pressed
        background = RippleDrawable(ColorStateList.valueOf(FlymeTheme.statePressed(context)), null, ColorDrawable(-0x1))
        isClickable = true
        isFocusable = true
    }

    private fun buildWidget(w: Widget) {
        widgetFrame.removeAllViews()
        widgetView = null
        val v: View? = when (w) {
            Widget.NONE -> null
            Widget.ARROW -> ImageView(context).apply {
                setImageResource(R.drawable.mz_arrow_next_right)
                imageTintList = ColorStateList.valueOf(FlymeTheme.onSurfaceVariant(context))
            }
            Widget.SWITCH -> FlymeSwitch(context).apply {
                isChecked = checkedState
                isFocusable = false
                isClickable = false
                setOnCheckedChangeListener { _, c -> setChecked(c) }
            }
            Widget.CHECKBOX -> FlymeCheckBox(context).apply { isChecked = checkedState; isFocusable = false; isClickable = false }
            Widget.RADIO -> FlymeRadioButton(context).apply { isChecked = checkedState; isFocusable = false; isClickable = false }
            Widget.TEXT -> TextView(context).apply {
                TextViewCompat.setTextAppearance(this, R.style.TextAppearance_Flymex_Cell_Summary)
                text = value
            }
            Widget.LOADING -> FlymeLoadingView(context)
        }
        widgetView = v
        widgetFrame.isVisible = v != null
        if (v != null) {
            v.isEnabled = isEnabled
            widgetFrame.addView(v, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL or Gravity.END))
        }
    }

    /** Add any view (e.g. a FlymeButton) to the trailing slot. */
    fun setWidgetView(view: View?) {
        widget = Widget.NONE
        widgetFrame.removeAllViews()
        widgetView = view
        widgetFrame.isVisible = view != null
        if (view != null) widgetFrame.addView(view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL or Gravity.END))
    }

    override fun performClick(): Boolean {
        when (widget) {
            Widget.SWITCH, Widget.CHECKBOX -> toggle()
            Widget.RADIO -> if (!checkedState) setChecked(true)
            else -> Unit
        }
        return super.performClick()
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        titleView.isEnabled = enabled
        summaryView.isEnabled = enabled
        widgetView?.isEnabled = enabled
        iconView.alpha = if (enabled) 1f else 0.4f
    }

    // Checkable
    override fun isChecked() = checkedState
    override fun setChecked(checked: Boolean) {
        if (checkedState == checked) return
        checkedState = checked
        when (val v = widgetView) {
            is FlymeSwitch -> if (v.isChecked != checked) v.isChecked = checked
            is Checkable -> if (v.isChecked != checked) v.isChecked = checked
            else -> Unit
        }
        refreshDrawableState()
        onCheckedChange?.invoke(this, checked)
    }
    override fun toggle() = setChecked(!checkedState)

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val state = super.onCreateDrawableState(extraSpace + 1)
        if (checkedState) mergeDrawableStates(state, intArrayOf(android.R.attr.state_checked))
        return state
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!showDivider) return
        val start = if (dividerInside) textLayout.left.toFloat() else paddingStart.toFloat()
        val end = (width - paddingEnd).toFloat()
        canvas.drawRect(start, height - dividerHeight, end, height.toFloat(), dividerPaint)
    }
}

/**
 * A Flyme immersive list card: rows inside a `?colorSurfaceContainerLowest` card with 10dp corners
 * (`fd_sys_radius_corner_smooth_small`), drawn 16dp in from the group edges (`immersiveItemPadding`),
 * 16dp below the previous group (`immersive_preference_divider`), with an optional category title
 * (`mz_preference_category_immersive.xml`: 13sp `?colorOnSurfaceVariant`, 18dp above, 8dp below, 32dp in).
 */
open class FlymeCellGroup @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.flymexCellGroupStyle,
) : LinearLayout(context, attrs, defStyleAttr) {

    val titleView: TextView
    val card: FlymeCardView
    val container: LinearLayout
    private val inset = resources.getDimensionPixelSize(R.dimen.fd_sys_margin_medium)

    var title: CharSequence?
        get() = titleView.text
        set(value) {
            titleView.text = value
            titleView.isVisible = !value.isNullOrEmpty()
        }

    var cardStyle: Boolean = true
        set(value) {
            field = value
            card.cardBackgroundColor = if (value) FlymeTheme.surfaceContainerLowest(context) else 0
            card.cornerRadius = if (value) FlymeTheme.dimension(context, R.attr.cornerSmall) else 0f
        }

    init {
        orientation = VERTICAL
        titleView = TextView(context).apply {
            TextViewCompat.setTextAppearance(this, R.style.TextAppearance_Flymex_Cell_Category)
            setPaddingRelative(inset * 2, context.dpInt(18f), context.dpInt(20f), context.dpInt(8f))
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            isVisible = false
        }
        super.addView(titleView, -1, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        card = FlymeCardView(context)
        container = LinearLayout(context).apply { orientation = VERTICAL }
        card.addView(container, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        super.addView(card, -1, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            marginStart = inset
            marginEnd = inset
            bottomMargin = inset
        })

        val a = context.obtainStyledAttributes(attrs, R.styleable.FlymeCellGroup, defStyleAttr, 0)
        title = a.getText(R.styleable.FlymeCellGroup_mzGroupTitle)
        cardStyle = a.getBoolean(R.styleable.FlymeCellGroup_mzGroupCardStyle, true)
        a.recycle()
        card.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, card.cornerRadius)
            }
        }
    }

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams?) {
        if (child === titleView || child === card) {
            super.addView(child, index, params)
            return
        }
        container.addView(child, index, params ?: LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        refreshEdges()
    }

    /** Re-apply head/foot insets and dividers after rows are added, removed or hidden. */
    fun refreshEdges() {
        val rows = (0 until container.childCount).map { container.getChildAt(it) }.filter { it.isVisible }
        rows.forEachIndexed { i, v ->
            if (v is FlymeCell) {
                v.setEdgeInsets(top = i == 0, bottom = i == rows.lastIndex)
                if (v.showDivider && i == rows.lastIndex) v.showDivider = false
            }
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (changed) refreshEdges()
        super.onLayout(changed, l, t, r, b)
    }
}
