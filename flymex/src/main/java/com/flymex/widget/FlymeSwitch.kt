package com.flymex.widget

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.SoundEffectConstants
import android.view.View
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.animation.PathInterpolator
import android.widget.Checkable
import androidx.core.graphics.ColorUtils
import com.flymex.R
import com.flymex.theme.dp
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Flyme switch — a port of `com.meizu.common.widget.Switch`, verified pixel-for-pixel against
 * Flyme 12.6 on a meizu 21 Pro (520dpi).
 *
 * - track: `mc_switch_anim_track` 22dp tall, 11dp radius, `mcSwitchMinWidth` 40dp
 * - thumb: 14dp circle (`mz_switch_thumb_to_width`), inset `mz_switch_thumb_padding_left` 4dp
 * - colours (`Widget.MeizuCommon.Switch`): track on `mz_switch_track_on_color_state` (`?colorBrand`,
 *   25 % when disabled), track off `mz_switch_track_off_color_state` (`?colorSurfaceContainerHigh`),
 *   thumb `mz_switch_thumb_on/off_color` (`?colorOnBrand`); dark-surface variants `mz_switch_*_dark_*`
 * - motion: 200ms, `mc_btn_checked_single_interpolator` (0.2, 0.3, 0.21, 0.97)
 */
open class FlymeSwitch @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.flymexSwitchStyle,
) : View(context, attrs, defStyleAttr), Checkable {

    fun interface OnCheckedChangeListener {
        fun onCheckedChanged(switch: FlymeSwitch, isChecked: Boolean)
    }

    private val trackHeight = context.dp(22f)
    private val minWidthPx: Float
    private val thumbPad: Float
    private val thumbSize: Float

    private var trackOn: ColorStateList
    private var trackOff: ColorStateList
    private var thumbOn: ColorStateList
    private var thumbOff: ColorStateList
    private var trackDarkOn: ColorStateList
    private var trackDarkOff: ColorStateList
    private var thumbDarkOn: ColorStateList
    private var thumbDarkOff: ColorStateList

    /** Use the dark-surface palette (`mz_switch_*_dark_*`), e.g. on dark artwork. */
    var onDark: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private var progress = 0f
    private var checked = false
    private var animator: ValueAnimator? = null
    private var listener: OnCheckedChangeListener? = null
    private var broadcasting = false

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var dragging = false
    private var dragStartProgress = 0f

    init {
        val res = context.resources
        val a = context.obtainStyledAttributes(attrs, R.styleable.FlymeSwitch, defStyleAttr, 0)
        minWidthPx = a.getDimension(R.styleable.FlymeSwitch_mcSwitchMinWidth, res.getDimension(R.dimen.mcSwitchMinWidth))
        trackOn = a.getColorStateList(R.styleable.FlymeSwitch_mcTrackOnColor) ?: csl(R.color.mz_switch_track_on_color_state)
        trackOff = a.getColorStateList(R.styleable.FlymeSwitch_mcTrackOffColor) ?: csl(R.color.mz_switch_track_off_color_state)
        thumbOn = a.getColorStateList(R.styleable.FlymeSwitch_mcThumbOnColor) ?: csl(R.color.mz_switch_thumb_on_color)
        thumbOff = a.getColorStateList(R.styleable.FlymeSwitch_mcThumbOffColor) ?: csl(R.color.mz_switch_thumb_off_color)
        trackDarkOn = a.getColorStateList(R.styleable.FlymeSwitch_mcTrackDarkOnColor) ?: csl(R.color.mz_switch_track_dark_on_color)
        trackDarkOff = a.getColorStateList(R.styleable.FlymeSwitch_mcTrackDarkOffColor) ?: csl(R.color.mz_switch_track_dark_off_color)
        thumbDarkOn = a.getColorStateList(R.styleable.FlymeSwitch_mcThumbDarkOnColor) ?: csl(R.color.mz_switch_thumb_dark_on_color)
        thumbDarkOff = a.getColorStateList(R.styleable.FlymeSwitch_mcThumbDarkOffColor) ?: csl(R.color.mz_switch_thumb_dark_off_color)
        onDark = a.getBoolean(R.styleable.FlymeSwitch_mcSwitchOnDark, false)
        checked = a.getBoolean(R.styleable.FlymeSwitch_android_checked, false)
        isEnabled = a.getBoolean(R.styleable.FlymeSwitch_android_enabled, true)
        a.recycle()

        thumbPad = res.getDimension(R.dimen.mz_switch_thumb_padding_left)
        thumbSize = res.getDimension(R.dimen.mz_switch_thumb_to_width)
        progress = if (checked) 1f else 0f
        isClickable = true
        isFocusable = true
    }

    private fun csl(id: Int): ColorStateList = context.getColorStateList(id)

    // ---------------------------------------------------------------- Checkable

    override fun isChecked(): Boolean = checked

    override fun setChecked(value: Boolean) = setChecked(value, animate = isAttachedToWindow && isLaidOut)

    fun setChecked(value: Boolean, animate: Boolean) {
        if (checked == value) return
        checked = value
        animateTo(if (value) 1f else 0f, animate)
        refreshDrawableState()
        if (!broadcasting) {
            broadcasting = true
            listener?.onCheckedChanged(this, checked)
            broadcasting = false
        }
        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED)
    }

    override fun toggle() = setChecked(!checked)

    fun setOnCheckedChangeListener(l: OnCheckedChangeListener?) {
        listener = l
    }

    override fun performClick(): Boolean {
        toggle()
        val handled = super.performClick()
        if (!handled) playSoundEffect(SoundEffectConstants.CLICK)
        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        return true
    }

    private fun animateTo(target: Float, animate: Boolean) {
        animator?.cancel()
        if (!animate) {
            progress = target
            invalidate()
            return
        }
        animator = ValueAnimator.ofFloat(progress, target).apply {
            duration = 200
            interpolator = CHECK_INTERPOLATOR
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    // ---------------------------------------------------------------- measure/draw

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = resolveSize((minWidthPx + paddingLeft + paddingRight).roundToInt(), widthMeasureSpec)
        val h = resolveSize((trackHeight + paddingTop + paddingBottom).roundToInt(), heightMeasureSpec)
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        val trackW = (width - paddingLeft - paddingRight).toFloat().coerceAtLeast(minWidthPx)
        val left = paddingLeft.toFloat()
        val top = paddingTop + (height - paddingTop - paddingBottom - trackHeight) / 2f
        val r = trackHeight / 2f

        val state = drawableState
        val offTrack = (if (onDark) trackDarkOff else trackOff).getColorForState(state, Color.GRAY)
        val onTrack = (if (onDark) trackDarkOn else trackOn).getColorForState(state, Color.BLUE)
        val offThumb = (if (onDark) thumbDarkOff else thumbOff).getColorForState(state, Color.WHITE)
        val onThumb = (if (onDark) thumbDarkOn else thumbOn).getColorForState(state, Color.WHITE)

        paint.color = ColorUtils.blendARGB(offTrack, onTrack, progress)
        rect.set(left, top, left + trackW, top + trackHeight)
        canvas.drawRoundRect(rect, r, r, paint)

        val startX = left + thumbPad
        val endX = left + trackW - thumbPad - thumbSize
        val cx = startX + (endX - startX) * progress + thumbSize / 2f
        val cy = top + trackHeight / 2f
        paint.color = ColorUtils.blendARGB(offThumb, onThumb, progress)
        canvas.drawCircle(cx, cy, thumbSize / 2f, paint)
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        invalidate()
    }

    // ---------------------------------------------------------------- touch (tap or drag)

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                dragging = false
                dragStartProgress = progress
                isPressed = true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - downX
                if (!dragging && abs(dx) > touchSlop) {
                    dragging = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                    animator?.cancel()
                }
                if (dragging) {
                    val travel = (width - paddingLeft - paddingRight) - thumbPad * 2 - thumbSize
                    progress = (dragStartProgress + dx / travel).coerceIn(0f, 1f)
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                isPressed = false
                if (dragging) {
                    dragging = false
                    val target = progress >= 0.5f
                    if (target != checked) setChecked(target, animate = true) else animateTo(if (checked) 1f else 0f, true)
                } else {
                    performClick()
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                isPressed = false
                dragging = false
                animateTo(if (checked) 1f else 0f, true)
            }
        }
        return true
    }

    // ---------------------------------------------------------------- a11y / state

    override fun getAccessibilityClassName(): CharSequence = "android.widget.Switch"

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.isCheckable = true
        info.isChecked = checked
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val state = super.onCreateDrawableState(extraSpace + 1)
        if (checked) mergeDrawableStates(state, CHECKED_STATE_SET)
        return state
    }

    override fun onSaveInstanceState(): Parcelable {
        return SavedState(super.onSaveInstanceState(), checked)
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is SavedState) {
            super.onRestoreInstanceState(state.superState)
            checked = state.checked
            progress = if (checked) 1f else 0f
            invalidate()
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    private class SavedState : BaseSavedState {
        val checked: Boolean

        constructor(superState: Parcelable?, checked: Boolean) : super(superState) {
            this.checked = checked
        }

        constructor(source: Parcel) : super(source) {
            checked = source.readInt() == 1
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(if (checked) 1 else 0)
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(source: Parcel) = SavedState(source)
            override fun newArray(size: Int) = arrayOfNulls<SavedState>(size)
        }
    }

    companion object {
        private val CHECKED_STATE_SET = intArrayOf(android.R.attr.state_checked)

        /** `interpolator/mc_btn_checked_single_interpolator` */
        val CHECK_INTERPOLATOR = PathInterpolator(0.2f, 0.3f, 0.21f, 0.97f)
    }
}
