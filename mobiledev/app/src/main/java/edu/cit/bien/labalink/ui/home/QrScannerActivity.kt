package edu.cit.bien.labalink.ui.home

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
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
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class QrScannerActivity : AppCompatActivity() {

    private lateinit var barcodeScanner:
            DecoratedBarcodeView
    private lateinit var tvResult: TextView
    private lateinit var prefs: SharedPreferences
    private var isScanning = true
    private var isProcessing = false

    companion object {
        private const val CAMERA_PERMISSION_CODE
                = 100
    }

    override fun onCreate(
        savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scanner)

        hideSystemNavigation()

        prefs = getSharedPreferences(
            "labalink_prefs", MODE_PRIVATE)

        barcodeScanner =
            findViewById(R.id.barcodeScanner)
        tvResult = findViewById(R.id.tvResult)

        findViewById<ImageButton>(R.id.btnCancel)
            .setOnClickListener { finish() }

        findViewById<Button>(R.id.btnCancelBottom)
            .setOnClickListener { finish() }

        checkCameraPermission()
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startScanning()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray) {
        super.onRequestPermissionsResult(
            requestCode, permissions, grantResults)

        if (requestCode ==
            CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] ==
                PackageManager.PERMISSION_GRANTED
            ) {
                startScanning()
            } else {
                Toast.makeText(
                    this,
                    "Camera permission is required " +
                            "to scan QR codes",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }

    private fun startScanning() {
        barcodeScanner.decodeContinuous(
            object : BarcodeCallback {
                override fun barcodeResult(
                    result: BarcodeResult) {
                    if (!isScanning ||
                        isProcessing) return
                    isScanning = false
                    isProcessing = true
                    handleQrResult(result.text)
                }
            }
        )
    }

    private fun handleQrResult(qrToken: String) {
        showResult("Scanning... $qrToken")

        val machineId = intent
            .getLongExtra("machineId", -1L)
        val hourlyRate = intent
            .getDoubleExtra("hourlyRate", 0.0)
        val userId = prefs.getLong("userId", 1L)

        if (machineId == -1L) {
            showResult(
                "Error: No machine selected")
            isScanning = true
            isProcessing = false
            return
        }

        lifecycleScope.launch {
            try {
                val now = LocalDateTime.now()
                val formatter = DateTimeFormatter
                    .ofPattern(
                        "yyyy-MM-dd'T'HH:mm:ss")
                val startTime = now.format(formatter)
                val endTime = now.plusHours(1)
                    .format(formatter)

                val response = RetrofitClient
                    .authApi
                    .createBooking(
                        edu.cit.bien.labalink
                            .model.BookingRequest(
                                userId = userId,
                                machineId = machineId,
                                startTime = startTime,
                                endTime = endTime,
                                totalPrice = hourlyRate
                            )
                    )

                if (response.isSuccessful) {
                    val booking = response.body()!!
                    showResult(
                        "✅ Machine activated! " +
                                "Booking #${booking.id} created")

                    barcodeScanner
                        .postDelayed({
                            val resultIntent =
                                Intent()
                            resultIntent.putExtra(
                                "bookingId",
                                booking.id)
                            setResult(
                                RESULT_OK,
                                resultIntent)
                            finish()
                        }, 2000)
                } else {
                    val errorBody = response
                        .errorBody()
                        ?.string() ?: ""
                    showResult(
                        "❌ Failed: $errorBody")
                    barcodeScanner
                        .postDelayed({
                            isScanning = true
                            isProcessing = false
                        }, 2000)
                }
            } catch (e: Exception) {
                showResult(
                    "❌ Cannot connect to server")
                barcodeScanner.postDelayed({
                    isScanning = true
                    isProcessing = false
                }, 2000)
            }
        }
    }

    private fun showResult(message: String) {
        runOnUiThread {
            tvResult.text = message
            tvResult.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        barcodeScanner.resume()
    }

    override fun onPause() {
        super.onPause()
        barcodeScanner.pause()
    }

    private fun hideSystemNavigation() {
        if (Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.R) {
            window.insetsController?.let {
                    controller ->
                controller.hide(
                    android.view.WindowInsets
                        .Type.navigationBars())
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
    }
}