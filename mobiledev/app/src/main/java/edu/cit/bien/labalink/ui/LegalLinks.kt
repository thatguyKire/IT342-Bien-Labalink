package edu.cit.bien.labalink.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import android.widget.Toast
import edu.cit.bien.labalink.BuildConfig

/**
 * Renders "<prefix> Terms of Service and Privacy Policy." with both names
 * tappable. The URLs come from BuildConfig (see build.gradle.kts).
 */
fun TextView.showLegalLinks(prefix: String) {
    val text = SpannableStringBuilder("$prefix ")
    appendLink(text, "Terms of Service", BuildConfig.TERMS_URL)
    text.append(" and ")
    appendLink(text, "Privacy Policy", BuildConfig.PRIVACY_URL)
    text.append(".")
    setText(text)
    movementMethod = LinkMovementMethod.getInstance()
}

private fun TextView.appendLink(
    text: SpannableStringBuilder,
    label: String,
    url: String,
) {
    val start = text.length
    text.append(label)
    text.setSpan(object : ClickableSpan() {
        override fun onClick(widget: View) {
            try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(context, url, Toast.LENGTH_LONG).show()
            }
        }
    }, start, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
}
