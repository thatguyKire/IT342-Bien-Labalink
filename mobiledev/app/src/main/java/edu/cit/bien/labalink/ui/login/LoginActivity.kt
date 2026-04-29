package edu.cit.bien.labalink.ui.login

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import edu.cit.bien.labalink.R
import edu.cit.bien.labalink.api.RetrofitClient
import edu.cit.bien.labalink.model.LoginRequest
import edu.cit.bien.labalink.ui.dashboard.DashboardActivity
import edu.cit.bien.labalink.ui.register.RegisterActivity
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnGoogleLogin: Button
    private lateinit var tvError: TextView
    private lateinit var tvRegisterLink: TextView
    private lateinit var prefs: SharedPreferences
    private lateinit var googleSignInClient:
            GoogleSignInClient

    companion object {
        private const val RC_SIGN_IN = 9001
    }

    override fun onCreate(
        savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        prefs = getSharedPreferences(
            "labalink_prefs", MODE_PRIVATE)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnGoogleLogin = findViewById(
            R.id.btnGoogleLogin)
        tvError = findViewById(R.id.tvError)
        tvRegisterLink = findViewById(
            R.id.tvRegisterLink)

        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(
            GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .build()

        googleSignInClient = GoogleSignIn
            .getClient(this, gso)

        btnLogin.setOnClickListener {
            handleLogin()
        }

        btnGoogleLogin.setOnClickListener {
            handleGoogleLogin()
        }

        tvRegisterLink.setOnClickListener {
            startActivity(Intent(
                this,
                RegisterActivity::class.java))
        }
    }

    private fun handleGoogleLogin() {
        val signInIntent = googleSignInClient
            .signInIntent
        startActivityForResult(
            signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?) {
        super.onActivityResult(
            requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn
                .getSignedInAccountFromIntent(data)
            try {
                val account = task
                    .getResult(ApiException::class.java)

                val email = account.email ?: ""
                val name = account.displayName ?: ""

                // Send to our backend
                sendGoogleUserToBackend(email, name)

            } catch (e: ApiException) {
                showError(
                    "Google sign in failed. " +
                            "Try again.")
            }
        }
    }

    private fun sendGoogleUserToBackend(
        email: String,
        name: String) {

        btnGoogleLogin.isEnabled = false
        btnGoogleLogin.text = "Signing in..."

        lifecycleScope.launch {
            try {
                val response = RetrofitClient
                    .authApi
                    .googleLogin(
                        mapOf(
                            "email" to email,
                            "name" to name,
                            "role" to "CUSTOMER"
                        )
                    )

                if (response.isSuccessful) {
                    val body = response.body()!!
                    prefs.edit()
                        .putString("token",
                            body.accessToken)
                        .putString("username",
                            body.username)
                        .putString("role", body.role)
                        .putString("email", body.email)
                        .apply()

                    startActivity(Intent(
                        this@LoginActivity,
                        DashboardActivity::class.java))
                    finish()
                } else {
                    showError(
                        "Google login failed.")
                }
            } catch (e: Exception) {
                showError(
                    "Cannot connect to server.")
            } finally {
                btnGoogleLogin.isEnabled = true
                btnGoogleLogin.text =
                    "Continue with Google"
            }
        }
    }

    private fun handleLogin() {
        val email = etEmail.text
            .toString().trim()
        val password = etPassword.text
            .toString().trim()

        if (email.isEmpty() ||
            password.isEmpty()) {
            showError("Please fill in all fields")
            return
        }

        btnLogin.isEnabled = false
        btnLogin.text = "Logging in..."
        hideError()

        lifecycleScope.launch {
            try {
                val response = RetrofitClient
                    .authApi
                    .login(LoginRequest(
                        email, password))

                if (response.isSuccessful) {
                    val body = response.body()!!
                    prefs.edit()
                        .putString("token",
                            body.accessToken)
                        .putString("username",
                            body.username)
                        .putString("role", body.role)
                        .putString("email", body.email)
                        .apply()

                    startActivity(Intent(
                        this@LoginActivity,
                        DashboardActivity::class.java))
                    finish()
                } else {
                    showError(
                        "Invalid email or password")
                }
            } catch (e: Exception) {
                showError(
                    "Cannot connect to server.")
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