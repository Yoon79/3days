package com.goodafteryoon.threedays

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.goodafteryoon.threedays.databinding.ItemGoalBinding
import java.util.concurrent.TimeUnit

class GoalAdapter(
    private val onEdit: (GoalItem) -> Unit,
    private val onDelete: (GoalItem) -> Unit
) : ListAdapter<GoalItem, GoalAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<GoalItem>() {
            override fun areItemsTheSame(oldItem: GoalItem, newItem: GoalItem): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: GoalItem, newItem: GoalItem): Boolean = oldItem == newItem
        }

        fun formatDuration(millis: Long): String {
            if (millis <= 0L) return "00:00:00"
            val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(millis)
            val days = totalSeconds / (24 * 3600)
            val hours = (totalSeconds % (24 * 3600)) / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (days > 0) String.format("%d일 %02d:%02d:%02d", days, hours, minutes, seconds)
            else String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }
    }

    inner class VH(val binding: ItemGoalBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemGoalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.binding.textGoal.text = item.text
        val remaining = item.dueEpochMillis - System.currentTimeMillis()
        holder.binding.textCountdown.text = formatDuration(remaining)
        holder.binding.iconEdit.setOnClickListener { onEdit(item) }
        holder.binding.iconDelete.setOnClickListener { onDelete(item) }
    }
}
