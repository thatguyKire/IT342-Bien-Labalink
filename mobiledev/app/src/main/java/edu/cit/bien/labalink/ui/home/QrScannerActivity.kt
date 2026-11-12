package edu.cit.bien.labalink.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import edu.cit.bien.labalink.R
import edu.cit.bien.labalink.api.RetrofitClient
import edu.cit.bien.labalink.api.errorMessage
import edu.cit.bien.labalink.model.BookingRequest
import edu.cit.bien.labalink.ui.playIcon
import edu.cit.bien.labalink.ui.riseIn
import edu.cit.bien.labalink.ui.BarBackground
import edu.cit.bien.labalink.ui.setContentViewEdgeToEdge
import edu.cit.bien.labalink.ui.startSweep
import kotlinx.coroutines.launch

/**
 * Scans the machine's QR sticker and sends the scanned code to the server,
 * which checks that it belongs to the selected machine, prices the session
 * and charges the wallet. (The old version ignored the scanned value
 * entirely and sent a price chosen by the app.)
 */
class QrScannerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MACHINE_ID = "machineId"
        const val EXTRA_DURATION = "durationMinutes"
        const val EXTRA_BOOKING_ID = "bookingId"
        private const val CAMERA_PERMISSION_CODE = 100
        private const val MAX_QR_LENGTH = 64
    }

    private enum class ResultKind { WORKING, SUCCESS, ERROR }

    private lateinit var barcodeScanner: DecoratedBarcodeView
    private lateinit var resultCard: View
    private lateinit var ivResultIcon: ImageView
    private lateinit var tvResult: TextView
    private var isProcessing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Camera stays full-bleed; only the overlay bars avoid the system bars.
        setContentViewEdgeToEdge(
            R.layout.activity_qr_scanner,
            topInset = R.id.topBar,
            bottomInset = R.id.bottomControls,
            statusBar = BarBackground.DARK,
            navigationBar = BarBackground.DARK)

        barcodeScanner = findViewById(R.id.barcodeScanner)
        barcodeScanner.setStatusText("")
        resultCard = findViewById(R.id.resultCard)
        ivResultIcon = findViewById(R.id.ivResultIcon)
        tvResult = findViewById(R.id.tvResult)

        findViewById<ImageButton>(R.id.btnCancel).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnCancelBottom).setOnClickListener { finish() }

        val scanLine = findViewById<View>(R.id.scanLine)
        val travel = resources.displayMetrics.density * (260 - 32 - 3)
        scanLine.startSweep(travel)
        findViewById<View>(R.id.bottomControls).riseIn()

        checkCameraPermission()
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startScanning()
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != CAMERA_PERMISSION_CODE) return
        if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            startScanning()
        } else {
            Toast.makeText(
                this,
                "Camera permission is required to scan QR codes",
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    private fun startScanning() {
        barcodeScanner.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult) {
                if (isProcessing) return
                isProcessing = true
                handleQrResult(result.text.orEmpty().trim())
            }
        })
    }

    private fun handleQrResult(qrToken: String) {
        val machineId = intent.getLongExtra(EXTRA_MACHINE_ID, -1L)
        if (machineId <= 0) {
            showResult("No machine selected", ResultKind.ERROR)
            finishDelayed()
            return
        }
        if (qrToken.isEmpty() || qrToken.length > MAX_QR_LENGTH) {
            showResult("That is not a LabaLink QR code", ResultKind.ERROR)
            resumeScanningDelayed()
            return
        }

        showResult("Starting machine…", ResultKind.WORKING)
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.createBooking(
                    BookingRequest(
                        machineId = machineId,
                        qrToken = qrToken,
                        durationMinutes = intent.getIntExtra(EXTRA_DURATION, 60)))

                val booking = response.body()
                if (response.isSuccessful && booking != null) {
                    showResult(
                        "Machine started! Booking #${booking.id}", ResultKind.SUCCESS)
                    barcodeScanner.postDelayed({
                        setResult(
                            RESULT_OK,
                            Intent().putExtra(EXTRA_BOOKING_ID, booking.id))
                        finish()
                    }, 1500)
                } else {
                    showResult(
                        response.errorMessage("Could not start this machine"),
                        ResultKind.ERROR)
                    resumeScanningDelayed()
                }
            } catch (e: Exception) {
                showResult("Cannot connect to server", ResultKind.ERROR)
                resumeScanningDelayed()
            }
        }
    }

    private fun resumeScanningDelayed() {
        barcodeScanner.postDelayed({ isProcessing = false }, 2500)
    }

    private fun finishDelayed() {
        barcodeScanner.postDelayed({ finish() }, 1500)
    }

    private fun showResult(message: String, kind: ResultKind) {
        tvResult.text = message
        ivResultIcon.setImageResource(
            when (kind) {
                ResultKind.WORKING -> R.drawable.ic_hourglass
                ResultKind.SUCCESS -> R.drawable.avd_success
                ResultKind.ERROR -> R.drawable.ic_circle_alert
            })
        resultCard.backgroundTintList = ContextCompat.getColorStateList(
            this,
            when (kind) {
                ResultKind.WORKING -> R.color.white_16
                ResultKind.SUCCESS -> R.color.success
                ResultKind.ERROR -> R.color.danger
            })
        if (resultCard.visibility != View.VISIBLE) {
            resultCard.visibility = View.VISIBLE
            resultCard.riseIn()
        }
        if (kind == ResultKind.SUCCESS) ivResultIcon.playIcon()
    }

    override fun onResume() {
        super.onResume()
        barcodeScanner.resume()
    }

    override fun onPause() {
        super.onPause()
        barcodeScanner.pause()
    }
}
