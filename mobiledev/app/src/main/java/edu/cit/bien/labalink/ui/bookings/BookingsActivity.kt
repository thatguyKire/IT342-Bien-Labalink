package edu.cit.bien.labalink.ui.bookings

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import edu.cit.bien.labalink.R
import edu.cit.bien.labalink.api.RetrofitClient
import edu.cit.bien.labalink.api.errorMessage
import edu.cit.bien.labalink.model.BookingResponse
import edu.cit.bien.labalink.ui.Tab
import edu.cit.bien.labalink.ui.peso
import edu.cit.bien.labalink.ui.riseIn
import edu.cit.bien.labalink.ui.setContentViewEdgeToEdge
import edu.cit.bien.labalink.ui.setupBottomNav
import kotlinx.coroutines.launch

class BookingsActivity : AppCompatActivity() {

    private lateinit var rvBookings: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var adapter: BookingAdapter

    private var currentFilter: String? = null
    private var allBookings: List<BookingResponse> = emptyList()
    private var firstLoad = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentViewEdgeToEdge(
            R.layout.activity_bookings, topInset = R.id.topBar, bottomInset = R.id.bottomNav)

        rvBookings = findViewById(R.id.rvBookings)
        emptyState = findViewById(R.id.emptyState)

        adapter = BookingAdapter(emptyList()) { booking -> confirmCancel(booking) }
        rvBookings.layoutManager = LinearLayoutManager(this)
        rvBookings.adapter = adapter

        val filterByChip = mapOf(
            R.id.filterAll to null,
            R.id.filterPending to "PENDING",
            R.id.filterActive to "ACTIVE",
            R.id.filterCompleted to "COMPLETED")
        findViewById<ChipGroup>(R.id.filterRow)
            .setOnCheckedStateChangeListener { _, checkedIds ->
                currentFilter = checkedIds.firstOrNull()?.let(filterByChip::get)
                rvBookings.scheduleLayoutAnimation()
                applyFilter()
            }

        setupBottomNav(Tab.BOOKINGS) {
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
        MaterialAlertDialogBuilder(this)
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
        if (firstLoad && filtered.isNotEmpty()) {
            rvBookings.scheduleLayoutAnimation()
            firstLoad = false
        }
        adapter.updateData(filtered)
        rvBookings.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
        if (filtered.isEmpty() && emptyState.visibility != View.VISIBLE) {
            emptyState.visibility = View.VISIBLE
            emptyState.riseIn()
        } else if (filtered.isNotEmpty()) {
            emptyState.visibility = View.GONE
        }
    }
}
