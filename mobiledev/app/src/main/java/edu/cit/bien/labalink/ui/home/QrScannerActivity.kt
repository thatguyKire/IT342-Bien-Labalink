package edu.cit.bien.labalink.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
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
import edu.cit.bien.labalink.ui.hideNavigationBar
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

    private lateinit var barcodeScanner: DecoratedBarcodeView
    private lateinit var tvResult: TextView
    private var isProcessing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scanner)
        hideNavigationBar()

        barcodeScanner = findViewById(R.id.barcodeScanner)
        tvResult = findViewById(R.id.tvResult)

        findViewById<ImageButton>(R.id.btnCancel).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnCancelBottom).setOnClickListener { finish() }

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
            showResult("No machine selected")
            finishDelayed()
            return
        }
        if (qrToken.isEmpty() || qrToken.length > MAX_QR_LENGTH) {
            showResult("❌ That is not a LabaLink QR code")
            resumeScanningDelayed()
            return
        }

        showResult("Starting machine...")
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.createBooking(
                    BookingRequest(
                        machineId = machineId,
                        qrToken = qrToken,
                        durationMinutes = intent.getIntExtra(EXTRA_DURATION, 60)))

                val booking = response.body()
                if (response.isSuccessful && booking != null) {
                    showResult("✅ Machine started! Booking #${booking.id}")
                    barcodeScanner.postDelayed({
                        setResult(
                            RESULT_OK,
                            Intent().putExtra(EXTRA_BOOKING_ID, booking.id))
                        finish()
                    }, 1500)
                } else {
                    showResult("❌ " + response.errorMessage(
                        "Could not start this machine"))
                    resumeScanningDelayed()
                }
            } catch (e: Exception) {
                showResult("❌ Cannot connect to server")
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

    private fun showResult(message: String) {
        tvResult.text = message
        tvResult.visibility = View.VISIBLE
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
