package edu.cit.bien.labalink.features.login

import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
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
import edu.cit.bien.labalink.R
import edu.cit.bien.labalink.api.RetrofitClient
import edu.cit.bien.labalink.model.LoginRequest
import edu.cit.bien.labalink.features.home.HomeActivity
import edu.cit.bien.labalink.features.register.RegisterActivity
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnGoogleLogin: Button
    private lateinit var tvError: TextView
    private lateinit var tvRegisterLink: TextView
    private lateinit var prefs: SharedPreferences
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val email = account.email ?: ""
                val name = account.displayName ?: ""
                sendGoogleUserToBackend(email, name)
            } catch (e: ApiException) {
                showError("Google sign in failed. Try again.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(android.view.WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        }

        prefs = getSharedPreferences("labalink_prefs", MODE_PRIVATE)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin)
        tvError = findViewById(R.id.tvError)
        tvRegisterLink = findViewById(R.id.tvRegisterLink)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        btnLogin.setOnClickListener {
            handleLogin()
        }

        btnGoogleLogin.setOnClickListener {
            handleGoogleLogin()
        }

        tvRegisterLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun handleGoogleLogin() {
        googleSignInLauncher.launch(googleSignInClient.signInIntent)
    }

    private fun sendGoogleUserToBackend(email: String, name: String) {
        btnGoogleLogin.isEnabled = false
        btnGoogleLogin.text = "Signing in..."

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.authApi.googleLogin(
                    mapOf(
                        "email" to email,
                        "name" to name,
                        "role" to "CUSTOMER"
                    )
                )

                if (response.isSuccessful) {
                    val body = response.body()!!
                    prefs.edit()
                        .putString("token", body.accessToken)
                        .putString("username", body.username)
                        .putString("role", body.role)
                        .putString("email", body.email)
                        // ADDED userId HERE
                        .putLong("userId", body.userId)
                        .apply()

                    startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
                    finish()
                } else {
                    showError("Google login failed.")
                }
            } catch (e: Exception) {
                showError("Cannot connect to server.")
            } finally {
                btnGoogleLogin.isEnabled = true
                btnGoogleLogin.text = "Continue with Google"
            }
        }
    }

    private fun handleLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields")
            return
        }

        btnLogin.isEnabled = false
        btnLogin.text = "Logging in..."
        hideError()

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.authApi.login(LoginRequest(email, password))

                if (response.isSuccessful) {
                    val body = response.body()!!
                    prefs.edit()
                        .putString("token", body.accessToken)
                        .putString("username", body.username)
                        .putString("role", body.role)
                        .putString("email", body.email)
                        // ADDED userId HERE
                        .putLong("userId", body.userId)
                        .apply()

                    startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
                    finish()
                } else {
                    showError("Invalid email or password")
                }
            } catch (e: Exception) {
                showError("Cannot connect to server.")
            } finally {
                btnLogin.isEnabled = true
                btnLogin.text = "Login"
            }
        }
    }

    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
    }

    private fun hideError() {
        tvError.visibility = View.GONE
    }
}