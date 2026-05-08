package edu.cit.bien.labalink.ui.bookings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.cit.bien.labalink.R
import edu.cit.bien.labalink.model.BookingResponse
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class BookingAdapter(
    private var bookings: List<BookingResponse>
) : RecyclerView.Adapter<BookingAdapter.ViewHolder>() {

    class ViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {
        val tvMachineName: TextView =
            view.findViewById(R.id.tvMachineName)
        val tvMachineType: TextView =
            view.findViewById(R.id.tvMachineType)
        val tvMachineIcon: TextView =
            view.findViewById(R.id.tvMachineIcon)
        val tvStatus: TextView =
            view.findViewById(R.id.tvStatus)
        val tvStartTime: TextView =
            view.findViewById(R.id.tvStartTime)
        val tvEndTime: TextView =
            view.findViewById(R.id.tvEndTime)
        val tvTotalPrice: TextView =
            view.findViewById(R.id.tvTotalPrice)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int): ViewHolder {
        val view = LayoutInflater.from(
            parent.context)
            .inflate(R.layout.item_booking,
                parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int) {
        val booking = bookings[position]

        holder.tvMachineName.text =
            booking.machineName
        holder.tvMachineType.text =
            booking.machineType

        // Machine icon based on type
        holder.tvMachineIcon.text =
            if (booking.machineType == "WASHER")
                "🫧" else "💨"

        // Format price
        holder.tvTotalPrice.text =
            "₱%.2f".format(booking.totalPrice)

        // Format times
        holder.tvStartTime.text =
            formatTime(booking.startTime)
        holder.tvEndTime.text =
            booking.endTime?.let {
                formatTime(it)
            } ?: "—"

        // Status badge
        setStatusBadge(
            holder.tvStatus, booking.status)
    }

    private fun formatTime(dateStr: String): String {
        return try {
            val formatter = DateTimeFormatter
                .ofPattern("yyyy-MM-dd'T'HH:mm:ss")
            val dateTime = LocalDateTime
                .parse(dateStr, formatter)
            dateTime.format(
                DateTimeFormatter
                    .ofPattern("MMM d, hh:mm a"))
        } catch (e: Exception) {
            dateStr.take(16)
        }
    }

    private fun setStatusBadge(
        tv: TextView,
        status: String) {
        when (status) {
            "PENDING" -> {
                tv.text = "PENDING"
                tv.setTextColor(
                    android.graphics.Color
                        .parseColor("#6B7280"))
                tv.setBackgroundResource(
                    R.drawable.badge_pending)
            }
            "ACTIVE" -> {
                tv.text = "ACTIVE"
                tv.setTextColor(
                    android.graphics.Color
                        .parseColor("#1D4ED8"))
                tv.setBackgroundResource(
                    R.drawable.badge_active)
            }
            "COMPLETED" -> {
                tv.text = "COMPLETED"
                tv.setTextColor(
                    android.graphics.Color
                        .parseColor("#065F46"))
                tv.setBackgroundResource(
                    R.drawable.badge_completed)
            }
            "CANCELLED" -> {
                tv.text = "CANCELLED"
                tv.setTextColor(
                    android.graphics.Color
                        .parseColor("#B91C1C"))
                tv.setBackgroundResource(
                    R.drawable.badge_cancelled)
            }
        }
    }

    fun updateData(
        newBookings: List<BookingResponse>) {
        bookings = newBookings
        notifyDataSetChanged()
    }

    override fun getItemCount() = bookings.size
}