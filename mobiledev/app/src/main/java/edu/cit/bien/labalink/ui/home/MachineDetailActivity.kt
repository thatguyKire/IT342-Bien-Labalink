package edu.cit.bien.labalink.ui.home

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import edu.cit.bien.labalink.databinding.ActivityMachineDetailBinding
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts

class MachineDetailActivity : AppCompatActivity() {

    private lateinit var binding:
            ActivityMachineDetailBinding

    private val qrScannerLauncher =
        registerForActivityResult(
            ActivityResultContracts
                .StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val bookingId = result.data
                    ?.getLongExtra("bookingId", -1L)
                android.widget.Toast.makeText(
                    this,
                    "Booking #$bookingId created! " +
                            "Machine is now RUNNING",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }

    override fun onCreate(
        savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMachineDetailBinding
            .inflate(layoutInflater)
        setContentView(binding.root)


        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(android.view.WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        }

        val machineName = intent
            .getStringExtra("machineName")
            ?: "Machine"
        val machineType = intent
            .getStringExtra("machineType")
            ?: "WASHER"
        val machineStatus = intent
            .getStringExtra("machineStatus")
            ?: "AVAILABLE"
        val hourlyRate = intent
            .getDoubleExtra("hourlyRate", 0.0)
        val qrToken = intent
            .getStringExtra("qrToken")
            ?: ""

        binding.tvMachineName.text = machineName
        binding.tvMachineType.text = machineType
        binding.tvQrToken.text = qrToken
        binding.tvHourlyRate.text =
            "₱ %.2f / hr".format(hourlyRate)
        binding.tvStatus.text = machineStatus

        when (machineType) {
            "WASHER" -> {
                binding.tvImageLabel.text =
                    "Washing Machine"
            }
            else -> {
                binding.tvImageLabel.text =
                    "Dryer Machine"
            }
        }

        when (machineStatus) {
            "AVAILABLE" -> {
                binding.tvStatus.setTextColor(
                    android.graphics.Color
                        .parseColor("#10B981"))
            }
            "RUNNING" -> {
                binding.tvStatus.setTextColor(
                    android.graphics.Color
                        .parseColor("#1D4ED8"))
            }
            "OUT_OF_ORDER" -> {
                binding.tvStatus.setTextColor(
                    android.graphics.Color
                        .parseColor("#B91C1C"))
            }
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnStartNewWash.setOnClickListener {
            val machineId = intent
                .getLongExtra("machineId", -1L)
            val hourlyRate = intent
                .getDoubleExtra("hourlyRate", 0.0)

            val scanIntent = Intent(
                this,
                QrScannerActivity::class.java)
            scanIntent.putExtra("machineId", machineId)
            scanIntent.putExtra("hourlyRate", hourlyRate)
            qrScannerLauncher.launch(scanIntent)
        }
    }
}