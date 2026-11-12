package edu.cit.bien.labalink.ui

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.max

/**
 * Android 15+ draws every app edge to edge, behind the status and
 * navigation bars. Use these instead of setContentView: the screen goes
 * edge to edge on every Android version and keeps clear of the system bars,
 * display cutout and keyboard.
 *
 * By default the root view takes all the padding. Pass [topInset] /
 * [bottomInset] to pad a top bar or bottom bar instead, so its background
 * runs behind the system bar while its content stays clear of it.
 */
fun AppCompatActivity.setContentViewEdgeToEdge(
    @LayoutRes layout: Int,
    @IdRes topInset: Int = View.NO_ID,
    @IdRes bottomInset: Int = View.NO_ID,
    statusBar: BarBackground = BarBackground.LIGHT,
    navigationBar: BarBackground = BarBackground.LIGHT,
) {
    enableEdgeToEdge(statusBar.style(), navigationBar.style())
    setContentView(layout)
    val root = findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
    padForSystemBars(root, topInset, bottomInset)
}

fun AppCompatActivity.setContentViewEdgeToEdge(
    root: View,
    @IdRes topInset: Int = View.NO_ID,
    @IdRes bottomInset: Int = View.NO_ID,
    statusBar: BarBackground = BarBackground.LIGHT,
    navigationBar: BarBackground = BarBackground.LIGHT,
) {
    enableEdgeToEdge(statusBar.style(), navigationBar.style())
    setContentView(root)
    padForSystemBars(root, topInset, bottomInset)
}

/**
 * What the screen draws behind a system bar, so the bar's icons stay
 * readable. LabaLink has a light theme only, so this is set per screen
 * rather than following the system's dark mode.
 */
enum class BarBackground {
    LIGHT, DARK;

    internal fun style(): SystemBarStyle = when (this) {
        LIGHT -> SystemBarStyle.light(Color.TRANSPARENT, LEGACY_NAV_SCRIM)
        DARK -> SystemBarStyle.dark(Color.TRANSPARENT)
    }
}

// Android 7 can't draw dark navigation icons, so it gets a dim scrim.
private val LEGACY_NAV_SCRIM = Color.argb(0x80, 0x1B, 0x1B, 0x1B)

private fun padForSystemBars(
    root: View,
    @IdRes topInset: Int,
    @IdRes bottomInset: Int,
) {
    val top = root.findOrSelf(topInset)
    val bottom = root.findOrSelf(bottomInset)
    val views = linkedSetOf(root, top, bottom)
    val initial = views.associateWith {
        intArrayOf(it.paddingLeft, it.paddingTop, it.paddingRight, it.paddingBottom)
    }

    ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
        val bars = insets.getInsets(
            WindowInsetsCompat.Type.systemBars()
                or WindowInsetsCompat.Type.displayCutout())
        // The keyboard covers the navigation bar, so only the part taller
        // than it is extra. The root shrinks by that much, which lifts a
        // bottom bar or the end of a form above the keyboard.
        val keyboard = max(
            0, insets.getInsets(WindowInsetsCompat.Type.ime()).bottom - bars.bottom)

        for (view in views) {
            val p = initial.getValue(view)
            view.setPadding(
                p[0] + if (view === root) bars.left else 0,
                p[1] + if (view === top) bars.top else 0,
                p[2] + if (view === root) bars.right else 0,
                p[3] + (if (view === bottom) bars.bottom else 0)
                    + (if (view === root) keyboard else 0))
        }
        WindowInsetsCompat.CONSUMED
    }
}

private fun View.findOrSelf(@IdRes id: Int): View =
    if (id == View.NO_ID) this else findViewById(id)
