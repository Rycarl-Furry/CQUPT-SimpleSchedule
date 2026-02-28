package com.example.myapplication.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.myapplication.databinding.DialogCourseDetailBinding
import com.example.myapplication.databinding.FragmentScheduleBinding
import com.example.myapplication.model.CourseInstance
import com.example.myapplication.model.CurriculumResponse
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson

class ScheduleFragment : Fragment() {

    private var _binding: FragmentScheduleBinding? = null
    private val binding get() = _binding!!

    private var curriculumData: CurriculumResponse? = null
    private var currentWeek = 1
    private var maxWeek = 20

    private val courseColors = listOf(
        "#E3F2FD", "#F3E5F5", "#E8F5E9", "#FFF3E0", "#FBE9E7",
        "#F1F8E9", "#E0F7FA", "#F9FBE7", "#FCE4EC", "#E8EAF6"
    )

    private val periodStartTimes = listOf(
        "08:00", "08:55", "10:15", "11:10", "14:00", "14:55",
        "16:15", "17:10", "19:00", "19:55", "20:50", "21:45"
    )

    private val periodEndTimes = listOf(
        "08:45", "09:40", "11:00", "11:55", "14:45", "15:40",
        "17:00", "17:55", "19:45", "20:40", "21:35", "22:30"
    )

    private val dayNames = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

    private var courseColorMap = mutableMapOf<String, Int>()
    private var colorIndex = 0

    private var touchStartX = 0f
    private var touchStartY = 0f
    private val swipeThreshold = 100f

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val curriculumJson = arguments?.getString("curriculum_data")
        if (curriculumJson != null) {
            curriculumData = Gson().fromJson(curriculumJson, CurriculumResponse::class.java)
            maxWeek = curriculumData?.instances?.maxOfOrNull { it.week } ?: 20
            currentWeek = calculateCurrentWeek()
            showCurriculumInfo()
            renderSchedule()
        }

        binding.btnPrevWeek.setOnClickListener {
            if (currentWeek > 1) {
                currentWeek--
                updateWeekDisplay()
                renderSchedule()
            }
        }

        binding.btnNextWeek.setOnClickListener {
            if (currentWeek < maxWeek) {
                currentWeek++
                updateWeekDisplay()
                renderSchedule()
            }
        }

        binding.gridSchedule.setOnTouchListener { _, event ->
            handleSwipeGesture(event)
        }
    }

    private fun handleSwipeGesture(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStartX = event.x
                touchStartY = event.y
                return true
            }
            MotionEvent.ACTION_UP -> {
                val deltaX = event.x - touchStartX
                val deltaY = event.y - touchStartY
                
                if (Math.abs(deltaX) > swipeThreshold && Math.abs(deltaX) > Math.abs(deltaY)) {
                    if (deltaX < 0) {
                        if (currentWeek < maxWeek) {
                            currentWeek++
                            updateWeekDisplay()
                            renderSchedule()
                            showToast("第${currentWeek}周")
                        }
                    } else {
                        if (currentWeek > 1) {
                            currentWeek--
                            updateWeekDisplay()
                            renderSchedule()
                            showToast("第${currentWeek}周")
                        }
                    }
                    return true
                }
            }
        }
        return false
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun calculateCurrentWeek(): Int {
        return try {
            val week1Monday = curriculumData?.week_1_monday?.substring(0, 10) ?: return 1
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val startDate = dateFormat.parse(week1Monday)
            val today = java.util.Date()
            val diffInMillis = today.time - (startDate?.time ?: 0)
            val diffInDays = diffInMillis / (1000 * 60 * 60 * 24)
            val week = (diffInDays / 7).toInt() + 1
            week.coerceIn(1, maxWeek)
        } catch (e: Exception) {
            1
        }
    }

    private fun showCurriculumInfo() {
        curriculumData?.let { curriculum ->
            binding.tvStudentInfo.text = "${curriculum.student_name} (${curriculum.student_id})"
            binding.tvWeekInfo.text = "${curriculum.academic_year} 第${curriculum.semester}学期"
            updateWeekDisplay()
        }
    }

    private fun updateWeekDisplay() {
        binding.tvCurrentWeek.text = "第${currentWeek}周"
    }

    private fun renderSchedule() {
        courseColorMap.clear()
        colorIndex = 0

        val gridLayout = binding.gridSchedule
        gridLayout.removeAllViews()

        val weekCourses = curriculumData?.instances
            ?.filter { it.week == currentWeek }
            ?: emptyList()

        val occupiedCells = Array(12) { BooleanArray(7) { false } }

        val courseStartMap = mutableMapOf<Pair<Int, Int>, Int>()
        for (course in weekCourses) {
            val startPeriod = course.periods.minOrNull() ?: continue
            val key = course.day to startPeriod
            courseStartMap[key] = weekCourses.indexOf(course)

            if (!courseColorMap.containsKey(course.course)) {
                val color = Color.parseColor(courseColors[colorIndex % courseColors.size])
                courseColorMap[course.course] = color
                colorIndex++
            }
        }

        for (period in 0 until 12) {
            val periodView = TextView(requireContext()).apply {
                layoutParams = GridLayout.LayoutParams(
                    GridLayout.spec(period, 1, 1f),
                    GridLayout.spec(0, 1, 0.8f)
                ).apply {
                    width = 0
                    height = 0
                    setMargins(1, 1, 1, 1)
                }
                gravity = Gravity.CENTER
                text = "${period + 1}"
                textSize = 10f
                setTextColor(Color.parseColor("#666666"))
            }
            gridLayout.addView(periodView)

            for (day in 0 until 7) {
                if (occupiedCells[period][day]) continue

                val courseIndex = courseStartMap[day + 1 to period + 1]
                if (courseIndex != null) {
                    val course = weekCourses[courseIndex]
                    val periodCount = course.periods.size

                    for (p in 0 until periodCount) {
                        if (period + p < 12) {
                            occupiedCells[period + p][day] = true
                        }
                    }

                    val courseView = createCourseView(course, periodCount)
                    courseView.setOnClickListener {
                        showCourseDetailDialog(course)
                    }
                    val layoutParams = GridLayout.LayoutParams(
                        GridLayout.spec(period, periodCount, periodCount.toFloat()),
                        GridLayout.spec(day + 1, 1, 1f)
                    ).apply {
                        width = 0
                        height = 0
                        setMargins(1, 1, 1, 1)
                    }
                    courseView.layoutParams = layoutParams
                    gridLayout.addView(courseView)
                } else {
                    val emptyView = View(requireContext()).apply {
                        layoutParams = GridLayout.LayoutParams(
                            GridLayout.spec(period, 1, 1f),
                            GridLayout.spec(day + 1, 1, 1f)
                        ).apply {
                            width = 0
                            height = 0
                            setMargins(1, 1, 1, 1)
                        }
                        val drawable = GradientDrawable().apply {
                            shape = GradientDrawable.RECTANGLE
                            cornerRadius = 4f
                            setColor(Color.parseColor("#F5F5F5"))
                        }
                        background = drawable
                    }
                    gridLayout.addView(emptyView)
                }
            }
        }
    }

    private fun createCourseView(course: CourseInstance, periodCount: Int): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2))
            gravity = Gravity.CENTER

            val color = courseColorMap[course.course] ?: Color.parseColor("#E3F2FD")
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 6f
                setColor(color)
            }
            background = drawable

            val nameView = TextView(context).apply {
                text = course.course
                textSize = if (periodCount >= 2) 9f else 8f
                setTextColor(Color.parseColor("#1565C0"))
                gravity = Gravity.CENTER
                setLines(2)
                maxLines = 2
            }
            addView(nameView, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))

            if (periodCount >= 2) {
                val locationView = TextView(context).apply {
                    text = course.location
                    textSize = 7f
                    setTextColor(Color.parseColor("#666666"))
                    gravity = Gravity.CENTER
                    maxLines = 1
                    setPadding(0, dpToPx(1), 0, 0)
                }
                addView(locationView, LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ))
            }
        }
    }

    private fun showCourseDetailDialog(course: CourseInstance) {
        val startPeriod = course.periods.minOrNull() ?: 1
        val endPeriod = course.periods.maxOrNull() ?: startPeriod
        
        val startTime = periodStartTimes.getOrElse(startPeriod - 1) { "" }
        val endTime = periodEndTimes.getOrElse(endPeriod - 1) { "" }
        val dayName = dayNames.getOrElse(course.day - 1) { "" }

        val timeRange = if (startPeriod == endPeriod) {
            "$dayName 第${startPeriod}节 (${startTime}-${endTime})"
        } else {
            "$dayName 第${startPeriod}-${endPeriod}节 (${startTime}-${endTime})"
        }

        val dialogBinding = DialogCourseDetailBinding.inflate(layoutInflater)
        
        dialogBinding.tvCourseName.text = course.course
        dialogBinding.tvTeacher.text = course.teacher
        dialogBinding.tvLocation.text = course.location
        dialogBinding.tvTime.text = timeRange
        dialogBinding.tvType.text = course.type

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setBackgroundInsetStart(40)
            .setBackgroundInsetEnd(40)
            .setBackgroundInsetTop(20)
            .setBackgroundInsetBottom(20)
            .create()

        dialogBinding.btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(curriculumJson: String): ScheduleFragment {
            return ScheduleFragment().apply {
                arguments = Bundle().apply {
                    putString("curriculum_data", curriculumJson)
                }
            }
        }
    }
}
