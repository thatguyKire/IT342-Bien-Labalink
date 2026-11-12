package edu.cit.bien.labalink.ui.home

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
import edu.cit.bien.labalink.session
import edu.cit.bien.labalink.ui.Tab
import edu.cit.bien.labalink.ui.animatePeso
import edu.cit.bien.labalink.ui.openTab
import edu.cit.bien.labalink.ui.riseIn
import edu.cit.bien.labalink.ui.setContentViewEdgeToEdge
import edu.cit.bien.labalink.ui.setupBottomNav
import edu.cit.bien.labalink.ui.startHalo
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    private lateinit var rvMachines: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var tvMachineCount: TextView
    private lateinit var tvAvailableCount: TextView
    private lateinit var tvRunningCount: TextView
    private lateinit var tvWalletBalance: TextView
    private lateinit var adapter: MachineAdapter
    private var firstLoad = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentViewEdgeToEdge(
            R.layout.activity_home, topInset = R.id.topBar, bottomInset = R.id.bottomNav)

        rvMachines = findViewById(R.id.rvMachines)
        emptyState = findViewById(R.id.emptyState)
        tvMachineCount = findViewById(R.id.tvMachineCount)
        tvAvailableCount = findViewById(R.id.tvAvailableCount)
        tvRunningCount = findViewById(R.id.tvRunningCount)
        tvWalletBalance = findViewById(R.id.tvWalletBalance)

        val firstName = session.username.trim().substringBefore(' ')
        if (firstName.isNotEmpty()) {
            findViewById<TextView>(R.id.tvGreeting).text = "Hi, $firstName"
        }
        findViewById<View>(R.id.heroCard).apply {
            clipToOutline = true
            riseIn()
        }
        findViewById<View>(R.id.liveHalo).startHalo()
        findViewById<View>(R.id.walletPill).setOnClickListener {
            openTab(Tab.HOME, Tab.WALLET)
        }

        adapter = MachineAdapter(emptyList()) { machine ->
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

        setupBottomNav(Tab.HOME) {
            if (adapter.itemCount > 0) rvMachines.smoothScrollToPosition(0)
        }
    }

    override fun onResume() {
        super.onResume()
        loadMachines()
        loadWallet()
    }

    private fun loadMachines() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getMachines()
                if (!response.isSuccessful) return@launch
                val machines = response.body().orEmpty()

                // Cards rise in one after another on the first load only.
                if (firstLoad && machines.isNotEmpty()) {
                    rvMachines.scheduleLayoutAnimation()
                    firstLoad = false
                }
                adapter.updateData(machines)
                tvMachineCount.text = "${machines.size} total"
                tvAvailableCount.text =
                    "${machines.count { it.status == "AVAILABLE" }} available"
                tvRunningCount.text =
                    "${machines.count { it.status == "RUNNING" }} running"
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
                RetrofitClient.api.getWallet().body()?.let {
                    tvWalletBalance.animatePeso(it.walletBalance)
                }
            } catch (e: Exception) {
                // keep last value
            }
        }
    }
}
