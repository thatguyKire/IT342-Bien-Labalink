package edu.cit.bien.labalink.ui.home

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import edu.cit.bien.labalink.R
import edu.cit.bien.labalink.databinding.ActivityMachineDetailBinding
import edu.cit.bien.labalink.ui.applyTone
import edu.cit.bien.labalink.ui.labelOf
import edu.cit.bien.labalink.ui.machineIcon
import edu.cit.bien.labalink.ui.machineLabel
import edu.cit.bien.labalink.ui.machineTone
import edu.cit.bien.labalink.ui.peso
import edu.cit.bien.labalink.ui.riseIn
import edu.cit.bien.labalink.ui.setContentViewEdgeToEdge
import edu.cit.bien.labalink.ui.showStatus

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
        setContentViewEdgeToEdge(
            binding.root, topInset = R.id.topAppBar, bottomInset = R.id.bottomActionArea)

        val machineId = intent.getLongExtra(EXTRA_MACHINE_ID, -1L)
        val machineName = intent.getStringExtra(EXTRA_NAME) ?: "Machine"
        val machineType = intent.getStringExtra(EXTRA_TYPE) ?: "WASHER"
        val status = intent.getStringExtra(EXTRA_STATUS) ?: "AVAILABLE"
        val hourlyRate = intent.getDoubleExtra(EXTRA_RATE, 0.0)
        val sessionPrice = hourlyRate * DEFAULT_DURATION_MINUTES / 60.0

        binding.tvMachineName.text = machineName
        binding.tvMachineType.text = machineLabel(machineType)
        binding.tvMachineType.applyTone(machineTone(machineType))
        binding.tvHourlyRate.text = "${peso(hourlyRate)} / hr"
        binding.ivMachineIcon.setImageResource(machineIcon(machineType))
        binding.tvImageLabel.text =
            if (machineType == "WASHER") "Washing machine" else "Dryer"
        // The QR secret is never sent to customers: they must scan the
        // sticker on the machine itself.
        binding.tvQrToken.text = "Point the camera at the QR code on this machine."
        binding.tvPriceNote.text =
            "${peso(sessionPrice)} for $DEFAULT_DURATION_MINUTES minutes."
        binding.tvStatus.showStatus(status)

        binding.heroCard.clipToOutline = true
        binding.heroCard.riseIn()
        binding.details.riseIn(delayMs = 80)

        binding.btnBack.setOnClickListener { finish() }

        val available = status == "AVAILABLE" && machineId > 0
        binding.btnStartNewWash.isEnabled = available
        binding.btnStartNewWash.text = if (available) {
            "Scan and pay ${peso(sessionPrice)}"
        } else {
            "${labelOf(status)} right now"
        }
        if (!available) {
            binding.tvActionHint.text = "Pick an available machine to start a wash."
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
