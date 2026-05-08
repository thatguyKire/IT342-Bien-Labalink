package edu.cit.bien.labalink.ui.home


import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import edu.cit.bien.labalink.R
import edu.cit.bien.labalink.api.RetrofitClient
import kotlinx.coroutines.launch

class WalletActivity : AppCompatActivity() {

    private lateinit var tvBalance: TextView
    private lateinit var tvError: TextView
    private lateinit var tvSuccess: TextView
    private lateinit var etCustomAmount: EditText
    private lateinit var etCardNumber: EditText
    private lateinit var etExpiry: EditText
    private lateinit var etCvc: EditText
    private lateinit var btnTopUp: Button
    private lateinit var tvNoTransactions: TextView
    private lateinit var rvTransactions: RecyclerView
    private lateinit var prefs: SharedPreferences
    private lateinit var adapter: TransactionAdapter

    private var selectedAmount = 100.0
    private var currentClientSecret = ""
    private var currentPaymentIntentId = ""

    override fun onCreate(
        savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet)

        hideSystemNavigation()

        prefs = getSharedPreferences(
            "labalink_prefs", MODE_PRIVATE)

        tvBalance = findViewById(R.id.tvBalance)
        tvError = findViewById(R.id.tvError)
        tvSuccess = findViewById(R.id.tvSuccess)
        etCustomAmount =
            findViewById(R.id.etCustomAmount)
        etCardNumber =
            findViewById(R.id.etCardNumber)
        etExpiry = findViewById(R.id.etExpiry)
        etCvc = findViewById(R.id.etCvc)
        btnTopUp = findViewById(R.id.btnTopUp)
        tvNoTransactions =
            findViewById(R.id.tvNoTransactions)
        rvTransactions =
            findViewById(R.id.rvTransactions)

        adapter = TransactionAdapter(emptyList())
        rvTransactions.layoutManager =
            LinearLayoutManager(this)
        rvTransactions.adapter = adapter

        // Initialize Stripe

        setupQuickAmounts()
        setupBackButton()
        setupTopUpButton()
        loadWalletData()
    }

    private fun setupQuickAmounts() {
        val btn50 =
            findViewById<TextView>(R.id.btn50)
        val btn100 =
            findViewById<TextView>(R.id.btn100)
        val btn200 =
            findViewById<TextView>(R.id.btn200)
        val btn500 =
            findViewById<TextView>(R.id.btn500)

        val buttons = listOf(
            btn50 to 50.0,
            btn100 to 100.0,
            btn200 to 200.0,
            btn500 to 500.0
        )

        buttons.forEach { (btn, amount) ->
            btn.setOnClickListener {
                selectedAmount = amount
                etCustomAmount.setText("")
                buttons.forEach { (b, _) ->
                    b.setBackgroundResource(
                        R.drawable.filter_inactive_bg)
                    b.setTextColor(
                        android.graphics.Color
                            .parseColor("#6B7280"))
                }
                btn.setBackgroundResource(
                    R.drawable.filter_active_bg)
                btn.setTextColor(
                    android.graphics.Color.WHITE)
            }
        }
    }

    private fun setupBackButton() {
        findViewById<ImageButton>(R.id.btnBack)
            .setOnClickListener { finish() }
    }

    private fun setupTopUpButton() {
        btnTopUp.setOnClickListener {
            val customText =
                etCustomAmount.text.toString()
            if (customText.isNotEmpty()) {
                val parsed =
                    customText.toDoubleOrNull()
                if (parsed == null || parsed < 50) {
                    showError(
                        "Minimum amount is ₱50")
                    return@setOnClickListener
                }
                selectedAmount = parsed
            }

            val cardNumber =
                etCardNumber.text.toString()
                    .replace(" ", "")
            val expiry =
                etExpiry.text.toString()
            val cvc = etCvc.text.toString()

            if (cardNumber.isEmpty() ||
                expiry.isEmpty() ||
                cvc.isEmpty()) {
                showError(
                    "Please fill in all card details")
                return@setOnClickListener
            }

            processPayment()
        }
    }

    private fun processPayment() {
        btnTopUp.isEnabled = false
        btnTopUp.text = "Processing..."
        hideError()
        hideSuccess()

        val userId = prefs
            .getLong("userId", 1L)

        lifecycleScope.launch {
            try {
                val intentResponse = RetrofitClient
                    .authApi
                    .createPaymentIntent(
                        mapOf(
                            "userId" to userId,
                            "amount" to selectedAmount
                        )
                    )

                if (intentResponse.isSuccessful) {
                    val body =
                        intentResponse.body()!!
                    currentPaymentIntentId =
                        body.id.toString()

                    val confirmResponse =
                        RetrofitClient.authApi
                            .confirmPayment(
                                mapOf(
                                    "paymentIntentId"
                                            to body.id
                                        .toString()
                                )
                            )

                    if (confirmResponse
                            .isSuccessful) {
                        showSuccess(
                            "Successfully topped up " +
                                    "₱%.2f!".format(
                                        selectedAmount))
                        loadWalletData()
                        clearCardFields()
                    } else {
                        showError(
                            "Payment confirmation failed")
                    }
                } else {
                    showError(
                        "Failed to create payment")
                }
            } catch (e: Exception) {
                showError(
                    "Cannot connect to server")
            } finally {
                btnTopUp.isEnabled = true
                btnTopUp.text =
                    "Top Up via Payment Gateway"
            }
        }
    }

    private fun loadWalletData() {
        val userId = prefs.getLong("userId", 1L)
        val email = prefs
            .getString("email", "") ?: ""

        lifecycleScope.launch {
            try {
                val balanceResponse = RetrofitClient
                    .authApi
                    .getWalletBalance(userId)

                if (balanceResponse.isSuccessful) {
                    val balance = balanceResponse
                        .body()?.walletBalance ?: 0.0
                    tvBalance.text =
                        "₱ %.2f".format(balance)
                    prefs.edit()
                        .putFloat("walletBalance",
                            balance.toFloat())
                        .apply()
                }

                val historyResponse = RetrofitClient
                    .authApi
                    .getPaymentHistory(email)

                if (historyResponse.isSuccessful) {
                    val history =
                        historyResponse.body()
                            ?: emptyList()
                    if (history.isEmpty()) {
                        tvNoTransactions.visibility =
                            View.VISIBLE
                        rvTransactions.visibility =
                            View.GONE
                    } else {
                        tvNoTransactions.visibility =
                            View.GONE
                        rvTransactions.visibility =
                            View.VISIBLE
                        adapter.updateData(history)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun clearCardFields() {
        etCardNumber.setText("")
        etExpiry.setText("")
        etCvc.setText("")
        etCustomAmount.setText("")
    }

    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
        tvSuccess.visibility = View.GONE
    }

    private fun showSuccess(message: String) {
        tvSuccess.text = message
        tvSuccess.visibility = View.VISIBLE
        tvError.visibility = View.GONE
    }

    private fun hideError() {
        tvError.visibility = View.GONE
    }

    private fun hideSuccess() {
        tvSuccess.visibility = View.GONE
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