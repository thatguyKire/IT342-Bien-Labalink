package edu.cit.bien.labalink.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.cit.bien.labalink.R
import edu.cit.bien.labalink.model.PaymentHistoryItem

class TransactionAdapter(
    private var transactions: List<PaymentHistoryItem>
) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() { // <-- FIXED HERE

    class ViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {
        val tvAmount: TextView =
            view.findViewById(R.id.tvAmount)
        val tvReference: TextView =
            view.findViewById(R.id.tvReference)
        val tvStatus: TextView =
            view.findViewById(R.id.tvStatus)
        val tvDate: TextView =
            view.findViewById(R.id.tvDate)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater
            .from(parent.context)
            .inflate(
                R.layout.item_transaction,
                parent,
                false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val tx = transactions[position]

        holder.tvAmount.text =
            "₱ %.2f".format(tx.amount)
        holder.tvReference.text =
            tx.providerReference

        when (tx.status) {
            "SUCCESS" -> {
                holder.tvStatus.text = "SUCCESS"
                holder.tvStatus.setTextColor(
                    android.graphics.Color
                        .parseColor("#065F46"))
                holder.tvStatus
                    .setBackgroundResource(
                        R.drawable.badge_completed)
            }
            "FAILED" -> {
                holder.tvStatus.text = "FAILED"
                holder.tvStatus.setTextColor(
                    android.graphics.Color
                        .parseColor("#B91C1C"))
                holder.tvStatus
                    .setBackgroundResource(
                        R.drawable.badge_cancelled)
            }
            else -> {
                holder.tvStatus.text = "PENDING"
                holder.tvStatus.setTextColor(
                    android.graphics.Color
                        .parseColor("#6B7280"))
                holder.tvStatus
                    .setBackgroundResource(
                        R.drawable.badge_pending)
            }
        }

        holder.tvDate.text =
            tx.createdAt.take(10)
    }

    fun updateData(
        newTransactions: List<PaymentHistoryItem>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }

    override fun getItemCount() = transactions.size
}