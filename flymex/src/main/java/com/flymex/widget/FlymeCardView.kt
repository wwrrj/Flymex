package com.flymex.widget

import android.content.Context
import android.graphics.Outline
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import com.flymex.R
import com.flymex.theme.FlymeTheme

/**
 * Rounded-corner container — Flyme's `com.meizu.common.widget.SmoothCornerView` plus the
 * `Widget.Flyme.Shadow.Small/Medium/Large` elevation styles.
 *
 * - default radius `?cornerLarge` (20dp), background `?colorSurfaceContainerLowest`
 * - `mzShadowLevel`: medium = `fd_sys_shadow_elevation_medium` (40dp) with spot
 *   `fd_sys_color_shadow_spot_medium`; large = 60dp with `fd_sys_color_shadow_spot_large`
 */
open class FlymeCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.flymexCardViewStyle,
) : FrameLayout(context, attrs, defStyleAttr) {

    enum class Shadow { NONE, SMALL, MEDIUM, LARGE }

    private val shape = GradientDrawable()

    var cornerRadius: Float = 0f
        set(value) {
            field = value
            shape.cornerRadius = value
            invalidateOutline()
        }

    var cardBackgroundColor: Int = 0
        set(value) {
            field = value
            shape.setColor(value)
        }

    var shadow: Shadow = Shadow.NONE
        set(value) {
            field = value
            applyShadow(value)
        }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.FlymeCardView, defStyleAttr, 0)
        cornerRadius = a.getDimension(R.styleable.FlymeCardView_mzCornerRadius, FlymeTheme.dimension(context, R.attr.cornerLarge))
        cardBackgroundColor = a.getColor(R.styleable.FlymeCardView_mzCardBackgroundColor, FlymeTheme.surfaceContainerLowest(context))
        val level = a.getInt(R.styleable.FlymeCardView_mzShadowLevel, 0)
        a.recycle()
        background = shape
        clipToOutline = true
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, cornerRadius)
            }
        }
        shadow = Shadow.values()[level.coerceIn(0, 3)]
    }

    private fun applyShadow(level: Shadow) {
        val res = resources
        when (level) {
            Shadow.NONE -> elevation = 0f
            Shadow.SMALL -> {
                // Widget.Flyme.Shadow.Small: dy 8dp, blur 8dp, #28000000 — approximated with elevation
                elevation = res.getDimension(R.dimen.fd_sys_shadow_radius_small)
                setShadowColors(context.getColor(R.color.fd_sys_color_shadow_small), context.getColor(R.color.fd_sys_color_shadow_small))
            }
            Shadow.MEDIUM -> {
                elevation = res.getDimension(R.dimen.fd_sys_shadow_elevation_medium)
                setShadowColors(context.getColor(R.color.fd_sys_color_shadow_spot_medium), context.getColor(R.color.fd_sys_color_shadow_ambient_medium))
            }
            Shadow.LARGE -> {
                elevation = res.getDimension(R.dimen.fd_sys_shadow_elevation_large)
                setShadowColors(context.getColor(R.color.fd_sys_color_shadow_spot_large), context.getColor(R.color.fd_sys_color_shadow_ambient_large))
            }
        }
    }

    private fun setShadowColors(spot: Int, ambient: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            outlineSpotShadowColor = spot
            outlineAmbientShadowColor = ambient
        }
    }
}
