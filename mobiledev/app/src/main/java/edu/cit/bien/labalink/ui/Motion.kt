package edu.cit.bien.labalink.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.graphics.drawable.Animatable
import android.os.Build
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import edu.cit.bien.labalink.R

// Motion rules: 100-150ms for presses, 250-400ms for entrances and screens,
// never more than 600ms for anything the user waits on. Only transform and
// alpha are animated, so nothing triggers a layout pass.

private val EASING = FastOutSlowInInterpolator()

/**
 * False when the user switched animations off (Settings > Accessibility >
 * Remove animations, or a 0x animator scale). Looping animations check it;
 * one-shot animators are skipped by the system automatically.
 */
val motionEnabled: Boolean
    get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
        ValueAnimator.areAnimatorsEnabled()

/** Fades a view in while lifting it into place. */
fun View.riseIn(delayMs: Long = 0) {
    if (!motionEnabled) return
    alpha = 0f
    translationY = resources.displayMetrics.density * 24
    animate()
        .alpha(1f)
        .translationY(0f)
        .setStartDelay(delayMs)
        .setDuration(400)
        .setInterpolator(EASING)
        .start()
}

/** A ring that keeps expanding and fading behind a "live" dot. */
fun View.startHalo() {
    if (!motionEnabled) {
        visibility = View.INVISIBLE
        return
    }
    val halo = ObjectAnimator.ofPropertyValuesHolder(
        this,
        PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 3f),
        PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 3f),
        PropertyValuesHolder.ofFloat(View.ALPHA, 0.6f, 0f),
    ).apply {
        duration = 1400
        repeatCount = ValueAnimator.INFINITE
        interpolator = EASING
    }
    runWhileAttached(halo)
}

/** Moves a view down [distancePx] and back, forever (scanner line). */
fun View.startSweep(distancePx: Float) {
    if (!motionEnabled) return
    val sweep = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, 0f, distancePx).apply {
        duration = 1600
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        interpolator = AccelerateDecelerateInterpolator()
    }
    runWhileAttached(sweep)
}

/**
 * Shows a peso amount, counting from the value currently shown (or from
 * zero the first time). Tabular figures in the text style keep it steady.
 */
fun TextView.animatePeso(target: Double) {
    val from = getTag(R.id.tag_shown_amount) as? Double ?: 0.0
    setTag(R.id.tag_shown_amount, target)
    if (from == target || !motionEnabled) {
        text = peso(target)
        return
    }
    ValueAnimator.ofFloat(from.toFloat(), target.toFloat()).apply {
        duration = 600
        interpolator = EASING
        addUpdateListener { text = peso((it.animatedValue as Float).toDouble()) }
        addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                text = peso(target) // exact final value, no float rounding
            }
        })
        start()
    }
}

/** Starts an AnimatedVectorDrawable shown by this ImageView. */
fun ImageView.playIcon() {
    (drawable as? Animatable)?.start()
}

private fun View.runWhileAttached(animator: Animator) {
    (getTag(R.id.tag_looping_animator) as? Animator)?.cancel()
    setTag(R.id.tag_looping_animator, animator)
    addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) = Unit
        override fun onViewDetachedFromWindow(v: View) {
            animator.cancel()
            v.removeOnAttachStateChangeListener(this)
        }
    })
    animator.start()
}
