package com.example.kampusride.ui.ticket

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.kampusride.R
import com.example.kampusride.data.model.Booking
import com.example.kampusride.databinding.ItemTicketBinding
import com.example.kampusride.util.DateUtil

class TicketAdapter(
    private val onClick: (Booking) -> Unit
) : ListAdapter<Booking, TicketAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(val binding: ItemTicketBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTicketBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val booking = getItem(position)
        val context = holder.itemView.context
        val done = DateUtil.isPast(booking.travelDate)
        with(holder.binding) {
            tvTicketCode.text = booking.ticketCode
            tvRoute.text = context.getString(R.string.route_format, booking.pickupPoint, booking.destination)
            tvDateTime.text = context.getString(
                R.string.date_time_format, DateUtil.pretty(booking.travelDate), booking.departureTime
            )
            tvSeat.text = context.getString(R.string.ticket_seat_format, booking.seatNumber)
            tvStatus.text = if (done) context.getString(R.string.status_done) else booking.status
            tvStatus.setBackgroundResource(if (done) R.drawable.bg_badge_gray else R.drawable.bg_badge_green)
            tvStatus.setTextColor(context.getColor(if (done) R.color.text_gray else R.color.green))
        }
        holder.itemView.setOnClickListener { onClick(booking) }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Booking>() {
        override fun areItemsTheSame(oldItem: Booking, newItem: Booking) =
            oldItem.bookingId == newItem.bookingId

        override fun areContentsTheSame(oldItem: Booking, newItem: Booking) = oldItem == newItem
    }
}
