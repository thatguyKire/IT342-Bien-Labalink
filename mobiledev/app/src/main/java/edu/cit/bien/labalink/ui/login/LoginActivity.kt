package edu.cit.bien.labalink.ui.login

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
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
    private lateinit var tvError: TextView
    private lateinit var tvRegisterLink: TextView
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        prefs = getSharedPreferences(
            "labalink_prefs",
            MODE_PRIVATE
        )

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvError = findViewById(R.id.tvError)
        tvRegisterLink = findViewById(R.id.tvRegisterLink)

        btnLogin.setOnClickListener {
            handleLogin()
        }

        tvRegisterLink.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    RegisterActivity::class.java
                )
            )
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
                val response = RetrofitClient.authApi
                    .login(LoginRequest(email, password))

                if (response.isSuccessful) {
                    val body = response.body()!!

                    // Save token and user info
                    prefs.edit()
                        .putString("token", body.accessToken)
                        .putString("username", body.username)
                        .putString("role", body.role)
                        .putString("email", body.email)
                        .apply()

                    startActivity(
                        Intent(
                            this@LoginActivity,
                            DashboardActivity::class.java
                        )
                    )
                    finish()
                } else {
                    showError("Invalid email or password")
                }
            } catch (e: Exception) {
                showError("Cannot connect to server. " +
                        "Make sure backend is running.")
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