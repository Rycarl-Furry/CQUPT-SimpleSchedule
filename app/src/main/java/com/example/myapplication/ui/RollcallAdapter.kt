package com.example.myapplication.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.model.Rollcall

class RollcallAdapter(
    private val onItemClick: (Rollcall) -> Unit
) : RecyclerView.Adapter<RollcallAdapter.ViewHolder>() {

    private val items = mutableListOf<Rollcall>()

    fun submitList(newItems: List<Rollcall>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun getItems(): List<Rollcall> = items.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_rollcall, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
        holder.itemView.setOnClickListener {
            if (!item.is_checked_in) {
                onItemClick(item)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCourseName: TextView = itemView.findViewById(R.id.tvCourseName)
        private val tvTeacherName: TextView = itemView.findViewById(R.id.tvTeacherName)
        private val tvCheckinType: TextView = itemView.findViewById(R.id.tvCheckinType)
        private val tvCheckinStatus: TextView = itemView.findViewById(R.id.tvCheckinStatus)

        fun bind(item: Rollcall) {
            tvCourseName.text = item.name
            tvTeacherName.text = item.teacher_name

            tvCheckinType.text = when (item.type) {
                "qr" -> "二维码"
                "number" -> "数字"
                "radar" -> "雷达"
                else -> "未知"
            }

            if (item.is_checked_in) {
                tvCheckinStatus.text = "已签到"
                tvCheckinStatus.setTextColor(itemView.context.getColor(android.R.color.holo_green_dark))
                itemView.alpha = 0.6f
            } else {
                tvCheckinStatus.text = "未签到"
                tvCheckinStatus.setTextColor(itemView.context.getColor(android.R.color.holo_red_dark))
                itemView.alpha = 1.0f
            }
        }
    }
}
