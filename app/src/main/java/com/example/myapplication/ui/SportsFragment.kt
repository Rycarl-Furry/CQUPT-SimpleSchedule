package com.example.myapplication.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.R
import com.example.myapplication.cache.CurriculumCache
import com.example.myapplication.databinding.FragmentSportsBinding
import com.example.myapplication.model.SportsRecord
import com.example.myapplication.network.NetworkService
import kotlinx.coroutines.launch

class SportsFragment : Fragment() {

    private var _binding: FragmentSportsBinding? = null
    private val binding get() = _binding!!
    
    private val networkService = NetworkService()
    private lateinit var cache: CurriculumCache

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cache = CurriculumCache(requireContext())
        
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        
        loadSportsData()
    }

    private fun loadSportsData() {
        val token = cache.getAccessToken()
        
        if (token == null) {
            binding.noLoginView.visibility = View.VISIBLE
            binding.emptyView.visibility = View.GONE
            binding.progressBar.visibility = View.GONE
            return
        }
        
        binding.progressBar.visibility = View.VISIBLE
        binding.noLoginView.visibility = View.GONE
        binding.emptyView.visibility = View.GONE
        binding.recordContainer.removeAllViews()
        
        lifecycleScope.launch {
            val result = networkService.fetchSportsResult(token)
            binding.progressBar.visibility = View.GONE
            
            result.fold(
                onSuccess = { response ->
                    val records = response.data.data.list
                    val totalCount = response.data.data.totalCount
                    
                    if (records.isEmpty()) {
                        binding.emptyView.visibility = View.VISIBLE
                    } else {
                        displayRecords(records, totalCount)
                    }
                },
                onFailure = { error ->
                    Toast.makeText(
                        requireContext(),
                        "加载失败: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.emptyView.visibility = View.VISIBLE
                }
            )
        }
    }

    private fun displayRecords(records: List<SportsRecord>, totalCount: Int) {
        var runCount = 0
        var otherCount = 0
        
        records.forEach { record ->
            if (record.sportsType == "1") {
                runCount++
            } else {
                otherCount++
            }
        }
        
        binding.tvTotalCount.text = totalCount.toString()
        binding.tvRunCount.text = runCount.toString()
        binding.tvOtherCount.text = otherCount.toString()
        
        records.forEach { record ->
            val recordView = createRecordView(record)
            binding.recordContainer.addView(recordView)
        }
    }

    private fun createRecordView(record: SportsRecord): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dpToPx(8))
            }
            
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 12f
                setColor(Color.parseColor("#FAFAFA"))
                setStroke(1, Color.parseColor("#E8E8E8"))
            }
            background = drawable
            
            val headerLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
            
            val typeText = getSportsTypeText(record.sportsType)
            val typeColor = if (record.sportsType == "1") "#4CAF50" else "#FF9800"
            
            val typeView = TextView(context).apply {
                text = typeText
                textSize = 12f
                setTextColor(Color.parseColor(typeColor))
                setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
                val chipDrawable = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 8f
                    setColor(Color.parseColor(if (record.sportsType == "1") "#E8F5E9" else "#FFF3E0"))
                }
                background = chipDrawable
            }
            headerLayout.addView(typeView)
            
            val dateView = TextView(context).apply {
                text = formatDate(record.sportsDateStr)
                textSize = 12f
                setTextColor(Color.parseColor("#888888"))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(dpToPx(8), 0, 0, 0)
                }
            }
            headerLayout.addView(dateView)
            
            val validText = getValidText(record.isValid)
            val validColor = if (record.isValid == "1") "#4CAF50" else "#E53935"
            
            val validView = TextView(context).apply {
                text = validText
                textSize = 12f
                setTextColor(Color.parseColor(validColor))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 0)
                }
                gravity = android.view.Gravity.END
            }
            headerLayout.addView(validView)
            
            addView(headerLayout)
            
            val timeText = formatTimeRange(record.sportsStartTime, record.sportsEndTime)
            val timeView = TextView(context).apply {
                text = "时间: $timeText"
                textSize = 14f
                setTextColor(Color.parseColor("#333333"))
                setPadding(0, dpToPx(8), 0, 0)
            }
            addView(timeView)
            
            val placeView = TextView(context).apply {
                text = "地点: ${record.placeName}"
                textSize = 14f
                setTextColor(Color.parseColor("#333333"))
                setPadding(0, dpToPx(4), 0, 0)
            }
            addView(placeView)
            
            if (record.isValid != "1" && record.reason != null) {
                val reasonView = TextView(context).apply {
                    text = "原因: ${getReasonText(record.reason)}"
                    textSize = 12f
                    setTextColor(Color.parseColor("#E53935"))
                    setPadding(0, dpToPx(4), 0, 0)
                }
                addView(reasonView)
            }
        }
    }

    private fun getSportsTypeText(type: String): String {
        return when (type) {
            "1" -> "跑步"
            "2" -> "其他"
            else -> "未知"
        }
    }

    private fun getValidText(valid: String): String {
        return when (valid) {
            "1" -> "有效"
            "2" -> "无效"
            else -> "未知"
        }
    }

    private fun getReasonText(reason: String): String {
        return when (reason) {
            "4" -> "时长无效"
            "5" -> "配速过快"
            "6" -> "配速过慢"
            "7" -> "距离未达标"
            "8" -> "锻炼时间内无跑步检测"
            "9" -> "跑步检测过少"
            "10" -> "未经过所有摄像头组"
            "11" -> "第1组摄像头次数未达标"
            "12" -> "第2组摄像头次数未达标"
            "13" -> "第3组摄像头次数未达标"
            else -> reason
        }
    }

    private fun formatDate(dateStr: String): String {
        return if (dateStr.length == 8) {
            "${dateStr.substring(0, 4)}-${dateStr.substring(4, 6)}-${dateStr.substring(6, 8)}"
        } else {
            dateStr
        }
    }

    private fun formatTimeRange(startTime: String, endTime: String): String {
        val start = startTime.substring(11, minOf(16, startTime.length))
        val end = endTime.substring(11, minOf(16, endTime.length))
        return "$start - $end"
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): SportsFragment {
            return SportsFragment()
        }
    }
}
