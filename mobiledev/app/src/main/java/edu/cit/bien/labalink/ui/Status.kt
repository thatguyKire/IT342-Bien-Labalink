package edu.cit.bien.labalink.ui

import android.content.res.ColorStateList
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.widget.ImageViewCompat
import edu.cit.bien.labalink.R

/** Colour pairs (text on tint) used for badges and icon tiles. */
enum class Tone(@ColorRes val content: Int, @ColorRes val container: Int) {
    BRAND(R.color.brand, R.color.brand_tint),
    ACCENT(R.color.accent_deep, R.color.accent_tint),
    NEUTRAL(R.color.neutral, R.color.neutral_tint),
    INFO(R.color.info, R.color.info_tint),
    SUCCESS(R.color.success, R.color.success_tint),
    WARNING(R.color.warning, R.color.warning_tint),
    DANGER(R.color.danger, R.color.danger_tint),
}

/** Every machine, booking and payment status the API returns. */
private val STATUSES = mapOf(
    "AVAILABLE" to ("Available" to Tone.SUCCESS),
    "RUNNING" to ("Running" to Tone.INFO),
    "OUT_OF_ORDER" to ("Out of order" to Tone.DANGER),
    "PENDING" to ("Pending" to Tone.WARNING),
    "ACTIVE" to ("Active" to Tone.INFO),
    "COMPLETED" to ("Completed" to Tone.SUCCESS),
    "CANCELLED" to ("Cancelled" to Tone.NEUTRAL),
    "SUCCESS" to ("Paid" to Tone.SUCCESS),
    "FAILED" to ("Failed" to Tone.DANGER),
)

fun toneOf(status: String): Tone = STATUSES[status]?.second ?: Tone.NEUTRAL

fun labelOf(status: String): String = STATUSES[status]?.first
    ?: status.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }

/** Shows [status] as a coloured badge (style Widget.LabaLink.Badge). */
fun TextView.showStatus(status: String) {
    text = labelOf(status)
    applyTone(toneOf(status))
}

fun TextView.applyTone(tone: Tone) {
    setTextColor(ContextCompat.getColor(context, tone.content))
    ViewCompat.setBackgroundTintList(
        this, ColorStateList.valueOf(ContextCompat.getColor(context, tone.container)))
}

/** Colours an icon tile (style Widget.LabaLink.IconTile) and its icon. */
fun ImageView.applyTone(tone: Tone) {
    ViewCompat.setBackgroundTintList(
        this, ColorStateList.valueOf(ContextCompat.getColor(context, tone.container)))
    ImageViewCompat.setImageTintList(
        this, ColorStateList.valueOf(ContextCompat.getColor(context, tone.content)))
}
