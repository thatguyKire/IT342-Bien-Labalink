package edu.cit.bien.labalink.ui.register

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import edu.cit.bien.labalink.R
import edu.cit.bien.labalink.api.RetrofitClient
import edu.cit.bien.labalink.model.RegisterRequest
import edu.cit.bien.labalink.ui.login.LoginActivity
import kotlinx.coroutines.launch
import android.os.Build


class RegisterActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var tvError: TextView
    private lateinit var tvLoginLink: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(
                    android.view.WindowInsets.Type
                        .navigationBars())
                controller.systemBarsBehavior =
                    android.view.WindowInsetsController
                        .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        }

        etUsername = findViewById(R.id.etUsername)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnRegister = findViewById(R.id.btnRegister)
        tvError = findViewById(R.id.tvError)
        tvLoginLink = findViewById(R.id.tvLoginLink)

        btnRegister.setOnClickListener {
            handleRegister()
        }

        tvLoginLink.setOnClickListener {
            startActivity(
                Intent(this, LoginActivity::class.java)
            )
            finish()
        }
    }

    private fun handleRegister() {
        val username = etUsername.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        // Basic validation
        if (username.isEmpty() ||
            email.isEmpty() ||
            password.isEmpty()) {
            showError("Please fill in all fields")
            return
        }

        if (password.length < 8) {
            showError(
                "Password must be at least 8 characters"
            )
            return
        }

        btnRegister.isEnabled = false
        btnRegister.text = "Creating account..."
        hideError()

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.authApi
                    .register(
                        RegisterRequest(
                            username,
                            email,
                            password
                        )
                    )

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@RegisterActivity,
                        "Registration successful! Please login.",
                        Toast.LENGTH_LONG
                    ).show()
                    startActivity(
                        Intent(
                            this@RegisterActivity,
                            LoginActivity::class.java
                        )
                    )
                    finish()
                } else if (response.code() == 409) {
                    showError("Email already exists")
                } else {
                    showError("Registration failed. Try again.")
                }
            } catch (e: Exception) {
                showError("Cannot connect to server. " +
                        "Make sure backend is running.")
            } finally {
                btnRegister.isEnabled = true
                btnRegister.text = "Create Account"
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