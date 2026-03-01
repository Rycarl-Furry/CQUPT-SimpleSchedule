package com.example.myapplication.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R

class WeekPickerAdapter(
    private val weeks: List<Int>,
    private val onSelectedChanged: (Int) -> Unit
) : RecyclerView.Adapter<WeekPickerAdapter.ViewHolder>() {

    private var selectedPosition = 0

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvWeek: TextView = view.findViewById(R.id.tvWeek)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_week_picker, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.tvWeek.text = weeks[position].toString()
        
        val alpha = when {
            position == selectedPosition -> 1.0f
            Math.abs(position - selectedPosition) == 1 -> 0.5f
            else -> 0.3f
        }
        holder.tvWeek.alpha = alpha
        holder.tvWeek.setTextSize(if (position == selectedPosition) 22f else 18f)
    }

    override fun getItemCount(): Int = weeks.size

    fun setSelectedPosition(position: Int) {
        val oldPosition = selectedPosition
        selectedPosition = position
        notifyItemChanged(oldPosition)
        notifyItemChanged(position)
        onSelectedChanged(weeks[position])
    }

    fun getSelectedWeek(): Int = weeks[selectedPosition]
}
