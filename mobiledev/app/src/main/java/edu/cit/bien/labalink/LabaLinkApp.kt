package edu.cit.bien.labalink

import android.app.Application
import android.content.Intent
import android.os.Handler
import android.os.Looper
import edu.cit.bien.labalink.api.RetrofitClient
import edu.cit.bien.labalink.session.SessionManager
import edu.cit.bien.labalink.ui.login.LoginActivity

class LabaLinkApp : Application() {

    lateinit var session: SessionManager
        private set

    override fun onCreate() {
        super.onCreate()
        session = SessionManager(this)
        RetrofitClient.init(session, ::onSessionExpired)
    }

    /** Token rejected by the server: wipe it and go back to sign-in. */
    private fun onSessionExpired() {
        session.clear()
        Handler(Looper.getMainLooper()).post {
            startActivity(
                Intent(this, LoginActivity::class.java)
                    .putExtra(LoginActivity.EXTRA_SESSION_EXPIRED, true)
                    .addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK))
        }
    }
}

val android.app.Activity.session: SessionManager
    get() = (application as LabaLinkApp).session
