package edu.cit.bien.labalink.ui.home

import android.app.ActivityOptions
import android.content.Intent
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
import edu.cit.bien.labalink.ui.hideNavigationBar
import edu.cit.bien.labalink.ui.peso
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    private lateinit var rvMachines: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var tvMachineCount: TextView
    private lateinit var tvWalletBalance: TextView
    private lateinit var adapter: MachineAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        hideNavigationBar()

        rvMachines = findViewById(R.id.rvMachines)
        emptyState = findViewById(R.id.emptyState)
        tvMachineCount = findViewById(R.id.tvMachineCount)
        tvWalletBalance = findViewById(R.id.tvWalletBalance)

        adapter = MachineAdapter(emptyList()) { machine ->
            // machineId was never passed before, so starting a wash from
            // the detail screen always failed with "No machine selected".
            startActivity(
                Intent(this, MachineDetailActivity::class.java)
                    .putExtra(MachineDetailActivity.EXTRA_MACHINE_ID, machine.id)
                    .putExtra(MachineDetailActivity.EXTRA_NAME, machine.machineName)
                    .putExtra(MachineDetailActivity.EXTRA_TYPE, machine.machineType)
                    .putExtra(MachineDetailActivity.EXTRA_STATUS, machine.status)
                    .putExtra(MachineDetailActivity.EXTRA_RATE, machine.hourlyRate))
        }
        rvMachines.layoutManager = GridLayoutManager(this, 2)
        rvMachines.adapter = adapter

        setupBottomNav()
    }

    override fun onResume() {
        super.onResume()
        loadMachines()
        loadWallet()
    }

    private fun setupBottomNav() {
        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            if (adapter.itemCount > 0) rvMachines.smoothScrollToPosition(0)
        }
        findViewById<LinearLayout>(R.id.navBookings).setOnClickListener {
            startActivity(
                Intent(this, BookingsActivity::class.java),
                ActivityOptions.makeCustomAnimation(this, 0, 0).toBundle())
            finish()
        }
        findViewById<LinearLayout>(R.id.navProfile).setOnClickListener {
            startActivity(Intent(this, WalletActivity::class.java))
        }
    }

    private fun loadMachines() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getMachines()
                if (!response.isSuccessful) return@launch
                val machines = response.body().orEmpty()
                adapter.updateData(machines)
                tvMachineCount.text = "${machines.size} total"
                rvMachines.visibility =
                    if (machines.isEmpty()) View.GONE else View.VISIBLE
                emptyState.visibility =
                    if (machines.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                tvMachineCount.text = "Offline"
            }
        }
    }

    private fun loadWallet() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getWallet()
                response.body()?.let {
                    tvWalletBalance.text = peso(it.walletBalance)
                }
            } catch (e: Exception) {
                // keep last value
            }
        }
    }
}
