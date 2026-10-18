package edu.cit.bien.labalink.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import edu.cit.bien.labalink.BuildConfig
import edu.cit.bien.labalink.R
import edu.cit.bien.labalink.api.RetrofitClient
import edu.cit.bien.labalink.api.errorMessage
import edu.cit.bien.labalink.model.GoogleTokenRequest
import edu.cit.bien.labalink.model.LoginRequest
import edu.cit.bien.labalink.model.LoginResponse
import edu.cit.bien.labalink.session
import edu.cit.bien.labalink.ui.hideNavigationBar
import edu.cit.bien.labalink.ui.home.HomeActivity
import edu.cit.bien.labalink.ui.register.RegisterActivity
import edu.cit.bien.labalink.ui.showLegalLinks
import kotlinx.coroutines.launch
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SESSION_EXPIRED = "session_expired"
    }

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnGoogleLogin: Button
    private lateinit var tvError: TextView
    private var googleSignInClient: GoogleSignInClient? = null

    @Suppress("DEPRECATION")
    private val googleSignInLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            try {
                val account = GoogleSignIn
                    .getSignedInAccountFromIntent(result.data)
                    .getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken.isNullOrBlank()) {
                    showError("Google sign in failed. Try again.")
                } else {
                    sendGoogleTokenToBackend(idToken)
                }
            } catch (e: ApiException) {
                showError("Google sign in was cancelled.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (session.isLoggedIn) {
            openHome()
            return
        }

        setContentView(R.layout.activity_login)
        hideNavigationBar()

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin)
        tvError = findViewById(R.id.tvError)

        if (intent.getBooleanExtra(EXTRA_SESSION_EXPIRED, false)) {
            showError("Your session expired. Please sign in again.")
        }

        setupGoogleSignIn()
        findViewById<TextView>(R.id.tvLegalNotice)
            .showLegalLinks("By continuing, you agree to our")

        btnLogin.setOnClickListener { handleLogin() }
        findViewById<TextView>(R.id.tvRegisterLink).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    @Suppress("DEPRECATION")
    private fun setupGoogleSignIn() {
        val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        if (webClientId.isBlank()) {
            // Without a client ID Google won't issue an ID token the server
            // can verify, so don't offer the button.
            btnGoogleLogin.visibility = View.GONE
            return
        }
        val options = GoogleSignInOptions
            .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, options)
        btnGoogleLogin.setOnClickListener {
            googleSignInClient?.let {
                // Always show the account picker.
                it.signOut().addOnCompleteListener {
                    googleSignInLauncher.launch(
                        googleSignInClient!!.signInIntent)
                }
            }
        }
    }

    private fun sendGoogleTokenToBackend(idToken: String) {
        setLoading(btnGoogleLogin, true, "Signing in...")
        lifecycleScope.launch {
            try {
                handleLoginResponse(
                    RetrofitClient.api.googleLogin(GoogleTokenRequest(idToken)),
                    "Google sign in failed.")
            } catch (e: Exception) {
                showError("Cannot connect to server.")
            } finally {
                setLoading(btnGoogleLogin, false, "Continue with Google")
            }
        }
    }

    private fun handleLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields")
            return
        }

        hideError()
        setLoading(btnLogin, true, "Logging in...")
        lifecycleScope.launch {
            try {
                handleLoginResponse(
                    RetrofitClient.api.login(LoginRequest(email, password)),
                    "Invalid email or password")
            } catch (e: Exception) {
                showError("Cannot connect to server.")
            } finally {
                setLoading(btnLogin, false, "Login")
            }
        }
    }

    private fun handleLoginResponse(
        response: Response<LoginResponse>,
        fallbackError: String
    ) {
        val body = response.body()
        if (!response.isSuccessful || body == null) {
            showError(response.errorMessage(fallbackError))
            return
        }
        if (body.role != "CUSTOMER") {
            showError("Attendants use the LabaLink web dashboard.")
            return
        }
        session.save(body)
        openHome()
    }

    private fun openHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    private fun setLoading(button: Button, loading: Boolean, label: String) {
        button.isEnabled = !loading
        button.text = label
    }

    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
    }

    private fun hideError() {
        tvError.visibility = View.GONE
    }
}
