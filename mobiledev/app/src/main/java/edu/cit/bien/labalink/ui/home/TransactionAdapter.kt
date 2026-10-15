package edu.cit.bien.labalink.ui.home

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.cit.bien.labalink.R
import edu.cit.bien.labalink.model.PaymentResponse
import edu.cit.bien.labalink.ui.peso

class TransactionAdapter(
    private var transactions: List<PaymentResponse>
) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
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

        holder.tvAmount.text = peso(tx.amount)
        holder.tvReference.text = tx.providerReference ?: "—"
        holder.tvDate.text = (tx.paidAt ?: tx.createdAt).orEmpty().take(10)

        val (label, color, background) = when (tx.status) {
            "SUCCESS" -> Triple("SUCCESS", "#065F46", R.drawable.badge_completed)
            "FAILED" -> Triple("FAILED", "#B91C1C", R.drawable.badge_cancelled)
            else -> Triple("PENDING", "#6B7280", R.drawable.badge_pending)
        }
        holder.tvStatus.text = label
        holder.tvStatus.setTextColor(Color.parseColor(color))
        holder.tvStatus.setBackgroundResource(background)
    }

    fun updateData(newTransactions: List<PaymentResponse>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }

    override fun getItemCount() = transactions.size
}
