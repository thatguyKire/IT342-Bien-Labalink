package edu.cit.bien.labalink.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.cit.bien.labalink.R
import edu.cit.bien.labalink.model.MachineResponse

class MachineAdapter(
    private var machines: List<MachineResponse>,
    private val onMachineClick:
        (MachineResponse) -> Unit
) : RecyclerView.Adapter<MachineAdapter.ViewHolder>() {

    class ViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {
        val tvMachineIcon: TextView =
            view.findViewById(R.id.tvMachineIcon)
        val tvMachineType: TextView =
            view.findViewById(R.id.tvMachineType)
        val tvMachineName: TextView =
            view.findViewById(R.id.tvMachineName)
        val tvMachineStatus: TextView =
            view.findViewById(R.id.tvMachineStatus)
        val tvHourlyRate: TextView =
            view.findViewById(R.id.tvHourlyRate)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater
            .from(parent.context)
            .inflate(
                R.layout.item_machine,
                parent,
                false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val machine = machines[position]

        holder.tvMachineType.text =
            machine.machineType
        holder.tvMachineName.text =
            machine.machineName
        holder.tvHourlyRate.text =
            "₱%.2f/hr".format(machine.hourlyRate)

        when (machine.machineType) {
            "WASHER" ->
                holder.tvMachineIcon.text = "🫧"
            else ->
                holder.tvMachineIcon.text = "💨"
        }

        when (machine.status) {
            "AVAILABLE" -> {
                holder.tvMachineStatus.text =
                    "Available"
                holder.tvMachineStatus.setTextColor(
                    android.graphics.Color
                        .parseColor("#6B7280"))
                holder.tvMachineStatus
                    .setBackgroundResource(
                        R.drawable.badge_pending)
            }
            "RUNNING" -> {
                holder.tvMachineStatus.text =
                    "Running"
                holder.tvMachineStatus.setTextColor(
                    android.graphics.Color
                        .parseColor("#1D4ED8"))
                holder.tvMachineStatus
                    .setBackgroundResource(
                        R.drawable.badge_active)
            }
            "OUT_OF_ORDER" -> {
                holder.tvMachineStatus.text =
                    "Out of Order"
                holder.tvMachineStatus.setTextColor(
                    android.graphics.Color
                        .parseColor("#B91C1C"))
                holder.tvMachineStatus
                    .setBackgroundResource(
                        R.drawable.badge_cancelled)
            }
        }

        // Click listener on each card
        holder.itemView.setOnClickListener {
            onMachineClick(machine)
        }
    }

    fun updateData(
        newMachines: List<MachineResponse>) {
        machines = newMachines
        notifyDataSetChanged()
    }

    override fun getItemCount() = machines.size
}