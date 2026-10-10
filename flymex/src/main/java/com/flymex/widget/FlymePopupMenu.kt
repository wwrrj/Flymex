package com.flymex.widget

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.ListPopupWindow
import androidx.core.view.isVisible
import com.flymex.R
import com.flymex.theme.FlymeTheme
import com.flymex.theme.dpInt

/**
 * Flyme popup menu (`Widget.Flyme.Light.ListPopupWindow`, `Theme.Flyme.Light.CompactMenu`).
 *
 * Background `mz_popup_bg_light` (`?colorSurfaceContainerLowest`, 16dp corners, 0.5dp
 * `?colorOutlineVariant` stroke); rows 48dp (`mz_popup_menu_item_height`), 21dp padding,
 * 232–280dp wide (`mz_popup_menu_item_min_width` / `mz_popup_menu_width_max`); enters with
 * `mz_overflow_popup_enter` (alpha 100ms + scale 0.965→1 in 150ms from the top-end corner).
 */
class FlymePopupMenu(private val anchor: View) {

    data class Item(val id: Int, val title: CharSequence, val icon: Drawable? = null, val enabled: Boolean = true)

    private val context: Context = anchor.context
    private val items = ArrayList<Item>()
    private val popup = ListPopupWindow(context, null, R.attr.flymexPopupMenuStyle, R.style.Widget_Flymex_ListPopupWindow)
    private var listener: ((Item) -> Unit)? = null

    fun addItem(id: Int, title: CharSequence, icon: Drawable? = null, enabled: Boolean = true) = apply { items += Item(id, title, icon, enabled) }
    fun addItem(title: CharSequence, onClick: () -> Unit) = apply {
        val id = items.size
        items += Item(id, title)
        val prev = listener
        listener = { if (it.id == id) onClick() else prev?.invoke(it) }
    }
    fun setOnItemClickListener(l: (Item) -> Unit) = apply { listener = l }

    fun show() {
        val res = context.resources
        popup.anchorView = anchor
        popup.setBackgroundDrawable(context.getDrawable(R.drawable.mz_popup_bg_light))
        popup.setAnimationStyle(R.style.Animation_Flymex_Popup)
        popup.isModal = true
        popup.verticalOffset = context.dpInt(4f)
        val maxW = res.getDimensionPixelSize(R.dimen.mz_popup_menu_width_max)
        val minW = res.getDimensionPixelSize(R.dimen.mz_popup_menu_item_min_width)
        popup.width = measureWidth().coerceIn(minW, maxW)
        popup.setAdapter(Adapter())
        popup.setOnItemClickListener { _, _, pos, _ ->
            val item = items[pos]
            if (item.enabled) {
                listener?.invoke(item)
                popup.dismiss()
            }
        }
        popup.show()
        popup.listView?.apply {
            divider = null
            dividerHeight = 0
            val p = res.getDimensionPixelSize(R.dimen.mz_popup_menu_item_padding_ext)
            setPadding(p, p, p, p)
            clipToPadding = false
        }
    }

    fun dismiss() = popup.dismiss()

    private fun measureWidth(): Int {
        val paint = android.graphics.Paint().apply {
            textSize = context.resources.getDimension(R.dimen.mz_text_size_normal)
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        val padding = context.resources.getDimensionPixelSize(R.dimen.mz_popup_list_item_content_padding_left) * 2 + context.resources.getDimensionPixelSize(R.dimen.mz_popup_menu_item_padding_ext) * 2
        val icon = if (items.any { it.icon != null }) context.dpInt(36f) else 0
        return items.maxOfOrNull { paint.measureText(it.title.toString()).toInt() }?.plus(padding + icon) ?: 0
    }

    private inner class Adapter : BaseAdapter() {
        override fun getCount() = items.size
        override fun getItem(position: Int) = items[position]
        override fun getItemId(position: Int) = items[position].id.toLong()
        override fun isEnabled(position: Int) = items[position].enabled

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val row = (convertView as? LinearLayout) ?: LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                minimumHeight = context.resources.getDimensionPixelSize(R.dimen.mz_popup_menu_item_height)
                val p = context.resources.getDimensionPixelSize(R.dimen.mz_popup_list_item_content_padding_left) - context.resources.getDimensionPixelSize(R.dimen.mz_popup_menu_item_padding_ext)
                setPadding(p, 0, p, 0)
                setBackgroundResource(R.drawable.mz_alert_dialog_listview_item_bg_radius)
                addView(ImageView(context).apply { id = android.R.id.icon }, LinearLayout.LayoutParams(context.dpInt(24f), context.dpInt(24f)).apply { marginEnd = context.dpInt(12f) })
                addView(TextView(context).apply {
                    id = android.R.id.text1
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, context.resources.getDimension(R.dimen.mz_text_size_normal))
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            }
            val item = items[position]
            val icon = row.findViewById<ImageView>(android.R.id.icon)
            icon.setImageDrawable(item.icon)
            icon.isVisible = item.icon != null
            val text = row.findViewById<TextView>(android.R.id.text1)
            text.text = item.title
            text.setTextColor(if (item.enabled) FlymeTheme.onSurface(context) else FlymeTheme.stateDisabled(context))
            return row
        }
    }
}
