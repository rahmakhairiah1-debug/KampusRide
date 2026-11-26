package com.example.kampusride.ui.booking

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kampusride.R
import com.example.kampusride.databinding.ItemSeatBinding

/** Grid kursi: tersedia (abu muda), dipilih (biru), terisi (abu-abu, tidak bisa diklik). */
class SeatAdapter(
    private val onSeatClick: (String) -> Unit
) : RecyclerView.Adapter<SeatAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemSeatBinding) : RecyclerView.ViewHolder(binding.root)

    private var seats: List<String> = emptyList()
    private var taken: Set<String> = emptySet()
    private var selected: String? = null

    fun submit(seats: List<String>, taken: Set<String>, selected: String?) {
        this.seats = seats
        this.taken = taken
        this.selected = selected
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSeatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = seats.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val seat = seats[position]
        val view = holder.binding.tvSeatNumber
        val context = view.context
        view.text = seat

        when {
            seat in taken -> {
                view.setBackgroundResource(R.drawable.bg_seat_taken)
                view.setTextColor(context.getColor(R.color.text_gray))
                view.isEnabled = false
            }
            seat == selected -> {
                view.setBackgroundResource(R.drawable.bg_seat_selected)
                view.setTextColor(context.getColor(R.color.white))
                view.isEnabled = true
            }
            else -> {
                view.setBackgroundResource(R.drawable.bg_seat_available)
                view.setTextColor(context.getColor(R.color.text_dark))
                view.isEnabled = true
            }
        }

        view.setOnClickListener {
            if (seat !in taken) {
                selected = seat
                notifyDataSetChanged()
                onSeatClick(seat)
            }
        }
    }
}
