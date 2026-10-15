package edu.cit.bien.labalink.ui.bookings

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.cit.bien.labalink.R
import edu.cit.bien.labalink.model.BookingResponse
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Tap a PENDING booking to cancel it (wallet is refunded by the server).
 * Uses SimpleDateFormat rather than java.time, which needs API 26 while
 * minSdk is 24 (the old adapter would crash on Android 7).
 */
class BookingAdapter(
    private var bookings: List<BookingResponse>,
    private val onCancelRequested: (BookingResponse) -> Unit
) : RecyclerView.Adapter<BookingAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMachineName: TextView = view.findViewById(R.id.tvMachineName)
        val tvMachineType: TextView = view.findViewById(R.id.tvMachineType)
        val tvMachineIcon: TextView = view.findViewById(R.id.tvMachineIcon)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvStartTime: TextView = view.findViewById(R.id.tvStartTime)
        val tvEndTime: TextView = view.findViewById(R.id.tvEndTime)
        val tvTotalPrice: TextView = view.findViewById(R.id.tvTotalPrice)
    }

    private val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    private val display = SimpleDateFormat("MMM d, hh:mm a", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_booking, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val booking = bookings[position]

        holder.tvMachineName.text = booking.machineName
        holder.tvMachineType.text = booking.durationMinutes
            ?.let { "${booking.machineType} · $it min" }
            ?: booking.machineType
        holder.tvMachineIcon.text =
            if (booking.machineType == "WASHER") "🫧" else "💨"
        holder.tvTotalPrice.text = "₱%.2f".format(booking.totalPrice)
        holder.tvStartTime.text = formatTime(booking.startTime)
        holder.tvEndTime.text = booking.endTime?.let(::formatTime) ?: "—"

        val (color, background) = when (booking.status) {
            "ACTIVE" -> "#1D4ED8" to R.drawable.badge_active
            "COMPLETED" -> "#065F46" to R.drawable.badge_completed
            "CANCELLED" -> "#B91C1C" to R.drawable.badge_cancelled
            else -> "#6B7280" to R.drawable.badge_pending
        }
        holder.tvStatus.text = booking.status
        holder.tvStatus.setTextColor(Color.parseColor(color))
        holder.tvStatus.setBackgroundResource(background)

        holder.itemView.setOnClickListener(
            if (booking.status == "PENDING") {
                View.OnClickListener { onCancelRequested(booking) }
            } else {
                null
            })
    }

    private fun formatTime(value: String): String = try {
        parser.parse(value.take(19))?.let(display::format) ?: value.take(16)
    } catch (e: Exception) {
        value.take(16)
    }

    fun updateData(newBookings: List<BookingResponse>) {
        bookings = newBookings
        notifyDataSetChanged()
    }

    override fun getItemCount() = bookings.size
}
