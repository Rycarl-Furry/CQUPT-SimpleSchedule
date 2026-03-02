package com.example.myapplication.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.cache.CurriculumCache
import com.example.myapplication.databinding.FragmentExamBinding
import com.example.myapplication.model.Exam
import com.example.myapplication.model.ExamResponse
import com.example.myapplication.model.MakeupExam
import com.example.myapplication.network.NetworkService
import kotlinx.coroutines.launch

class ExamFragment : Fragment() {

    private var _binding: FragmentExamBinding? = null
    private val binding get() = _binding!!
    
    private val networkService = NetworkService()
    private var studentId: String? = null
    private lateinit var cache: CurriculumCache
    
    private val retryHandler = Handler(Looper.getMainLooper())
    private var retryRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExamBinding.inflate(inflater, container, false)
        studentId = arguments?.getString("student_id")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cache = CurriculumCache(requireContext())
        
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        
        if (studentId != null) {
            loadExams(studentId!!)
        } else {
            binding.emptyView.visibility = View.VISIBLE
        }
    }

    private fun loadExams(studentId: String) {
        val cachedData = cache.getExamCache(studentId)
        
        if (cachedData != null) {
            displayExams(cachedData.exams, cachedData.makeup_exams)
            refreshInBackground(studentId)
        } else {
            binding.progressBar.visibility = View.VISIBLE
            binding.emptyView.visibility = View.GONE
            fetchExamsWithRetry(studentId, showLoading = true)
        }
    }

    private fun refreshInBackground(studentId: String) {
        lifecycleScope.launch {
            val result = networkService.fetchExams(studentId)
            result.fold(
                onSuccess = { response ->
                    cache.saveExamCache(studentId, response)
                    if (isDataDifferent(cache.getExamCache(studentId), response)) {
                        binding.examContainer.removeAllViews()
                        displayExams(response.exams, response.makeup_exams)
                        Toast.makeText(requireContext(), "考试安排已更新", Toast.LENGTH_SHORT).show()
                    }
                },
                onFailure = {
                    scheduleRetry(studentId, isBackgroundRefresh = true)
                }
            )
        }
    }

    private fun fetchExamsWithRetry(studentId: String, showLoading: Boolean) {
        if (showLoading) {
            binding.progressBar.visibility = View.VISIBLE
        }
        
        lifecycleScope.launch {
            val result = networkService.fetchExams(studentId)
            binding.progressBar.visibility = View.GONE
            
            result.fold(
                onSuccess = { response ->
                    cache.saveExamCache(studentId, response)
                    if (response.exams.isEmpty() && response.makeup_exams.isEmpty()) {
                        binding.emptyView.visibility = View.VISIBLE
                    } else {
                        displayExams(response.exams, response.makeup_exams)
                    }
                },
                onFailure = { error ->
                    val cachedData = cache.getExamCache(studentId)
                    if (cachedData != null) {
                        displayExams(cachedData.exams, cachedData.makeup_exams)
                        Toast.makeText(
                            requireContext(),
                            "使用缓存数据，${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "加载失败: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        scheduleRetry(studentId, isBackgroundRefresh = false)
                    }
                    binding.emptyView.visibility = if (cachedData == null) View.VISIBLE else View.GONE
                }
            )
        }
    }

    private fun scheduleRetry(studentId: String, isBackgroundRefresh: Boolean) {
        retryRunnable?.let { retryHandler.removeCallbacks(it) }
        
        retryRunnable = Runnable {
            if (isAdded && studentId.isNotEmpty()) {
                fetchExamsWithRetry(studentId, showLoading = !isBackgroundRefresh)
            }
        }
        retryHandler.postDelayed(retryRunnable!!, 10000)
    }

    private fun isDataDifferent(old: ExamResponse?, new: ExamResponse): Boolean {
        if (old == null) return true
        if (old.exams.size != new.exams.size || old.makeup_exams.size != new.makeup_exams.size) return true
        return false
    }

    private fun displayExams(exams: List<Exam>, makeupExams: List<MakeupExam>) {
        binding.examContainer.removeAllViews()
        
        if (exams.isEmpty() && makeupExams.isEmpty()) {
            binding.emptyView.visibility = View.VISIBLE
            return
        }
        
        binding.emptyView.visibility = View.GONE
        
        if (exams.isNotEmpty()) {
            val headerView = createSectionHeader("期末/半期考试")
            binding.examContainer.addView(headerView)
            
            exams.forEach { exam ->
                val examView = createExamView(exam)
                binding.examContainer.addView(examView)
            }
        }
        
        if (makeupExams.isNotEmpty()) {
            val headerView = createSectionHeader("补考安排")
            binding.examContainer.addView(headerView)
            
            makeupExams.forEach { exam ->
                val makeupView = createMakeupExamView(exam)
                binding.examContainer.addView(makeupView)
            }
        }
    }

    private fun createSectionHeader(title: String): View {
        return TextView(requireContext()).apply {
            text = title
            textSize = 14f
            setTextColor(Color.parseColor("#4A90D9"))
            setPadding(dpToPx(4), dpToPx(12), dpToPx(4), dpToPx(8))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
    }

    private fun createExamView(exam: Exam): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
            
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 12f
                setColor(Color.parseColor("#FFFFFF"))
                setStroke(1, Color.parseColor("#E8E8E8"))
            }
            background = drawable
            
            val titleView = TextView(context).apply {
                text = exam.course_name
                textSize = 15f
                setTextColor(Color.parseColor("#333333"))
                setPadding(0, 0, 0, dpToPx(8))
            }
            addView(titleView)
            
            val infoLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            
            val typeView = createInfoChip(exam.exam_type, "#E3F2FD", "#1565C0")
            infoLayout.addView(typeView)
            
            val weekView = createInfoChip(exam.exam_week, "#E8F5E9", "#2E7D32")
            infoLayout.addView(weekView)
            
            addView(infoLayout)
            
            val detailView = TextView(context).apply {
                val weekdayText = getWeekdayText(exam.weekday)
                text = "时间: $weekdayText ${exam.exam_time}\n地点: ${exam.exam_location}\n座位: ${exam.seat}"
                textSize = 13f
                setTextColor(Color.parseColor("#666666"))
                setPadding(0, dpToPx(8), 0, 0)
            }
            addView(detailView)
            
            if (exam.exam_qualification.isNotEmpty()) {
                val qualView = TextView(context).apply {
                    text = "考试资格: ${exam.exam_qualification}"
                    textSize = 12f
                    setTextColor(Color.parseColor("#888888"))
                    setPadding(0, dpToPx(4), 0, 0)
                }
                addView(qualView)
            }
        }
    }

    private fun createMakeupExamView(exam: MakeupExam): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
            
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 12f
                setColor(Color.parseColor("#FFFFFF"))
                setStroke(1, Color.parseColor("#E8E8E8"))
            }
            background = drawable
            
            val titleView = TextView(context).apply {
                text = exam.course_name
                textSize = 15f
                setTextColor(Color.parseColor("#333333"))
                setPadding(0, 0, 0, dpToPx(8))
            }
            addView(titleView)
            
            val infoLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            
            val typeView = createInfoChip("补考", "#FFF3E0", "#E65100")
            infoLayout.addView(typeView)
            
            addView(infoLayout)
            
            val dateText = formatExamDate(exam.exam_date)
            val detailView = TextView(context).apply {
                text = "日期: $dateText\n时间: ${exam.exam_time}\n地点: ${exam.exam_location}\n座位: ${exam.seat}"
                textSize = 13f
                setTextColor(Color.parseColor("#666666"))
                setPadding(0, dpToPx(8), 0, 0)
            }
            addView(detailView)
            
            if (exam.remark.isNotEmpty()) {
                val remarkView = TextView(context).apply {
                    text = "备注: ${exam.remark}"
                    textSize = 12f
                    setTextColor(Color.parseColor("#888888"))
                    setPadding(0, dpToPx(4), 0, 0)
                }
                addView(remarkView)
            }
        }
    }

    private fun createInfoChip(text: String, bgColor: String, textColor: String): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            textSize = 11f
            setTextColor(Color.parseColor(textColor))
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 8f
                setColor(Color.parseColor(bgColor))
            }
            background = drawable
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, dpToPx(8), 0)
            }
        }
    }

    private fun getWeekdayText(weekday: String): String {
        return when (weekday) {
            "1" -> "周一"
            "2" -> "周二"
            "3" -> "周三"
            "4" -> "周四"
            "5" -> "周五"
            "6" -> "周六"
            "7" -> "周日"
            else -> weekday
        }
    }

    private fun formatExamDate(date: String): String {
        return if (date.length == 8) {
            "${date.substring(0, 4)}-${date.substring(4, 6)}-${date.substring(6, 8)}"
        } else {
            date
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        retryRunnable?.let { retryHandler.removeCallbacks(it) }
        _binding = null
    }

    companion object {
        fun newInstance(studentId: String): ExamFragment {
            return ExamFragment().apply {
                arguments = Bundle().apply {
                    putString("student_id", studentId)
                }
            }
        }
    }
}
