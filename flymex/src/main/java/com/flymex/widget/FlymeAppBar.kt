package com.flymex.widget

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.RippleDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import com.flymex.R
import com.flymex.theme.FlymeTheme
import com.flymex.theme.dpInt

/**
 * Flyme title bar (`Widget.Flyme.AppCompat.ActionBar`, `MzBack` button).
 *
 * - 64dp tall (`mz_action_bar_default_height_appcompat`), 128dp when [expanded]
 *   (`mz_action_bar_default_height_appcompat_expanded`)
 * - back button 64dp wide (`mz_action_bar_back_button_width`) with `mz_ic_sb_back`
 * - title `TextAppearance.Flyme.HeadLine.Medium` (20sp medium); expanded title
 *   `TextAppearance.Flyme.AppCompat.Widget.ActionBar.Title.Expanded` (32sp medium) at the page padding
 * - actions are 34×44dp minimum (`mz_action_button_min_width/height`)
 */
open class FlymeAppBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.flymexAppBarStyle,
) : FrameLayout(context, attrs, defStyleAttr) {

    val backButton: ImageButton
    val titleView: TextView
    val subtitleView: TextView
    val expandedTitleView: TextView
    val actionsContainer: LinearLayout
    private val titleBlock: LinearLayout

    var title: CharSequence?
        get() = titleView.text
        set(value) {
            titleView.text = value
            expandedTitleView.text = value
            applyExpanded()
        }

    var subtitle: CharSequence?
        get() = subtitleView.text
        set(value) {
            subtitleView.text = value
            subtitleView.isVisible = !value.isNullOrEmpty()
        }

    var showBack: Boolean
        get() = backButton.isVisible
        set(value) {
            backButton.isVisible = value
            applyExpanded()
        }

    /** Large-title mode (Flyme's expanded action bar). */
    var expanded: Boolean = false
        set(value) {
            field = value
            applyExpanded()
        }

    var onBackClick: (() -> Unit)? = null

    init {
        val res = resources
        val barHeight = res.getDimensionPixelSize(R.dimen.mz_action_bar_default_height_appcompat)
        minimumHeight = barHeight

        backButton = ImageButton(context).apply {
            setImageResource(R.drawable.mz_ic_sb_back)
            imageTintList = ColorStateList.valueOf(FlymeTheme.onSurface(context))
            background = RippleDrawable(ColorStateList.valueOf(FlymeTheme.statePressed(context)), null, null).also { it.radius = context.dpInt(22f) }
            scaleType = android.widget.ImageView.ScaleType.CENTER
            contentDescription = "返回"
            setOnClickListener {
                val cb = onBackClick
                if (cb != null) cb() else (context as? androidx.activity.ComponentActivity)?.onBackPressedDispatcher?.onBackPressed()
            }
        }
        addView(backButton, LayoutParams(res.getDimensionPixelSize(R.dimen.mz_action_bar_back_button_width), barHeight, Gravity.START or Gravity.TOP))

        titleBlock = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL }
        titleView = TextView(context).apply {
            TextViewCompat.setTextAppearance(this, R.style.TextAppearance_Flymex_Headline_Medium)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            includeFontPadding = false
        }
        subtitleView = TextView(context).apply {
            TextViewCompat.setTextAppearance(this, R.style.TextAppearance_Flymex_Body_Mini)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            isVisible = false
        }
        titleBlock.addView(titleView)
        titleBlock.addView(subtitleView)
        addView(titleBlock, LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, barHeight, Gravity.START or Gravity.TOP))

        expandedTitleView = TextView(context).apply {
            TextViewCompat.setTextAppearance(this, R.style.TextAppearance_Flymex_Headline_Large)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            includeFontPadding = false
            isVisible = false
        }
        addView(expandedTitleView, LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.START or Gravity.BOTTOM).apply {
            marginStart = res.getDimensionPixelSize(R.dimen.mz_page_padding_default)
            bottomMargin = context.dpInt(14f)
        })

        actionsContainer = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        addView(actionsContainer, LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, barHeight, Gravity.END or Gravity.TOP).apply {
            marginEnd = res.getDimensionPixelSize(R.dimen.mz_action_bar_padding_right)
        })

        val a = context.obtainStyledAttributes(attrs, R.styleable.FlymeAppBar, defStyleAttr, 0)
        title = a.getText(R.styleable.FlymeAppBar_android_title)
        subtitle = a.getText(R.styleable.FlymeAppBar_android_subtitle)
        showBack = a.getBoolean(R.styleable.FlymeAppBar_mzShowBack, true)
        expanded = a.getBoolean(R.styleable.FlymeAppBar_mzExpanded, false)
        setBackgroundColor(a.getColor(R.styleable.FlymeAppBar_mzAppBarBackground, 0))
        a.recycle()
    }

    private fun applyExpanded() {
        val res = resources
        val barHeight = res.getDimensionPixelSize(R.dimen.mz_action_bar_default_height_appcompat)
        minimumHeight = if (expanded) res.getDimensionPixelSize(R.dimen.mz_action_bar_default_height_appcompat_expanded) else barHeight
        expandedTitleView.isVisible = expanded
        titleBlock.isVisible = !expanded
        (titleBlock.layoutParams as LayoutParams).marginStart = if (showBack) 0 else res.getDimensionPixelSize(R.dimen.mz_page_padding_default)
        requestLayout()
    }

    /** Add an icon action (34×44dp minimum, `mz_action_button_min_*`). */
    fun addAction(icon: Int, contentDescription: CharSequence, onClick: (View) -> Unit): ImageButton {
        val b = ImageButton(context).apply {
            setImageResource(icon)
            imageTintList = ColorStateList.valueOf(FlymeTheme.onSurface(context))
            background = RippleDrawable(ColorStateList.valueOf(FlymeTheme.statePressed(context)), null, null).also { it.radius = context.dpInt(22f) }
            scaleType = android.widget.ImageView.ScaleType.CENTER
            this.contentDescription = contentDescription
            minimumWidth = resources.getDimensionPixelSize(R.dimen.mz_action_button_min_width)
            minimumHeight = resources.getDimensionPixelSize(R.dimen.mz_action_button_min_height)
            setOnClickListener { onClick(it) }
        }
        actionsContainer.addView(b, LinearLayout.LayoutParams(context.dpInt(44f), context.dpInt(44f)).apply { marginStart = context.dpInt(4f) })
        return b
    }

    /** Add a text action in the brand colour (`mz_action_menu_textcolor_normal` style). */
    fun addTextAction(text: CharSequence, onClick: () -> Unit): TextView {
        val tv = FlymeButton(context).apply {
            variant = FlymeButton.Variant.TEXT
            this.text = text
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.mz_text_size_medium))
            setOnClickListener { onClick() }
        }
        actionsContainer.addView(tv, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, context.dpInt(44f)))
        return tv
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(resolveSize(minimumHeight, heightMeasureSpec), MeasureSpec.EXACTLY))
    }

    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        super.addView(child, index, params)
    }
}
