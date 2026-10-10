package com.flymex.widget

import android.content.Context
import android.content.res.ColorStateList
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.view.isVisible
import com.flymex.R
import com.flymex.theme.FlymeTheme
import com.flymex.theme.dpInt

/**
 * Flyme search bar (`mz_titlebar_search_view` / `mzSearchEditTextStyle`).
 *
 * 64dp row (`mz_action_bar_search_layout_height`); the field uses
 * `mz_titlebar_search_view_edittext_bg` (#f4f4f5 / night #222227, `?cornerMini` 8dp),
 * 16sp text, hint `mz_action_bar_search_text_color_hint_white` (#58000000), a leading search
 * glyph, a trailing clear button and an optional "取消" text action in the brand colour.
 */
open class FlymeSearchBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.flymexSearchBarStyle,
) : LinearLayout(context, attrs, defStyleAttr) {

    val editText: AppCompatEditText
    val clearButton: ImageButton
    val cancelButton: FlymeButton
    private val fieldView: LinearLayout

    var onQueryChange: ((String) -> Unit)? = null
    var onSearch: ((String) -> Unit)? = null
    var onCancel: (() -> Unit)? = null

    var showCancel: Boolean
        get() = cancelButton.isVisible
        set(value) {
            cancelButton.isVisible = value
        }

    var immersive: Boolean = false
        set(value) {
            field = value
            fieldView.setBackgroundResource(if (value) R.drawable.mz_titlebar_search_view_edittext_bg_immersive else R.drawable.mz_titlebar_search_view_edittext_bg)
        }

    var query: String
        get() = editText.text?.toString().orEmpty()
        set(value) {
            editText.setText(value)
            editText.setSelection(value.length)
        }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val padH = resources.getDimensionPixelSize(R.dimen.mz_page_padding_default)
        setPadding(padH, 0, padH, 0)
        minimumHeight = resources.getDimensionPixelSize(R.dimen.mz_action_bar_search_layout_height)

        fieldView = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.mz_titlebar_search_view_edittext_bg)
            setPadding(context.dpInt(12f), 0, context.dpInt(6f), 0)
        }
        val icon = ImageView(context).apply {
            setImageResource(R.drawable.mz_titlebar_ic_search)
            imageTintList = ColorStateList.valueOf(context.getColor(R.color.mz_action_bar_search_text_color_hint_white))
        }
        fieldView.addView(icon, LayoutParams(context.dpInt(20f), context.dpInt(20f)).apply { marginEnd = context.dpInt(8f) })
        editText = AppCompatEditText(context).apply {
            background = null
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(FlymeTheme.onSurface(context))
            setHintTextColor(context.getColor(R.color.mz_action_bar_search_text_color_hint_white))
            maxLines = 1
            isSingleLine = true
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            setPadding(0, 0, 0, 0)
            includeFontPadding = false
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                override fun afterTextChanged(s: Editable?) {
                    clearButton.isVisible = !s.isNullOrEmpty()
                    onQueryChange?.invoke(s?.toString().orEmpty())
                }
            })
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    onSearch?.invoke(query)
                    hideKeyboard()
                    true
                } else false
            }
        }
        fieldView.addView(editText, LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        clearButton = ImageButton(context).apply {
            setImageResource(R.drawable.mz_btn_textfield_delete)
            background = null
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            contentDescription = "清除"
            isVisible = false
            setOnClickListener { editText.setText("") }
        }
        fieldView.addView(clearButton, LayoutParams(context.dpInt(32f), context.dpInt(32f)))
        addView(fieldView, LayoutParams(0, context.dpInt(40f), 1f))

        cancelButton = FlymeButton(context).apply {
            variant = FlymeButton.Variant.TEXT
            text = "取消"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setOnClickListener {
                editText.setText("")
                hideKeyboard()
                onCancel?.invoke()
            }
        }
        addView(cancelButton, LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginStart = context.dpInt(4f) })

        val a = context.obtainStyledAttributes(attrs, R.styleable.FlymeSearchBar, defStyleAttr, 0)
        editText.hint = a.getText(R.styleable.FlymeSearchBar_android_hint) ?: "搜索"
        showCancel = a.getBoolean(R.styleable.FlymeSearchBar_mzShowCancel, true)
        immersive = a.getBoolean(R.styleable.FlymeSearchBar_mzImmersive, false)
        a.recycle()
    }

    fun hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(editText.windowToken, 0)
        editText.clearFocus()
    }
}
