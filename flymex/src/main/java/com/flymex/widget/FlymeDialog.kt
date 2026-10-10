package com.flymex.widget

import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatDialog
import androidx.core.view.isVisible
import com.flymex.R

/**
 * Flyme alert dialog (`flyme.support.v7.app.AlertDialog` / `FlymeAlertDialogLayout`).
 *
 * - 340dp wide, 16dp corners on `?colorSurfaceContainerLowest` (`mz_dialog_background_drawable`)
 * - title 19sp medium, message 15sp `?colorOnSurfaceVariant` with 5dp line spacing
 * - buttons: 48dp rounded (`mz_alert_dialog_button_bg_radius`), 17sp medium, positive in the
 *   brand colour, delete in red; stacked vertically (58dp) when there are three or long labels
 * - enters with `mz_dialog_alert_anim_enter` (250ms) and leaves with `mz_dialog_alert_anim_exit`
 * - `showAtBottom` gives the Flyme "ShowAtBottom" sheet variant (slide in, 18dp above the bottom)
 */
open class FlymeDialog(context: Context, theme: Int = 0) : AppCompatDialog(context, resolveTheme(context, theme)) {

    class Builder(private val context: Context) {
        private val d = FlymeDialog(context)
        fun setTitle(t: CharSequence?) = apply { d.setTitle(t) }
        fun setTitle(res: Int) = apply { d.setTitle(context.getText(res)) }
        fun setMessage(m: CharSequence?) = apply { d.message = m }
        fun setMessage(res: Int) = apply { d.message = context.getText(res) }
        fun setIcon(icon: Drawable?) = apply { d.icon = icon }
        fun setView(v: View?) = apply { d.customView = v }
        fun setPositiveButton(text: CharSequence, l: ((DialogInterface) -> Unit)? = null) = apply { d.setButton(BUTTON_POSITIVE, text, l) }
        fun setNegativeButton(text: CharSequence, l: ((DialogInterface) -> Unit)? = null) = apply { d.setButton(BUTTON_NEGATIVE, text, l) }
        fun setNeutralButton(text: CharSequence, l: ((DialogInterface) -> Unit)? = null) = apply { d.setButton(BUTTON_NEUTRAL, text, l) }
        /** Red "delete" styling for the positive button (`mz_alert_dialog_delete_button_color`). */
        fun setDestructive(destructive: Boolean) = apply { d.destructive = destructive }
        fun setItems(items: Array<CharSequence>, l: (DialogInterface, Int) -> Unit) = apply { d.setItems(items, -1, false, l) }
        fun setSingleChoiceItems(items: Array<CharSequence>, checked: Int, l: (DialogInterface, Int) -> Unit) = apply { d.setItems(items, checked, true, l) }
        fun setCancelable(c: Boolean) = apply { d.setCancelable(c) }
        fun setShowAtBottom(b: Boolean) = apply { d.showAtBottom = b }
        fun setOnDismissListener(l: DialogInterface.OnDismissListener?) = apply { d.setOnDismissListener(l) }
        fun create(): FlymeDialog = d
        fun show(): FlymeDialog = d.also { it.show() }
    }

    private lateinit var root: LinearLayout
    private lateinit var titleTemplate: View
    private lateinit var iconView: ImageView
    private lateinit var titleView: TextView
    private lateinit var scroll: ScrollView
    private lateinit var messageView: TextView
    private lateinit var customFrame: FrameLayout
    private lateinit var listView: ListView
    private lateinit var buttonBar: LinearLayout
    private var inflated = false

    private var titleText: CharSequence? = null
    var message: CharSequence? = null
        set(v) { field = v; if (inflated) apply() }
    var icon: Drawable? = null
        set(v) { field = v; if (inflated) apply() }
    var customView: View? = null
        set(v) { field = v; if (inflated) apply() }
    var destructive: Boolean = false
        set(v) { field = v; if (inflated) apply() }
    var showAtBottom: Boolean = false
        set(v) { field = v; if (inflated) applyWindow() }

    private data class Btn(val text: CharSequence, val listener: ((DialogInterface) -> Unit)?)
    private val buttons = HashMap<Int, Btn>()
    private var items: Array<CharSequence>? = null
    private var checkedItem = -1
    private var singleChoice = false
    private var itemListener: ((DialogInterface, Int) -> Unit)? = null

    override fun setTitle(title: CharSequence?) {
        titleText = title
        if (inflated) apply()
    }

    fun setButton(which: Int, text: CharSequence, l: ((DialogInterface) -> Unit)?) {
        buttons[which] = Btn(text, l)
        if (inflated) apply()
    }

    fun setItems(items: Array<CharSequence>, checked: Int, single: Boolean, l: (DialogInterface, Int) -> Unit) {
        this.items = items
        checkedItem = checked
        singleChoice = single
        itemListener = l
        if (inflated) apply()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportRequestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        root = LayoutInflater.from(context).inflate(R.layout.flymex_dialog, null) as LinearLayout
        titleTemplate = root.findViewById(R.id.flymex_title_template)
        iconView = root.findViewById(R.id.flymex_icon)
        titleView = root.findViewById(R.id.flymex_title)
        scroll = root.findViewById(R.id.flymex_scroll)
        messageView = root.findViewById(R.id.flymex_message)
        customFrame = root.findViewById(R.id.flymex_custom)
        listView = root.findViewById(R.id.flymex_list)
        buttonBar = root.findViewById(R.id.flymex_button_bar)
        setContentView(root)
        inflated = true
        apply()
        applyWindow()
    }

    private fun applyWindow() {
        val w = window ?: return
        val res = context.resources
        if (showAtBottom) {
            w.setGravity(Gravity.BOTTOM)
            w.setWindowAnimations(R.style.Animation_Flymex_Sheet)
            val lp = w.attributes
            lp.y = res.getDimensionPixelSize(R.dimen.mz_alert_dialog_margin_bottom_to_screen)
            lp.width = WindowManager.LayoutParams.MATCH_PARENT
            w.attributes = lp
            w.setBackgroundDrawableResource(R.drawable.mz_dialog_background_drawable)
            val margin = res.getDimensionPixelSize(R.dimen.mz_dialog_custom_view_margin_horizontal)
            w.decorView.setPadding(margin, 0, margin, 0)
        } else {
            w.setGravity(Gravity.CENTER)
            w.setWindowAnimations(R.style.Animation_Flymex_Dialog)
            w.setLayout(res.getDimensionPixelSize(R.dimen.mz_alert_dialog_width), WindowManager.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun apply() {
        val hasTitle = !titleText.isNullOrEmpty()
        titleTemplate.isVisible = hasTitle
        titleView.text = titleText
        iconView.setImageDrawable(icon)
        iconView.isVisible = icon != null
        val hasMessage = !message.isNullOrEmpty()
        scroll.isVisible = hasMessage
        messageView.text = message
        if (!hasTitle) messageView.setPadding(messageView.paddingLeft, context.resources.getDimensionPixelSize(R.dimen.mz_alertDialog_content_margin_top), messageView.paddingRight, messageView.paddingBottom)

        customFrame.removeAllViews()
        customView?.let { v ->
            (v.parent as? ViewGroup)?.removeView(v)
            customFrame.addView(v)
        }
        customFrame.isVisible = customView != null

        val list = items
        if (list != null) {
            listView.isVisible = true
            listView.adapter = ArrayAdapter(context, R.layout.flymex_dialog_item, android.R.id.text1, list)
            listView.choiceMode = if (singleChoice) ListView.CHOICE_MODE_SINGLE else ListView.CHOICE_MODE_NONE
            if (singleChoice && checkedItem >= 0) listView.setItemChecked(checkedItem, true)
            listView.setOnItemClickListener { _, _, pos, _ ->
                itemListener?.invoke(this, pos)
                if (!singleChoice) dismiss()
            }
            val pad = context.resources.getDimensionPixelSize(if (hasTitle) R.dimen.mz_alert_dialog_listview_no_message_padding_top else R.dimen.mz_alert_dialog_listview_no_message_no_title_padding_top)
            listView.setPadding(listView.paddingLeft, if (hasMessage) 0 else pad, listView.paddingRight, if (buttons.isEmpty()) context.resources.getDimensionPixelSize(R.dimen.mz_alert_dialog_normal_margin_start) else 0)
        } else {
            listView.isVisible = false
        }

        buildButtons()
    }

    private fun buildButtons() {
        buttonBar.removeAllViews()
        if (buttons.isEmpty()) {
            buttonBar.isVisible = false
            return
        }
        buttonBar.isVisible = true
        val res = context.resources
        val order = listOf(BUTTON_NEGATIVE, BUTTON_NEUTRAL, BUTTON_POSITIVE).filter { buttons.containsKey(it) }
        val maxLen = res.getDimensionPixelSize(R.dimen.mz_alert_dialog_button_max_text_length)
        val probe = FlymeButton(context).apply { setTextAppearance(R.style.TextAppearance_Flymex_Body_Large) }
        val vertical = order.size >= 3 || order.any { probe.paint.measureText(buttons[it]!!.text.toString()) > maxLen }
        buttonBar.orientation = if (vertical) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
        val gap = res.getDimensionPixelSize(R.dimen.mz_alert_dialog_button_margin_start)
        val hH = res.getDimensionPixelSize(R.dimen.mz_alert_dialog_horizontal_button_height)
        val vH = res.getDimensionPixelSize(R.dimen.mz_alert_dialog_vertical_button_height)
        val seq = if (vertical) order.reversed() else order
        seq.forEachIndexed { i, which ->
            val b = buttons[which]!!
            val btn = FlymeButton(context).apply {
                text = b.text
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 17f)
                typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
                isAllCaps = false
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                minWidth = 0
                minimumWidth = 0
                minHeight = 0
                minimumHeight = 0
                setPadding(res.getDimensionPixelSize(R.dimen.mz_alert_dialog_button_padding), 0, res.getDimensionPixelSize(R.dimen.mz_alert_dialog_button_padding), 0)
                stateListAnimator = null
                setBackgroundResource(R.drawable.mz_alert_dialog_button_bg_radius)
                val color = when {
                    which == BUTTON_POSITIVE && destructive -> context.getColorStateList(R.color.mz_alert_dialog_delete_button_color)
                    which == BUTTON_POSITIVE -> context.getColorStateList(R.color.mz_button_positive_text_default)
                    else -> com.flymex.theme.FlymeTheme.colorStateList(context, R.attr.colorOnSurface)
                }
                setTextColor(color)
                setOnClickListener {
                    b.listener?.invoke(this@FlymeDialog)
                    dismiss()
                }
            }
            val lp = if (vertical) LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, vH).apply { if (i > 0) topMargin = gap }
            else LinearLayout.LayoutParams(0, hH, 1f).apply { if (i > 0) marginStart = gap }
            buttonBar.addView(btn, lp)
        }
    }

    companion object {
        const val BUTTON_POSITIVE = DialogInterface.BUTTON_POSITIVE
        const val BUTTON_NEGATIVE = DialogInterface.BUTTON_NEGATIVE
        const val BUTTON_NEUTRAL = DialogInterface.BUTTON_NEUTRAL

        private fun resolveTheme(context: Context, theme: Int): Int {
            if (theme != 0) return theme
            val tv = android.util.TypedValue()
            return if (context.theme.resolveAttribute(R.attr.flymexDialogTheme, tv, true)) tv.resourceId else R.style.ThemeOverlay_Flymex_Dialog
        }
    }
}
