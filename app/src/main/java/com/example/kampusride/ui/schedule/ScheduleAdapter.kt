package com.example.kampusride.ui.schedule

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.kampusride.R
import com.example.kampusride.data.model.Schedule
import com.example.kampusride.databinding.ItemScheduleBinding
import com.example.kampusride.databinding.ItemScheduleHeaderBinding
import com.example.kampusride.util.DateUtil

sealed class ScheduleListItem {
    data class Header(val text: String) : ScheduleListItem()
    data class Row(val schedule: Schedule) : ScheduleListItem()

    companion object {
        /** Kelompokkan jadwal per tanggal dengan judul tanggal di atasnya. */
        fun group(list: List<Schedule>): List<ScheduleListItem> {
            val result = ArrayList<ScheduleListItem>()
            var lastDate: String? = null
            for (schedule in list) {
                if (schedule.tanggal != lastDate) {
                    result.add(Header(DateUtil.pretty(schedule.tanggal)))
                    lastDate = schedule.tanggal
                }
                result.add(Row(schedule))
            }
            return result
        }

        fun rows(list: List<Schedule>): List<ScheduleListItem> = list.map { Row(it) }
    }
}

class ScheduleAdapter(
    private val onClick: (Schedule) -> Unit
) : ListAdapter<ScheduleListItem, RecyclerView.ViewHolder>(DiffCallback) {

    class RowHolder(val binding: ItemScheduleBinding) : RecyclerView.ViewHolder(binding.root)
    class HeaderHolder(val binding: ItemScheduleHeaderBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int =
        if (getItem(position) is ScheduleListItem.Header) TYPE_HEADER else TYPE_ROW

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderHolder(ItemScheduleHeaderBinding.inflate(inflater, parent, false))
        } else {
            RowHolder(ItemScheduleBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ScheduleListItem.Header -> (holder as HeaderHolder).binding.tvHeader.text = item.text
            is ScheduleListItem.Row -> bindRow(holder as RowHolder, item.schedule)
        }
    }

    private fun bindRow(holder: RowHolder, schedule: Schedule) {
        val context = holder.itemView.context
        val direction = context.getString(
            if (schedule.outbound()) R.string.direction_pergi else R.string.direction_pulang
        )
        with(holder.binding) {
            tvBadge.text = context.getString(R.string.badge_format, schedule.koridor)
            if (schedule.outbound()) {
                tvTitle.text = schedule.wilayahText()
                tvSubtitle.text = context.getString(R.string.subtitle_to, schedule.kampus)
            } else {
                tvTitle.text = schedule.kampus
                tvSubtitle.text = context.getString(R.string.subtitle_to, schedule.wilayahText())
            }
            tvMeta.text = context.getString(R.string.meta_format, schedule.kodeRute, direction)
            tvTime.text = schedule.jamBerangkat
        }
        holder.itemView.setOnClickListener { onClick(schedule) }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ScheduleListItem>() {
        override fun areItemsTheSame(oldItem: ScheduleListItem, newItem: ScheduleListItem): Boolean = when {
            oldItem is ScheduleListItem.Header && newItem is ScheduleListItem.Header ->
                oldItem.text == newItem.text
            oldItem is ScheduleListItem.Row && newItem is ScheduleListItem.Row ->
                oldItem.schedule.scheduleId == newItem.schedule.scheduleId
            else -> false
        }

        override fun areContentsTheSame(oldItem: ScheduleListItem, newItem: ScheduleListItem) =
            oldItem == newItem
    }

    private companion object {
        const val TYPE_HEADER = 0
        const val TYPE_ROW = 1
    }
}
