package edu.cit.bien.labalink.ui.home

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import edu.cit.bien.labalink.R
import edu.cit.bien.labalink.api.RetrofitClient
import edu.cit.bien.labalink.ui.bookings.BookingsActivity
import edu.cit.bien.labalink.ui.login.LoginActivity
import kotlinx.coroutines.launch
import android.os.Build
import edu.cit.bien.labalink.ui.home.MachineDetailActivity


class HomeActivity : AppCompatActivity() {

    private lateinit var rvMachines: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var tvMachineCount: TextView
    private lateinit var tvWalletBalance: TextView
    private lateinit var adapter: MachineAdapter
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

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

        prefs = getSharedPreferences(
            "labalink_prefs", MODE_PRIVATE)

        rvMachines = findViewById(R.id.rvMachines)
        emptyState = findViewById(R.id.emptyState)
        tvMachineCount =
            findViewById(R.id.tvMachineCount)
        tvWalletBalance =
            findViewById(R.id.tvWalletBalance)

        adapter = MachineAdapter(emptyList()) { machine ->
            val intent = Intent(
                this,
                MachineDetailActivity::class.java)
            intent.putExtra(
                "machineName", machine.machineName)
            intent.putExtra(
                "machineType", machine.machineType)
            intent.putExtra(
                "machineStatus", machine.status)
            intent.putExtra(
                "hourlyRate", machine.hourlyRate)
            intent.putExtra(
                "qrToken", machine.qrToken)
            startActivity(intent)
        }
        rvMachines.layoutManager = GridLayoutManager(this, 2)
        rvMachines.adapter = adapter

        val walletBalance = prefs.getFloat(
            "walletBalance", 0.0f).toDouble()
        tvWalletBalance.text =
            "₱ %.2f".format(walletBalance)

        setupBottomNav()
        loadMachines()
    }

    private fun setupBottomNav() {
        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            if (adapter.itemCount > 0) {
                rvMachines.smoothScrollToPosition(0)
            }
        }

       // findViewById<LinearLayout>(R.id.navMachines).setOnClickListener {
         //   if (adapter.itemCount > 0) {
         //       rvMachines.smoothScrollToPosition(0)
         //   }
        //}

        findViewById<LinearLayout>(R.id.navBookings).setOnClickListener {
            val intent = Intent(this@HomeActivity, edu.cit.bien.labalink.ui.bookings.BookingsActivity::class.java)
            val options = android.app.ActivityOptions.makeCustomAnimation(this@HomeActivity, 0, 0).toBundle()
            startActivity(intent, options)
            finish()
        }


        findViewById<LinearLayout>(R.id.navProfile)
            .setOnClickListener {
                startActivity(Intent(
                    this,
                    WalletActivity::class.java))
            }
    }

    private fun loadMachines() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient
                    .authApi
                    .getAllMachines()

                if (response.isSuccessful) {
                    val machines = response.body()
                        ?: emptyList()
                    adapter.updateData(machines)
                    tvMachineCount.text =
                        "${machines.size} total"

                    if (machines.isEmpty()) {
                        rvMachines.visibility =
                            View.GONE
                        emptyState.visibility =
                            View.VISIBLE
                    } else {
                        rvMachines.visibility =
                            View.VISIBLE
                        emptyState.visibility =
                            View.GONE
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadMachines()
    }


}