package edu.cit.bien.labalink.ui.home

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import edu.cit.bien.labalink.databinding.ActivityMachineDetailBinding
import edu.cit.bien.labalink.ui.hideNavigationBar

class MachineDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MACHINE_ID = "machineId"
        const val EXTRA_NAME = "machineName"
        const val EXTRA_TYPE = "machineType"
        const val EXTRA_STATUS = "machineStatus"
        const val EXTRA_RATE = "hourlyRate"
        const val DEFAULT_DURATION_MINUTES = 60
    }

    private lateinit var binding: ActivityMachineDetailBinding

    private val qrScannerLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val bookingId = result.data
                    ?.getLongExtra(QrScannerActivity.EXTRA_BOOKING_ID, -1L)
                Toast.makeText(
                    this,
                    "Booking #$bookingId started! Machine is now running.",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMachineDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        hideNavigationBar()

        val machineId = intent.getLongExtra(EXTRA_MACHINE_ID, -1L)
        val machineName = intent.getStringExtra(EXTRA_NAME) ?: "Machine"
        val machineType = intent.getStringExtra(EXTRA_TYPE) ?: "WASHER"
        val status = intent.getStringExtra(EXTRA_STATUS) ?: "AVAILABLE"
        val hourlyRate = intent.getDoubleExtra(EXTRA_RATE, 0.0)

        binding.tvMachineName.text = machineName
        binding.tvMachineType.text = machineType
        binding.tvHourlyRate.text = "₱ %.2f / hr".format(hourlyRate)
        binding.tvImageLabel.text =
            if (machineType == "WASHER") "Washing Machine" else "Dryer Machine"
        // The QR secret is no longer sent to customers; they must scan the
        // sticker on the machine itself.
        binding.tvQrToken.text = "Scan the sticker on the machine"

        binding.tvStatus.text = status.replace('_', ' ')
        binding.tvStatus.setTextColor(Color.parseColor(when (status) {
            "AVAILABLE" -> "#10B981"
            "RUNNING" -> "#1D4ED8"
            else -> "#B91C1C"
        }))

        binding.btnBack.setOnClickListener { finish() }

        val available = status == "AVAILABLE" && machineId > 0
        binding.btnStartNewWash.isEnabled = available
        binding.btnStartNewWash.alpha = if (available) 1f else 0.5f
        binding.btnStartNewWash.text = if (available) {
            "Scan & Start · ₱ %.2f / %d min".format(
                hourlyRate * DEFAULT_DURATION_MINUTES / 60.0,
                DEFAULT_DURATION_MINUTES)
        } else {
            "Not available"
        }

        binding.btnStartNewWash.setOnClickListener {
            qrScannerLauncher.launch(
                Intent(this, QrScannerActivity::class.java)
                    .putExtra(QrScannerActivity.EXTRA_MACHINE_ID, machineId)
                    .putExtra(
                        QrScannerActivity.EXTRA_DURATION,
                        DEFAULT_DURATION_MINUTES))
        }
    }
}
