package edu.cit.bien.labalink.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.cit.bien.labalink.R
import edu.cit.bien.labalink.model.MachineResponse
import edu.cit.bien.labalink.ui.Tone
import edu.cit.bien.labalink.ui.applyTone
import edu.cit.bien.labalink.ui.machineIcon
import edu.cit.bien.labalink.ui.machineLabel
import edu.cit.bien.labalink.ui.machineTone
import edu.cit.bien.labalink.ui.peso
import edu.cit.bien.labalink.ui.showStatus

class MachineAdapter(
    private var machines: List<MachineResponse>,
    private val onMachineClick: (MachineResponse) -> Unit,
) : RecyclerView.Adapter<MachineAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivMachineIcon: ImageView = view.findViewById(R.id.ivMachineIcon)
        val tvMachineType: TextView = view.findViewById(R.id.tvMachineType)
        val tvMachineName: TextView = view.findViewById(R.id.tvMachineName)
        val tvMachineStatus: TextView = view.findViewById(R.id.tvMachineStatus)
        val tvHourlyRate: TextView = view.findViewById(R.id.tvHourlyRate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_machine, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val machine = machines[position]
        val outOfOrder = machine.status == "OUT_OF_ORDER"

        holder.tvMachineType.text = machineLabel(machine.machineType)
        holder.tvMachineName.text = machine.machineName
        holder.tvHourlyRate.text = "${peso(machine.hourlyRate)} / hr"
        holder.ivMachineIcon.setImageResource(machineIcon(machine.machineType))
        holder.ivMachineIcon.applyTone(
            if (outOfOrder) Tone.NEUTRAL else machineTone(machine.machineType))
        holder.tvMachineStatus.showStatus(machine.status)
        holder.itemView.alpha = if (outOfOrder) 0.7f else 1f
        holder.itemView.contentDescription =
            "${machine.machineName}, ${holder.tvMachineStatus.text}"
        holder.itemView.setOnClickListener { onMachineClick(machine) }
    }

    fun updateData(newMachines: List<MachineResponse>) {
        machines = newMachines
        notifyDataSetChanged()
    }

    override fun getItemCount() = machines.size
}
