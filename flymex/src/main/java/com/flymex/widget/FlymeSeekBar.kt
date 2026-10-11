package com.flymex.widget

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.core.graphics.ColorUtils
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.flymex.R
import com.flymex.theme.FlymeTheme
import kotlin.math.min

/**
 * Flyme seek bar — `Widget.Flyme.Light.SeekBar`, verified pixel-for-pixel against the brightness
 * slider of Flyme 12.6 Settings on a meizu 21 Pro (520dpi). It reproduces the ROM's drawables:
 *
 * - **track** (`mz_seekbar_background_enable`, drawn as the view background so it spans the full
 *   width): 22dp tall (`mz_seekbar_height`), corners `mz_seekbar_radius` 15dp — more than half the
 *   height, so the ends are full semicircles (a pill); colour `mz_seekbar_background_state_color`
 *   = `?colorSurfaceContainerLow`
 * - **left cap**: the same layer-list draws a 12dp-wide (11dp when disabled) `?colorBrand` piece
 *   with pill-rounded left corners under the fill, so the fill always starts rounded
 * - **fill** (`mz_seekbar_rectangle_progress`): a square `?colorBrand` rectangle from the thumb's
 *   zero position (11dp, `mz_seekbar_background_padding_start`) to the thumb centre; its square
 *   ends are hidden under the cap and the thumb
 * - **thumb** (`mz_seekbar_thumb`): 22dp `?colorBrand` disc with a 14dp `?colorOnPrimary` dot;
 *   disabled it is only the right half-disc, and the brand parts drop to 20 % alpha
 *   (`mz_seekbar_thumb_state_color`)
 * - paddings 11dp start/end and 4dp top/bottom (`Widget.Flymex.SeekBar`), so the thumb centre and
 *   the touch mapping both travel from 11dp to width − 11dp like the platform SeekBar
 * - **press scale** (`SeekBarAnimHelper`): on touch down the whole view springs to 1.05×
 *   (stiffness 250, damping 0.89) and back on release — [touchScaleEnabled], [touchScale]
 * - `showNumber`: 26dp value bubble (`mcLargeCircleRadis` 13dp, `mcTextNumberSize` 14sp) while dragging
 * - `Widget.Flymex.SeekBar.Step` / [stepItemsCount]: 3dp spots (`mc_enhance_seekbar_spot_radius`)
 *
 * Drawn in code; the AppCompat touch handling, `progress`/`max` and `OnSeekBarChangeListener`
 * API are unchanged.
 */
open class FlymeSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.flymexSeekBarStyle,
) : AppCompatSeekBar(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val rect = RectF()
    private val path = Path()
    private val capRadii = FloatArray(8)

    private val trackHeight = resources.getDimension(R.dimen.mz_seekbar_height)
    private val trackRadius = min(resources.getDimension(R.dimen.mz_seekbar_radius), trackHeight / 2f)
    private val thumbRadius = trackHeight / 2f
    private val dotRadius = dp(7f)
    private val capWidth = dp(12f)
    private val capWidthDisabled = dp(11f)
    private val spotRadius = resources.getDimension(R.dimen.mc_enhance_seekbar_spot_radius)

    private var trackColor = FlymeTheme.color(context, R.attr.colorSurfaceContainerLow)
    private var fillColor = FlymeTheme.brand(context)
    private var dotColor = FlymeTheme.onPrimary(context)

    private var bubbleRadius: Float
    private var bubbleDistance: Float

    /** Show the value bubble while dragging. */
    var showNumber: Boolean
    /** Format the bubble text; default is the raw progress. */
    var numberFormatter: (Int) -> String = { it.toString() }
    /** Number of discrete steps (`mcStepItemsCount`); draws spots and snaps `max` to `steps - 1`. 0 = continuous. */
    var stepItemsCount: Int = 0
        set(value) {
            field = value.coerceAtLeast(0)
            if (field > 1) max = field - 1
            invalidate()
        }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.FlymeSeekBar, defStyleAttr, 0)
        bubbleRadius = a.getDimension(R.styleable.FlymeSeekBar_mcLargeCircleRadis, dp(13f))
        bubbleDistance = a.getDimension(R.styleable.FlymeSeekBar_mcDistanceToCircle, dp(2f))
        textPaint.color = a.getColor(R.styleable.FlymeSeekBar_mcTextNumberColor, Color.WHITE)
        textPaint.textSize = a.getDimension(R.styleable.FlymeSeekBar_mcTextNumberSize, sp(14f))
        showNumber = a.getBoolean(R.styleable.FlymeSeekBar_mcShowNumber, false)
        stepItemsCount = a.getInt(R.styleable.FlymeSeekBar_mcStepItemsCount, 0)
        a.recycle()
        textPaint.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)

        // The platform draws nothing. A transparent 22dp thumb with the stock 11dp thumbOffset keeps
        // AbsSeekBar's touch→progress mapping identical to Flyme's (scale = (x − padding) / inner width).
        progressDrawable = ColorDrawable(Color.TRANSPARENT)
        background = null
        thumb = GradientDrawable().apply {
            setColor(Color.TRANSPARENT)
            val s = trackHeight.toInt()
            setSize(s, s)
        }
        thumbOffset = thumbRadius.toInt()
        splitTrack = false
        minimumHeight = trackHeight.toInt()
    }

    private fun dp(v: Float) = v * resources.displayMetrics.density
    private fun sp(v: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, resources.displayMetrics)

    /** Override the fill (progress) colour; defaults to `?colorBrand`, track `?colorSurfaceContainerLow`, dot `?colorOnPrimary`. */
    fun setColors(fill: Int, track: Int = trackColor, thumb: Int = dotColor) {
        fillColor = fill
        trackColor = track
        dotColor = thumb
        invalidate()
    }

    override fun setProgressTintList(tint: ColorStateList?) {
        tint?.let { fillColor = it.defaultColor }
        invalidate()
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val h = trackHeight.toInt() + paddingTop + paddingBottom
        setMeasuredDimension(getDefaultSize(suggestedMinimumWidth, widthMeasureSpec), resolveSize(h, heightMeasureSpec))
    }

    // ---- geometry ----------------------------------------------------------------------------

    /** Top of the 22dp track: centred in the padded height, like the background's center_vertical gravity. */
    private fun trackTop(): Float = paddingTop + (height - paddingTop - paddingBottom - trackHeight) / 2f

    /** Thumb centre for a 0..1 ratio: travels paddingLeft → width − paddingRight, kept inside the track. */
    private fun thumbCenterX(ratio: Float): Float {
        val inner = (width - paddingLeft - paddingRight).toFloat()
        return (paddingLeft + ratio * inner).coerceIn(thumbRadius, width - thumbRadius)
    }

    private fun currentRatio(): Float = if (max > 0) progress.toFloat() / max else 0f

    // ---- value bubble: a floating PopupWindow above the thumb, like AnimSeekBar's mcLargeCircleDrawble ----
    private var bubble: android.widget.PopupWindow? = null
    private val bubbleView by lazy {
        object : android.view.View(context) {
            override fun onDraw(c: Canvas) {
                val r = bubbleRadius
                paint.color = fillColor
                c.drawCircle(r, r, r, paint)
                textPaint.alpha = 255
                c.drawText(numberFormatter(progress), r, r - (textPaint.descent() + textPaint.ascent()) / 2f, textPaint)
            }
        }
    }

    private fun updateBubble(show: Boolean) {
        if (!showNumber) return
        val size = (bubbleRadius * 2).toInt()
        val loc = IntArray(2).also { getLocationInWindow(it) }
        val x = loc[0] + thumbCenterX(currentRatio()).toInt() - size / 2
        val y = loc[1] + trackTop().toInt() - bubbleDistance.toInt() - size
        if (show) {
            val p = bubble ?: android.widget.PopupWindow(bubbleView, size, size, false).also {
                it.isTouchable = false
                it.isClippingEnabled = false
                it.animationStyle = R.style.Animation_Flymex_Snackbar
                bubble = it
            }
            if (p.isShowing) p.update(x, y, -1, -1) else p.showAtLocation(this, android.view.Gravity.NO_GRAVITY, x, y)
            bubbleView.invalidate()
        } else {
            bubble?.dismiss()
        }
    }

    // ---- touch scale: MzSeekBar.mAnimHelper (SeekBarAnimHelper.handleTouchScale) ----------------
    // On ACTION_DOWN the whole view springs to 1.05× (mTouchScaledX/Y), back to 1× on UP/CANCEL,
    // with SpringForce stiffness 250 and damping ratio 0.89 on SCALE_X and SCALE_Y.

    /** Press-to-enlarge spring (`MzSeekBar.setTouchScaleEnable`); on by default like Flyme. */
    var touchScaleEnabled: Boolean = true
    /** Scale while touched (`SeekBarAnimHelper.mTouchScaledX/Y`, 1.05 in MzSeekBar). */
    var touchScale: Float = 1.05f

    private var scaleXAnim: SpringAnimation? = null
    private var scaleYAnim: SpringAnimation? = null

    private fun handleTouchScale(pressed: Boolean) {
        scaleXAnim?.takeIf { it.isRunning }?.cancel()
        scaleYAnim?.takeIf { it.isRunning }?.cancel()
        val target = if (pressed) touchScale else 1f
        scaleYAnim = SpringAnimation(this, DynamicAnimation.SCALE_Y).apply {
            spring = SpringForce(target).setStiffness(TOUCH_SCALE_STIFFNESS).setDampingRatio(TOUCH_SCALE_DAMPING)
            start()
        }
        scaleXAnim = SpringAnimation(this, DynamicAnimation.SCALE_X).apply {
            spring = SpringForce(target).setStiffness(TOUCH_SCALE_STIFFNESS).setDampingRatio(TOUCH_SCALE_DAMPING)
            start()
        }
    }

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        if (touchScaleEnabled && isEnabled) {
            when (event.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN -> handleTouchScale(true)
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> handleTouchScale(false)
            }
        }
        val handled = super.onTouchEvent(event)
        when (event.actionMasked) {
            android.view.MotionEvent.ACTION_DOWN, android.view.MotionEvent.ACTION_MOVE -> updateBubble(true)
            android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> updateBubble(false)
        }
        return handled
    }

    private companion object {
        /** `SeekBarAnimHelper`: SpringForce stiffness 250, damping ratio 0.89. */
        const val TOUCH_SCALE_STIFFNESS = 250f
        const val TOUCH_SCALE_DAMPING = 0.89f
    }

    override fun onDetachedFromWindow() {
        bubble?.dismiss()
        scaleXAnim?.cancel()
        scaleYAnim?.cancel()
        super.onDetachedFromWindow()
    }

    /**
     * The 1.05× press scale draws outside the view's own bounds. Flyme's preference layout leaves
     * 28dp of padding for that; here we instead switch off clipping on the ancestors up to the
     * first scrolling container, so the enlarged bar is never cut flat at its ends.
     */
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!touchScaleEnabled) return
        var p = parent
        while (p is android.view.ViewGroup) {
            if (p is android.widget.ScrollView || p is android.widget.AbsListView ||
                p is androidx.core.widget.NestedScrollView || p.javaClass.name.endsWith("RecyclerView")
            ) break
            p.clipChildren = false
            p.clipToPadding = false
            p = p.parent
        }
    }

    // ---- drawing ------------------------------------------------------------------------------

    override fun onDraw(canvas: Canvas) {
        val enabled = isEnabled
        val top = trackTop()
        val bottom = top + trackHeight
        val cy = top + thumbRadius
        val w = width.toFloat()
        val cx = thumbCenterX(currentRatio())
        val fillStart = thumbCenterX(0f)
        // mz_seekbar_thumb_state_color: ?colorBrand, 20 % when disabled
        val brand = if (enabled) fillColor else ColorUtils.setAlphaComponent(fillColor, 51)

        // 1. track: full-width pill (background drawable → ignores padding)
        paint.color = trackColor
        rect.set(0f, top, w, bottom)
        canvas.drawRoundRect(rect, trackRadius, trackRadius, paint)

        // 2. step spots (StepSeekBar) at each step's thumb centre
        if (stepItemsCount > 1) {
            paint.color = ColorUtils.setAlphaComponent(FlymeTheme.onSurfaceVariant(context), 0x40)
            for (i in 0 until stepItemsCount) {
                canvas.drawCircle(thumbCenterX(i / (stepItemsCount - 1f)), cy, spotRadius, paint)
            }
        }

        // 3. left cap: 12dp (11dp disabled) brand piece with pill-rounded left corners
        paint.color = brand
        val cap = if (enabled) capWidth else capWidthDisabled
        rect.set(0f, top, cap, bottom)
        capRadii.fill(0f)
        capRadii[0] = trackRadius; capRadii[1] = trackRadius      // top-left
        capRadii[6] = trackRadius; capRadii[7] = trackRadius      // bottom-left
        path.reset()
        path.addRoundRect(rect, capRadii, Path.Direction.CW)
        canvas.drawPath(path, paint)

        // 4. fill: square rectangle from the zero position to the thumb centre
        if (cx > fillStart) {
            rect.set(fillStart, top, cx, bottom)
            canvas.drawRect(rect, paint)
        }

        // 5. thumb: 22dp brand disc (right half only when disabled, so the translucent shapes never overlap)
        if (enabled) {
            canvas.drawCircle(cx, cy, thumbRadius, paint)
        } else {
            canvas.save()
            canvas.clipRect(cx, top, cx + thumbRadius, bottom)
            canvas.drawCircle(cx, cy, thumbRadius, paint)
            canvas.restore()
        }

        // 6. 14dp ?colorOnPrimary dot
        paint.color = dotColor
        canvas.drawCircle(cx, cy, dotRadius, paint)
    }
}
