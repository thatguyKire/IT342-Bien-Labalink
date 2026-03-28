package edu.cit.bien.labalink.ui.dashboard

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import edu.cit.bien.labalink.R
import edu.cit.bien.labalink.ui.login.LoginActivity

class DashboardActivity : AppCompatActivity() {

    private lateinit var tvWelcome: TextView
    private lateinit var tvRole: TextView
    private lateinit var btnLogout: Button
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        prefs = getSharedPreferences(
            "labalink_prefs",
            MODE_PRIVATE
        )

        tvWelcome = findViewById(R.id.tvWelcome)
        tvRole = findViewById(R.id.tvRole)
        btnLogout = findViewById(R.id.btnLogout)

        val username = prefs.getString(
            "username", "User"
        )
        val role = prefs.getString("role", "CUSTOMER")

        tvWelcome.text = "Hello, $username"
        tvRole.text = "Role: $role"

        btnLogout.setOnClickListener {
            prefs.edit().clear().apply()
            startActivity(
                Intent(this, LoginActivity::class.java)
            )
            finish()
        }
    }
}