package edu.cit.bien.labalink.ui.bookings

import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import edu.cit.bien.labalink.R
import edu.cit.bien.labalink.api.RetrofitClient
import edu.cit.bien.labalink.api.errorMessage
import edu.cit.bien.labalink.model.BookingResponse
import edu.cit.bien.labalink.ui.hideNavigationBar
import edu.cit.bien.labalink.ui.home.HomeActivity
import edu.cit.bien.labalink.ui.peso
import kotlinx.coroutines.launch

class BookingsActivity : AppCompatActivity() {

    private lateinit var rvBookings: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var adapter: BookingAdapter
    private lateinit var filters: List<Pair<TextView, String?>>

    private var currentFilter: String? = null
    private var allBookings: List<BookingResponse> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bookings)
        hideNavigationBar()

        rvBookings = findViewById(R.id.rvBookings)
        emptyState = findViewById(R.id.emptyState)

        adapter = BookingAdapter(emptyList()) { booking -> confirmCancel(booking) }
        rvBookings.layoutManager = LinearLayoutManager(this)
        rvBookings.adapter = adapter

        findViewById<View>(R.id.btnBack).setOnClickListener { goHome() }

        filters = listOf(
            findViewById<TextView>(R.id.filterAll) to null,
            findViewById<TextView>(R.id.filterPending) to "PENDING",
            findViewById<TextView>(R.id.filterActive) to "ACTIVE",
            findViewById<TextView>(R.id.filterCompleted) to "COMPLETED")
        filters.forEach { (view, status) ->
            view.setOnClickListener {
                currentFilter = status
                updateFilterUI(view)
                applyFilter()
            }
        }

        findViewById<LinearLayout>(R.id.navHome).setOnClickListener { goHome() }
        findViewById<LinearLayout>(R.id.navBookings).setOnClickListener {
            if (adapter.itemCount > 0) rvBookings.smoothScrollToPosition(0)
        }
    }

    override fun onResume() {
        super.onResume()
        loadBookings()
    }

    /** Only this customer's bookings; the server filters by the token. */
    private fun loadBookings() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getMyBookings()
                if (response.isSuccessful) {
                    allBookings = response.body().orEmpty()
                    applyFilter()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@BookingsActivity,
                    "Cannot connect to server",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun confirmCancel(booking: BookingResponse) {
        AlertDialog.Builder(this)
            .setTitle("Cancel booking?")
            .setMessage(
                "${booking.machineName} will be released and " +
                    "${peso(booking.totalPrice)} returned to your wallet.")
            .setPositiveButton("Cancel booking") { _, _ -> cancel(booking) }
            .setNegativeButton("Keep", null)
            .show()
    }

    private fun cancel(booking: BookingResponse) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.cancelBooking(booking.id)
                val message = if (response.isSuccessful) {
                    "Booking cancelled and refunded"
                } else {
                    response.errorMessage("Could not cancel booking")
                }
                Toast.makeText(this@BookingsActivity, message, Toast.LENGTH_LONG)
                    .show()
                loadBookings()
            } catch (e: Exception) {
                Toast.makeText(
                    this@BookingsActivity,
                    "Cannot connect to server",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun applyFilter() {
        val filtered = currentFilter
            ?.let { status -> allBookings.filter { it.status == status } }
            ?: allBookings
        adapter.updateData(filtered)
        rvBookings.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
        emptyState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun updateFilterUI(active: TextView) {
        filters.forEach { (view, _) ->
            val selected = view == active
            view.setBackgroundResource(
                if (selected) R.drawable.filter_active_bg
                else R.drawable.filter_inactive_bg)
            view.setTextColor(
                if (selected) Color.WHITE else Color.parseColor("#6B7280"))
        }
    }

    private fun goHome() {
        startActivity(
            Intent(this, HomeActivity::class.java),
            ActivityOptions.makeCustomAnimation(this, 0, 0).toBundle())
        finish()
    }
}
