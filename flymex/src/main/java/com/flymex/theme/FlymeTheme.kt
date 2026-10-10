package com.flymex.theme

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.util.TypedValue
import android.view.ContextThemeWrapper
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.StyleRes
import com.flymex.R

/**
 * Flyme theme colours ("主题色"). Flyme 12.6 ships these variants; each maps to
 * `fd_sys_color_primary_<name>` and friends.
 */
enum class FlymeAccent(@StyleRes val theme: Int, @StyleRes val overlay: Int) {
    BLUE(R.style.Theme_Flymex_Blue, 0),
    GREEN(R.style.Theme_Flymex_Green, R.style.ThemeOverlay_Flymex_Green),
    ORANGE(R.style.Theme_Flymex_Orange, R.style.ThemeOverlay_Flymex_Orange),
    PURPLE(R.style.Theme_Flymex_Purple, R.style.ThemeOverlay_Flymex_Purple),
    RED(R.style.Theme_Flymex_Red, R.style.ThemeOverlay_Flymex_Red),
    YELLOW(R.style.Theme_Flymex_Yellow, R.style.ThemeOverlay_Flymex_Yellow),
    CYAN(R.style.Theme_Flymex_Cyan, R.style.ThemeOverlay_Flymex_Cyan),
    POLESTAR(R.style.Theme_Flymex_Polestar, R.style.ThemeOverlay_Flymex_Polestar),
}

/** Helpers for resolving Flyme theme attributes. */
object FlymeTheme {

    /** Apply a Flyme theme colour to an Activity. Call before `super.onCreate()`. */
    @JvmStatic
    fun apply(activity: Activity, accent: FlymeAccent = FlymeAccent.BLUE) {
        activity.setTheme(accent.theme)
    }

    /** A context whose Flyme theme colour is [accent]; use it to inflate a sub-tree. */
    @JvmStatic
    fun withAccent(context: Context, accent: FlymeAccent): Context =
        if (accent.overlay == 0) context else ContextThemeWrapper(context, accent.overlay)

    @JvmStatic
    fun isNight(context: Context): Boolean =
        context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

    @JvmStatic
    @ColorInt
    fun color(context: Context, @AttrRes attr: Int, @ColorInt fallback: Int = 0): Int {
        val tv = TypedValue()
        if (!context.theme.resolveAttribute(attr, tv, true)) return fallback
        return when {
            tv.type >= TypedValue.TYPE_FIRST_COLOR_INT && tv.type <= TypedValue.TYPE_LAST_COLOR_INT -> tv.data
            tv.resourceId != 0 -> context.getColor(tv.resourceId)
            else -> fallback
        }
    }

    @JvmStatic
    fun colorStateList(context: Context, @AttrRes attr: Int): ColorStateList? {
        val tv = TypedValue()
        if (!context.theme.resolveAttribute(attr, tv, true)) return null
        return when {
            tv.type >= TypedValue.TYPE_FIRST_COLOR_INT && tv.type <= TypedValue.TYPE_LAST_COLOR_INT -> ColorStateList.valueOf(tv.data)
            tv.resourceId != 0 -> context.getColorStateList(tv.resourceId)
            else -> null
        }
    }

    @JvmStatic
    fun dimension(context: Context, @AttrRes attr: Int, fallbackPx: Float = 0f): Float {
        val tv = TypedValue()
        if (!context.theme.resolveAttribute(attr, tv, true)) return fallbackPx
        return when {
            tv.type == TypedValue.TYPE_DIMENSION -> tv.getDimension(context.resources.displayMetrics)
            tv.resourceId != 0 -> context.resources.getDimension(tv.resourceId)
            else -> fallbackPx
        }
    }

    // Convenience accessors for the most used Flyme roles.
    @JvmStatic fun primary(c: Context) = color(c, androidx.appcompat.R.attr.colorPrimary)
    @JvmStatic fun onPrimary(c: Context) = color(c, R.attr.colorOnPrimary)
    @JvmStatic fun primaryContainer(c: Context) = color(c, R.attr.colorPrimaryContainer)
    @JvmStatic fun brand(c: Context) = color(c, R.attr.colorBrand)
    @JvmStatic fun surface(c: Context) = color(c, R.attr.colorSurface)
    @JvmStatic fun surfaceContainerLowest(c: Context) = color(c, R.attr.colorSurfaceContainerLowest)
    @JvmStatic fun onSurface(c: Context) = color(c, R.attr.colorOnSurface)
    @JvmStatic fun onSurfaceVariant(c: Context) = color(c, R.attr.colorOnSurfaceVariant)
    @JvmStatic fun statePressed(c: Context) = color(c, R.attr.colorStatePressed)
    @JvmStatic fun stateDisabled(c: Context) = color(c, R.attr.colorStateDisabled)
}

/** `dp` → px for the receiver context. */
fun Context.dp(value: Float): Float = value * resources.displayMetrics.density
fun Context.dpInt(value: Float): Int = (value * resources.displayMetrics.density + 0.5f).toInt()
