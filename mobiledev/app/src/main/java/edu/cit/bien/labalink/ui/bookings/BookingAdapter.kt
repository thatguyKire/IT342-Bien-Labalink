package edu.cit.bien.labalink.ui.bookings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.cit.bien.labalink.R
import edu.cit.bien.labalink.model.BookingResponse
import edu.cit.bien.labalink.ui.applyTone
import edu.cit.bien.labalink.ui.formatDateTime
import edu.cit.bien.labalink.ui.machineIcon
import edu.cit.bien.labalink.ui.machineLabel
import edu.cit.bien.labalink.ui.machineTone
import edu.cit.bien.labalink.ui.peso
import edu.cit.bien.labalink.ui.showStatus

/** Tap a PENDING booking to cancel it (wallet is refunded by the server). */
class BookingAdapter(
    private var bookings: List<BookingResponse>,
    private val onCancelRequested: (BookingResponse) -> Unit
) : RecyclerView.Adapter<BookingAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivMachineIcon: ImageView = view.findViewById(R.id.ivMachineIcon)
        val tvMachineName: TextView = view.findViewById(R.id.tvMachineName)
        val tvMachineType: TextView = view.findViewById(R.id.tvMachineType)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvStartTime: TextView = view.findViewById(R.id.tvStartTime)
        val tvEndTime: TextView = view.findViewById(R.id.tvEndTime)
        val tvTotalPrice: TextView = view.findViewById(R.id.tvTotalPrice)
        val cancelHint: View = view.findViewById(R.id.cancelHint)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_booking, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val booking = bookings[position]
        val type = machineLabel(booking.machineType)

        holder.tvMachineName.text = booking.machineName
        holder.tvMachineType.text = booking.durationMinutes
            ?.let { "$type, $it min" }
            ?: type
        holder.ivMachineIcon.setImageResource(machineIcon(booking.machineType))
        holder.ivMachineIcon.applyTone(machineTone(booking.machineType))
        holder.tvTotalPrice.text = peso(booking.totalPrice)
        holder.tvStartTime.text = formatDateTime(booking.startTime)
        holder.tvEndTime.text = booking.endTime?.let(::formatDateTime) ?: "—"
        holder.tvStatus.showStatus(booking.status)

        val cancellable = booking.status == "PENDING"
        holder.cancelHint.visibility = if (cancellable) View.VISIBLE else View.GONE
        holder.itemView.isClickable = cancellable
        holder.itemView.setOnClickListener(
            if (cancellable) View.OnClickListener { onCancelRequested(booking) } else null)
    }

    fun updateData(newBookings: List<BookingResponse>) {
        bookings = newBookings
        notifyDataSetChanged()
    }

    override fun getItemCount() = bookings.size
}
