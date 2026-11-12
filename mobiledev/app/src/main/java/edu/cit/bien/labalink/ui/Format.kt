package edu.cit.bien.labalink.ui

import androidx.annotation.DrawableRes
import edu.cit.bien.labalink.R
import java.text.SimpleDateFormat
import java.util.Locale

fun peso(amount: Double): String = "₱ %,.2f".format(amount)

@DrawableRes
fun machineIcon(machineType: String): Int =
    if (machineType == "WASHER") R.drawable.ic_washing_machine else R.drawable.ic_wind

/** Washers in pandan green, dryers in mango. */
fun machineTone(machineType: String): Tone =
    if (machineType == "WASHER") Tone.BRAND else Tone.ACCENT

fun machineLabel(machineType: String): String =
    if (machineType == "WASHER") "Washer" else "Dryer"

// The API sends local date-times like 2026-11-12T14:25:30.123.
// java.time needs API 26, so SimpleDateFormat keeps minSdk 24 working.
private val API_TIME = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)

/** "Nov 12, 2:25 PM", or the raw value if it can't be parsed. */
fun formatDateTime(value: String): String =
    reformat(value, "MMM d, h:mm a")

/** "Nov 12, 2026", or the raw value if it can't be parsed. */
fun formatDate(value: String): String =
    reformat(value, "MMM d, yyyy")

private fun reformat(value: String, pattern: String): String = try {
    API_TIME.parse(value.take(19))
        ?.let { SimpleDateFormat(pattern, Locale.getDefault()).format(it) }
        ?: value.take(16)
} catch (e: Exception) {
    value.take(16)
}
