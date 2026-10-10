package com.flymex.widget

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.core.content.ContextCompat
import com.flymex.R

/**
 * Flyme corner button (`com.meizu.common.widget.MzButton`).
 *
 * Use a style for a fixed look, or [variant] to switch at runtime:
 * ```xml
 * <com.flymex.widget.FlymeButton style="@style/Widget.Flymex.Button.Tonal" … />
 * ```
 */
open class FlymeButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.flymexButtonStyle,
) : AppCompatButton(context, attrs, defStyleAttr) {

    enum class Variant(val background: Int, val textColor: Int) {
        /** `mz_btn_corner_default` — theme colour fill. */
        FILLED(R.drawable.mz_btn_corner_default, R.color.mz_button_corner_text_color_default),
        /** `mz_btn_corner_light_bg_state_drawable` — primary-container fill. */
        TONAL(R.drawable.mz_btn_corner_light_bg_state_drawable, R.color.mz_button_corner_text_color_light),
        /** `mz_btn_corner_normal_stroke` — 1dp theme-colour stroke. */
        OUTLINED(R.drawable.mz_btn_corner_normal_stroke, R.color.mz_button_corner_text_color_light),
        /** `mz_item_background_borderless` — text only. */
        TEXT(R.drawable.mz_item_background_borderless, R.color.mz_button_positive_text_default),
        /** `mz_btn_corner_bg_gray_smooth_regular` — surface-container-low fill. */
        LIGHT(R.drawable.mz_btn_corner_bg_gray_smooth_regular, 0),
        /** `mz_btn_corner_default_state_color` — 50dp pill used in bottom bars. */
        PILL(R.drawable.mz_btn_corner_default_state_color, R.color.mz_button_corner_text_color_default),
    }

    var variant: Variant? = null
        set(value) {
            field = value
            if (value != null) applyVariant(value)
        }

    private fun applyVariant(v: Variant) {
        setBackgroundResource(v.background)
        if (v.textColor != 0) {
            setTextColor(ContextCompat.getColorStateList(context, v.textColor))
        } else {
            setTextColor(com.flymex.theme.FlymeTheme.colorStateList(context, R.attr.colorOnSurface))
        }
    }
}

/** Flyme multi-choice check box (`Widget.Flyme.Light.CompoundButton.CheckBox`, `mz_btn_check_multiple`). */
open class FlymeCheckBox @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.flymexCheckBoxStyle,
) : AppCompatCheckBox(context, attrs, defStyleAttr)

/**
 * Flyme single-choice indicator. Flyme draws its radio state as the animated check mark
 * (`listChoiceIndicatorSingle` → `mz_btn_check_single`), not a ring and dot.
 */
open class FlymeRadioButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.flymexRadioButtonStyle,
) : AppCompatRadioButton(context, attrs, defStyleAttr)
