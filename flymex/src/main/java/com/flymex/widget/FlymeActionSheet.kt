package com.flymex.widget

import android.content.Context
import android.content.DialogInterface
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.flymex.R
import com.flymex.theme.FlymeTheme
import com.flymex.theme.dpInt

/**
 * Flyme action sheet — the "ShowAtBottom" dialog (`Flyme.AlertDialog.AppCompat.Light.ShowAtBottom`,
 * `Animation.Flyme.Dialog.ShowAtBottom`): a card 18dp above the screen bottom that slides up in
 * 304ms (`bottom_dialog_anim_duration`) with the `mz_showat_bottom_dialog_in_interpolator_` curve.
 *
 * Rows are 48dp (`mz_popup_menu_item_height`) with 21dp side padding; destructive rows use
 * `mz_alert_dialog_delete_button_color`; the cancel row is a full-width `Widget.Flymex.Button.Light`.
 */
class FlymeActionSheet private constructor(private val dialog: FlymeDialog) {

    class Builder(private val context: Context) {
        private var title: CharSequence? = null
        private var message: CharSequence? = null
        private val items = ArrayList<Item>()
        private var cancelText: CharSequence? = "取消"
        private var cancelable = true

        private data class Item(val text: CharSequence, val destructive: Boolean, val onClick: (() -> Unit)?)

        fun setTitle(t: CharSequence?) = apply { title = t }
        fun setMessage(m: CharSequence?) = apply { message = m }
        fun addItem(text: CharSequence, destructive: Boolean = false, onClick: (() -> Unit)? = null) = apply { items += Item(text, destructive, onClick) }
        fun setCancelText(t: CharSequence?) = apply { cancelText = t }
        fun setCancelable(c: Boolean) = apply { cancelable = c }

        fun create(): FlymeActionSheet {
            val d = FlymeDialog(context)
            d.showAtBottom = true
            d.setTitle(title)
            d.message = message
            d.setCancelable(cancelable)
            val column = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
            val rowH = context.resources.getDimensionPixelSize(R.dimen.mz_popup_menu_item_height)
            val padH = context.resources.getDimensionPixelSize(R.dimen.mz_popup_list_item_content_padding_left) - context.resources.getDimensionPixelSize(R.dimen.mz_dialog_custom_view_margin_horizontal)
            items.forEach { item ->
                val tv = TextView(context).apply {
                    text = item.text
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, context.resources.getDimension(R.dimen.mz_text_size_normal))
                    typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                    setTextColor(if (item.destructive) context.getColor(R.color.mz_alert_dialog_delete_button_color) else FlymeTheme.onSurface(context))
                    gravity = Gravity.CENTER_VERTICAL
                    minHeight = rowH
                    setPadding(padH.coerceAtLeast(0), 0, padH.coerceAtLeast(0), 0)
                    setBackgroundResource(R.drawable.mz_alert_dialog_listview_item_bg_radius)
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        item.onClick?.invoke()
                        d.dismiss()
                    }
                }
                column.addView(tv, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            }
            val cancel = cancelText
            if (!cancel.isNullOrEmpty()) {
                val b = FlymeButton(context).apply {
                    variant = FlymeButton.Variant.LIGHT
                    text = cancel
                    setOnClickListener { d.cancel() }
                }
                column.addView(b, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, context.resources.getDimensionPixelSize(R.dimen.mz_alert_dialog_horizontal_button_height)).apply {
                    topMargin = context.dpInt(12f)
                    val m = context.resources.getDimensionPixelSize(R.dimen.mz_alert_dialog_normal_margin_start) - context.resources.getDimensionPixelSize(R.dimen.mz_dialog_custom_view_margin_horizontal)
                    marginStart = m.coerceAtLeast(0); marginEnd = m.coerceAtLeast(0); bottomMargin = m.coerceAtLeast(0)
                })
            }
            column.isVisible = column.childCount > 0
            column.setPadding(0, if (title.isNullOrEmpty() && message.isNullOrEmpty()) context.dpInt(12f) else 0, 0, 0)
            d.customView = column
            return FlymeActionSheet(d)
        }

        fun show(): FlymeActionSheet = create().also { it.show() }
    }

    fun show() = dialog.show()
    fun dismiss() = dialog.dismiss()
    fun setOnDismissListener(l: DialogInterface.OnDismissListener?) = dialog.setOnDismissListener(l)
    val isShowing: Boolean get() = dialog.isShowing
}
