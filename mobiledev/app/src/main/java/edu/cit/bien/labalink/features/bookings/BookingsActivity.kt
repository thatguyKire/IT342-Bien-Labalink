package edu.cit.bien.labalink.ui.bookings

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import edu.cit.bien.labalink.R
import edu.cit.bien.labalink.api.RetrofitClient
import edu.cit.bien.labalink.model.BookingResponse
import edu.cit.bien.labalink.ui.home.HomeActivity
import kotlinx.coroutines.launch
import android.os.Build

class BookingsActivity : AppCompatActivity() {

    private lateinit var rvBookings: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var adapter: BookingAdapter

    private lateinit var filterAll: TextView
    private lateinit var filterPending: TextView
    private lateinit var filterActive: TextView
    private lateinit var filterCompleted: TextView

    private var currentFilter: String? = null
    private var allBookings: List<BookingResponse> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bookings)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(android.view.WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        }

        // Views
        rvBookings = findViewById(R.id.rvBookings)
        emptyState = findViewById(R.id.emptyState)
        filterAll = findViewById(R.id.filterAll)
        filterPending = findViewById(R.id.filterPending)
        filterActive = findViewById(R.id.filterActive)
        filterCompleted = findViewById(R.id.filterCompleted)

        // Setup RecyclerView
        adapter = BookingAdapter(emptyList())
        rvBookings.layoutManager = LinearLayoutManager(this)
        rvBookings.adapter = adapter

        // Back button
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        // Filter buttons
        filterAll.setOnClickListener {
            currentFilter = null
            updateFilterUI(filterAll)
            applyFilter()
        }
        filterPending.setOnClickListener {
            currentFilter = "PENDING"
            updateFilterUI(filterPending)
            applyFilter()
        }
        filterActive.setOnClickListener {
            currentFilter = "ACTIVE"
            updateFilterUI(filterActive)
            applyFilter()
        }
        filterCompleted.setOnClickListener {
            currentFilter = "COMPLETED"
            updateFilterUI(filterCompleted)
            applyFilter()
        }

        // --- BOTTOM NAV SETUP ---

        // 1. Home Button -> Go to HomeActivity instantly
        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            val intent = Intent(this@BookingsActivity, HomeActivity::class.java)
            val options = android.app.ActivityOptions.makeCustomAnimation(this@BookingsActivity, 0, 0).toBundle()
            startActivity(intent, options)
            finish()
        }

        // 2. Machines Button -> Go to HomeActivity instantly
      //  findViewById<LinearLayout>(R.id.navMachines).setOnClickListener {
          //  val intent = Intent(this@BookingsActivity, HomeActivity::class.java)
          //  val options = android.app.ActivityOptions.makeCustomAnimation(this@BookingsActivity, 0, 0).toBundle()
          //  startActivity(intent, options)
          //  finish()
        //}

        // 3. Bookings Button -> We are already here! Just scroll to top.
        findViewById<LinearLayout>(R.id.navBookings).setOnClickListener {
            if (adapter.itemCount > 0) {
                rvBookings.smoothScrollToPosition(0)
            }
        }
        // Load bookings
        loadBookings()
    }

    private fun loadBookings() {
        lifecycleScope.launch {
            try {
                val email = getSharedPreferences("labalink_prefs", MODE_PRIVATE)
                    .getString("email", "") ?: ""

                val response = RetrofitClient.authApi.getAllBookings()

                if (response.isSuccessful) {
                    allBookings = response.body() ?: emptyList()

                    // Filter by current user email
                    allBookings = allBookings.filter {
                        it.userEmail == email
                    }

                    applyFilter()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun applyFilter() {
        val filtered = if (currentFilter == null) {
            allBookings
        } else {
            allBookings.filter {
                it.status == currentFilter
            }
        }

        adapter.updateData(filtered)

        if (filtered.isEmpty()) {
            rvBookings.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
        } else {
            rvBookings.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
        }
    }

    private fun updateFilterUI(activeFilter: TextView) {
        val filters = listOf(filterAll, filterPending, filterActive, filterCompleted)

        filters.forEach { filter ->
            if (filter == activeFilter) {
                filter.setBackgroundResource(R.drawable.filter_active_bg)
                filter.setTextColor(android.graphics.Color.WHITE)
            } else {
                filter.setBackgroundResource(R.drawable.filter_inactive_bg)
                filter.setTextColor(android.graphics.Color.parseColor("#6B7280"))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadBookings()
    }
}