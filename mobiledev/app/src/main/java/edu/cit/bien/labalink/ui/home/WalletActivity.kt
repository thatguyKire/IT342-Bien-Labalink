package edu.cit.bien.labalink.ui.home

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
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
import edu.cit.bien.labalink.api.errorMessage
import edu.cit.bien.labalink.model.TopUpRequest
import edu.cit.bien.labalink.session
import edu.cit.bien.labalink.ui.hideNavigationBar
import edu.cit.bien.labalink.ui.login.LoginActivity
import edu.cit.bien.labalink.ui.peso
import kotlinx.coroutines.launch

/**
 * Wallet top-up through Stripe Checkout.
 *
 * 1. The server creates a Checkout Session and returns its hosted URL.
 * 2. The customer pays on Stripe's page in the browser (the app never
 *    sees card numbers; the old screen collected them in plain EditTexts).
 * 3. Back in the app, we ask the server to confirm with Stripe; the server
 *    credits the wallet exactly once, only if Stripe says it is paid.
 */
class WalletActivity : AppCompatActivity() {

    companion object {
        private const val MIN_TOP_UP = 50.0
        private const val MAX_TOP_UP = 10_000.0
    }

    private lateinit var tvBalance: TextView
    private lateinit var tvError: TextView
    private lateinit var tvSuccess: TextView
    private lateinit var etCustomAmount: EditText
    private lateinit var btnTopUp: Button
    private lateinit var tvNoTransactions: TextView
    private lateinit var rvTransactions: RecyclerView
    private lateinit var adapter: TransactionAdapter

    private var selectedAmount = 100.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet)
        hideNavigationBar()

        tvBalance = findViewById(R.id.tvBalance)
        tvError = findViewById(R.id.tvError)
        tvSuccess = findViewById(R.id.tvSuccess)
        etCustomAmount = findViewById(R.id.etCustomAmount)
        btnTopUp = findViewById(R.id.btnTopUp)
        tvNoTransactions = findViewById(R.id.tvNoTransactions)
        rvTransactions = findViewById(R.id.rvTransactions)

        adapter = TransactionAdapter(emptyList())
        rvTransactions.layoutManager = LinearLayoutManager(this)
        rvTransactions.adapter = adapter

        setupQuickAmounts()
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        btnTopUp.setOnClickListener { startTopUp() }
        findViewById<Button>(R.id.btnLogout).setOnClickListener { logout() }
    }

    override fun onResume() {
        super.onResume()
        val pending = session.pendingPaymentId
        if (pending > 0) confirmPendingPayment(pending) else loadWalletData()
    }

    private fun setupQuickAmounts() {
        val buttons = listOf(
            findViewById<TextView>(R.id.btn50) to 50.0,
            findViewById<TextView>(R.id.btn100) to 100.0,
            findViewById<TextView>(R.id.btn200) to 200.0,
            findViewById<TextView>(R.id.btn500) to 500.0)

        buttons.forEach { (button, amount) ->
            button.setOnClickListener {
                selectedAmount = amount
                etCustomAmount.setText("")
                buttons.forEach { (b, _) ->
                    b.setBackgroundResource(R.drawable.filter_inactive_bg)
                    b.setTextColor(Color.parseColor("#6B7280"))
                }
                button.setBackgroundResource(R.drawable.filter_active_bg)
                button.setTextColor(Color.WHITE)
            }
        }
    }

    private fun startTopUp() {
        val customText = etCustomAmount.text.toString().trim()
        val amount = if (customText.isNotEmpty()) {
            customText.toDoubleOrNull()
        } else {
            selectedAmount
        }
        if (amount == null || amount < MIN_TOP_UP || amount > MAX_TOP_UP) {
            showError("Enter an amount between ₱50 and ₱10,000")
            return
        }

        setBusy(true)
        hideMessages()
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.createCheckout(TopUpRequest(amount))
                val payment = response.body()
                val url = payment?.checkoutUrl
                if (!response.isSuccessful || payment == null || url == null) {
                    showError(response.errorMessage("Could not start payment"))
                    return@launch
                }
                if (!isTrustedCheckoutUrl(url)) {
                    showError("Unexpected payment page. Payment cancelled.")
                    return@launch
                }
                session.pendingPaymentId = payment.id
                openInBrowser(url)
            } catch (e: Exception) {
                showError("Cannot connect to server")
            } finally {
                setBusy(false)
            }
        }
    }

    /** Only ever hand the browser a Stripe-hosted HTTPS page. */
    private fun isTrustedCheckoutUrl(url: String): Boolean {
        val uri = Uri.parse(url)
        val host = uri.host ?: return false
        return uri.scheme == "https" &&
            (host == "checkout.stripe.com" || host.endsWith(".stripe.com"))
    }

    private fun openInBrowser(url: String) {
        try {
            startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .addCategory(Intent.CATEGORY_BROWSABLE))
        } catch (e: ActivityNotFoundException) {
            session.pendingPaymentId = -1L
            showError("No browser available to open the payment page")
        }
    }

    private fun confirmPendingPayment(paymentId: Long) {
        showSuccess("Confirming your payment...")
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.syncPayment(paymentId)
                val payment = response.body()
                when {
                    !response.isSuccessful || payment == null -> {
                        session.pendingPaymentId = -1L
                        hideMessages()
                    }
                    payment.status == "SUCCESS" -> {
                        session.pendingPaymentId = -1L
                        showSuccess("Wallet topped up by ${peso(payment.amount)}!")
                    }
                    payment.status == "FAILED" -> {
                        session.pendingPaymentId = -1L
                        showError("Payment was not completed. No money was taken.")
                    }
                    else -> showSuccess(
                        "Waiting for Stripe to confirm your payment. " +
                            "Pull back to this screen in a moment.")
                }
            } catch (e: Exception) {
                showError("Cannot connect to server")
            } finally {
                loadWalletData()
            }
        }
    }

    private fun loadWalletData() {
        lifecycleScope.launch {
            try {
                RetrofitClient.api.getWallet().body()?.let {
                    tvBalance.text = peso(it.walletBalance)
                }
                val history = RetrofitClient.api.getMyPayments().body().orEmpty()
                adapter.updateData(history)
                tvNoTransactions.visibility =
                    if (history.isEmpty()) View.VISIBLE else View.GONE
                rvTransactions.visibility =
                    if (history.isEmpty()) View.GONE else View.VISIBLE
            } catch (e: Exception) {
                // Offline: keep what is on screen.
            }
        }
    }

    private fun logout() {
        lifecycleScope.launch {
            try {
                RetrofitClient.api.logout() // revoke on the server
            } catch (e: Exception) {
                // Still log out locally.
            }
            session.clear()
            startActivity(
                Intent(this@WalletActivity, LoginActivity::class.java)
                    .addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK))
        }
    }

    private fun setBusy(busy: Boolean) {
        btnTopUp.isEnabled = !busy
        btnTopUp.text = if (busy) "Opening Stripe..." else "Pay securely with Stripe"
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

    private fun hideMessages() {
        tvError.visibility = View.GONE
        tvSuccess.visibility = View.GONE
    }
}
