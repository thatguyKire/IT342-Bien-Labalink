package edu.cit.bien.labalink.ui.register

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import edu.cit.bien.labalink.R
import edu.cit.bien.labalink.api.RetrofitClient
import edu.cit.bien.labalink.api.errorMessage
import edu.cit.bien.labalink.model.RegisterRequest
import edu.cit.bien.labalink.ui.hideNavigationBar
import edu.cit.bien.labalink.ui.login.LoginActivity
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var tvError: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        hideNavigationBar()

        etUsername = findViewById(R.id.etUsername)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnRegister = findViewById(R.id.btnRegister)
        tvError = findViewById(R.id.tvError)

        btnRegister.setOnClickListener { handleRegister() }
        findViewById<TextView>(R.id.tvLoginLink).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun handleRegister() {
        val username = etUsername.text.toString().trim()
        val email = etEmail.text.toString().trim()
        // Passwords are not trimmed: spaces are legitimate characters.
        val password = etPassword.text.toString()

        val problem = when {
            username.isEmpty() || email.isEmpty() || password.isEmpty() ->
                "Please fill in all fields"
            username.length !in 3..50 ->
                "Username must be 3-50 characters"
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                "Enter a valid email address"
            password.length !in 8..72 ->
                "Password must be 8-72 characters"
            password.none { it.isLetter() } || password.none { it.isDigit() } ->
                "Password must contain a letter and a number"
            else -> null
        }
        if (problem != null) {
            showError(problem)
            return
        }

        btnRegister.isEnabled = false
        btnRegister.text = "Creating account..."
        tvError.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.register(
                    RegisterRequest(username, email, password))
                if (response.isSuccessful) {
                    Toast.makeText(
                        this@RegisterActivity,
                        "Registration successful! Please log in.",
                        Toast.LENGTH_LONG
                    ).show()
                    startActivity(
                        Intent(this@RegisterActivity, LoginActivity::class.java))
                    finish()
                } else {
                    showError(response.errorMessage(
                        "Registration failed. Try again."))
                }
            } catch (e: Exception) {
                showError("Cannot connect to server.")
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
}
