package com.flymex.widget

import android.app.Activity
import android.content.Context
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.TextViewCompat
import com.flymex.R
import com.flymex.theme.FlymeTheme
import com.flymex.theme.dpInt

/**
 * Flyme toast (`MzToast` / SystemUI `text_toast.xml`).
 *
 * Frame `toast_frame`: white, 8dp radius, 0.5dp #0d000000 stroke (night: `mz_toast_frame_dark`,
 * #101010); min 128×39dp, padding 18×12dp, max width 284dp, 14sp medium text with 4dp line
 * spacing; 30dp above the bottom (`mz_toast_y_offset`); shadow `toast_elevation` 60dp.
 * Enter/exit motion is `mz_snackbar_enter_anim` (scale 0.8→1, 200ms + alpha 150ms).
 *
 * Rendered inside the Activity window (custom system toasts are restricted since API 30).
 */
object FlymeToast {
    const val LENGTH_SHORT = 2000L
    const val LENGTH_LONG = 3500L

    private val handler = Handler(Looper.getMainLooper())
    private var current: View? = null
    private var dismissRunnable: Runnable? = null
    private val scaleIn = PathInterpolator(0.18f, 0.7f, 0.05f, 1f)
    private val alphaIn = PathInterpolator(0.65f, 0f, 0.35f, 1f)

    @JvmStatic
    @JvmOverloads
    fun show(activity: Activity, text: CharSequence, duration: Long = LENGTH_SHORT, icon: Int = 0) {
        val ctx = activity
        val frame = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundResource(if (FlymeTheme.isNight(ctx)) R.drawable.mz_toast_frame_dark else R.drawable.toast_frame_flymex)
            minimumWidth = ctx.dpInt(128f)
            minimumHeight = ctx.dpInt(39f)
            setPadding(ctx.dpInt(18f), ctx.dpInt(12f), ctx.dpInt(18f), ctx.dpInt(12f))
            elevation = ctx.resources.getDimension(R.dimen.fd_sys_shadow_elevation_large)
            if (android.os.Build.VERSION.SDK_INT >= 28) {
                outlineSpotShadowColor = ctx.getColor(R.color.fd_sys_color_shadow_spot_large)
                outlineAmbientShadowColor = ctx.getColor(R.color.fd_sys_color_shadow_ambient_large)
            }
        }
        if (icon != 0) {
            frame.addView(ImageView(ctx).apply { setImageResource(icon) }, LinearLayout.LayoutParams(ctx.dpInt(16f), ctx.dpInt(16f)).apply {
                topMargin = ctx.dpInt(2f); marginEnd = ctx.dpInt(8f)
            })
        }
        frame.addView(TextView(ctx).apply {
            TextViewCompat.setTextAppearance(this, R.style.TextAppearance_Flymex_Toast)
            setTextColor(if (FlymeTheme.isNight(ctx)) -0x1 else -0x1000000)
            setText(text)
            maxWidth = ctx.dpInt(284f)
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
            setLineSpacing(ctx.dpInt(4f).toFloat(), 1f)
            includeFontPadding = false
        })
        present(activity, frame, duration)
    }

    /** `mc_toast_layout`: 30dp check icon over 14sp medium text, 144dp min width, 24/16dp padding, large shadow. */
    @JvmStatic
    @JvmOverloads
    fun complete(activity: Activity, text: CharSequence, duration: Long = LENGTH_SHORT) {
        val card = android.view.LayoutInflater.from(activity).inflate(R.layout.flymex_complete_toast, null)
        card.findViewById<TextView>(R.id.flymex_toast_text).text = text
        present(activity, card, duration, centered = true)
    }

    private fun present(activity: Activity, view: View, duration: Long, centered: Boolean = false) {
        dismiss()
        val decor = activity.window.decorView as ViewGroup
        val lp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            gravity = if (centered) Gravity.CENTER else Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            val inset = ViewCompat.getRootWindowInsets(decor)?.getInsets(WindowInsetsCompat.Type.systemBars())?.bottom ?: 0
            bottomMargin = if (centered) 0 else activity.resources.getDimensionPixelSize(R.dimen.mz_toast_y_offset) + inset
            leftMargin = activity.dpInt(16f)
            rightMargin = activity.dpInt(16f)
        }
        view.isClickable = false
        view.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        decor.addView(view, lp)
        current = view
        view.pivotY = 0f
        view.scaleX = 0.8f
        view.scaleY = 0.8f
        view.alpha = 0f
        view.animate().scaleX(1f).scaleY(1f).setDuration(200).setInterpolator(scaleIn).start()
        view.animate().alpha(1f).setDuration(150).setInterpolator(alphaIn).start()
        view.announceForAccessibility((view as? TextView)?.text ?: "")
        val r = Runnable { dismiss() }
        dismissRunnable = r
        handler.postDelayed(r, duration)
    }

    @JvmStatic
    fun dismiss() {
        dismissRunnable?.let { handler.removeCallbacks(it) }
        dismissRunnable = null
        val v = current ?: return
        current = null
        v.animate().alpha(0f).scaleX(0.9f).scaleY(0.9f).setDuration(150).setInterpolator(alphaIn)
            .withEndAction { (v.parent as? ViewGroup)?.removeView(v) }.start()
    }

    /** Resolve a context's Activity (for `Context`-based callers). */
    @JvmStatic
    fun show(context: Context, text: CharSequence, duration: Long = LENGTH_SHORT) {
        var c = context
        while (c is android.content.ContextWrapper) {
            if (c is Activity) {
                show(c, text, duration)
                return
            }
            c = c.baseContext
        }
        android.widget.Toast.makeText(context, text, if (duration > LENGTH_SHORT) android.widget.Toast.LENGTH_LONG else android.widget.Toast.LENGTH_SHORT).show()
    }
}
