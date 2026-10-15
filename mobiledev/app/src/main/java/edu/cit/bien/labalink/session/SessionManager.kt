package edu.cit.bien.labalink.session

import android.content.Context
import edu.cit.bien.labalink.model.LoginResponse

/**
 * Holds the signed-in customer. The JWT is stored encrypted (TokenCipher)
 * and treated as expired a minute early to avoid racing the server.
 * Nothing here is trusted for authorisation: the server re-checks the
 * token, role and ownership on every request.
 */
class SessionManager(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(
        PREFS, Context.MODE_PRIVATE)

    fun save(response: LoginResponse) {
        val expiresAt = System.currentTimeMillis() +
            (response.expiresIn - 60).coerceAtLeast(0) * 1000
        prefs.edit()
            .putString(KEY_TOKEN, TokenCipher.encrypt(response.accessToken))
            .putLong(KEY_EXPIRES_AT, expiresAt)
            .putString(KEY_USERNAME, response.username)
            .putString(KEY_EMAIL, response.email)
            .putString(KEY_ROLE, response.role)
            .apply()
    }

    val token: String?
        get() {
            val encrypted = prefs.getString(KEY_TOKEN, null) ?: return null
            if (System.currentTimeMillis() >= prefs.getLong(KEY_EXPIRES_AT, 0)) {
                clear()
                return null
            }
            return TokenCipher.decrypt(encrypted) ?: run { clear(); null }
        }

    val isLoggedIn: Boolean
        get() = token != null

    val username: String
        get() = prefs.getString(KEY_USERNAME, null) ?: "there"

    /** Top-up awaiting confirmation after returning from Stripe Checkout. */
    var pendingPaymentId: Long
        get() = prefs.getLong(KEY_PENDING_PAYMENT, -1L)
        set(value) = prefs.edit().putLong(KEY_PENDING_PAYMENT, value).apply()

    fun clear() {
        prefs.edit().clear().apply()
        // Older builds kept the raw token in plain text here.
        appContext.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
            .edit().clear().apply()
    }

    private companion object {
        const val PREFS = "labalink_session"
        const val LEGACY_PREFS = "labalink_prefs"
        const val KEY_TOKEN = "token"
        const val KEY_EXPIRES_AT = "expires_at"
        const val KEY_USERNAME = "username"
        const val KEY_EMAIL = "email"
        const val KEY_ROLE = "role"
        const val KEY_PENDING_PAYMENT = "pending_payment_id"
    }
}
