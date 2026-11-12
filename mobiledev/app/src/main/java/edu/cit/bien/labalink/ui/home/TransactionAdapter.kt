package edu.cit.bien.labalink.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.cit.bien.labalink.R
import edu.cit.bien.labalink.model.PaymentResponse
import edu.cit.bien.labalink.ui.applyTone
import edu.cit.bien.labalink.ui.formatDate
import edu.cit.bien.labalink.ui.peso
import edu.cit.bien.labalink.ui.showStatus
import edu.cit.bien.labalink.ui.toneOf

class TransactionAdapter(
    private var transactions: List<PaymentResponse>
) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivIcon)
        val tvAmount: TextView = view.findViewById(R.id.tvAmount)
        val tvReference: TextView = view.findViewById(R.id.tvReference)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_transaction, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val tx = transactions[position]

        holder.tvAmount.text =
            if (tx.status == "SUCCESS") "+ ${peso(tx.amount)}" else peso(tx.amount)
        holder.tvReference.text = tx.providerReference ?: "—"
        holder.tvDate.text = (tx.paidAt ?: tx.createdAt)?.let(::formatDate) ?: ""
        holder.tvStatus.showStatus(tx.status)
        holder.ivIcon.setImageResource(
            when (tx.status) {
                "SUCCESS" -> R.drawable.ic_arrow_down_left
                "FAILED" -> R.drawable.ic_circle_x
                else -> R.drawable.ic_hourglass
            })
        holder.ivIcon.applyTone(toneOf(tx.status))
    }

    fun updateData(newTransactions: List<PaymentResponse>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }

    override fun getItemCount() = transactions.size
}
