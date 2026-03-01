package com.example.myapplication.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.cache.CurriculumCache
import com.example.myapplication.databinding.DialogCourseDetailBinding
import com.example.myapplication.databinding.DialogSportsBinding
import com.example.myapplication.databinding.DialogWeekPickerBinding
import com.example.myapplication.databinding.DialogAddCustomScheduleBinding
import com.example.myapplication.databinding.FragmentScheduleBinding
import com.example.myapplication.model.CourseInstance
import com.example.myapplication.model.CurriculumResponse
import com.example.myapplication.model.SportsRecord
import com.example.myapplication.model.CustomSchedule
import com.example.myapplication.network.NetworkService
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import kotlinx.coroutines.launch

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
    private var isAnimating = false
    private var lastClickTime = 0L
    private val doubleClickThreshold = 300L
    
    private val networkService = NetworkService()
    private lateinit var cache: CurriculumCache
    private var customSchedules: List<CustomSchedule> = emptyList()

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
        cache = CurriculumCache(requireContext())
        customSchedules = cache.getCustomSchedules()

        val curriculumJson = arguments?.getString("curriculum_data")
        if (curriculumJson != null) {
            curriculumData = Gson().fromJson(curriculumJson, CurriculumResponse::class.java)
            maxWeek = curriculumData?.instances?.maxOfOrNull { it.week } ?: 20
            currentWeek = calculateCurrentWeek()
            showCurriculumInfo()
            renderSchedule()
        }

        binding.btnPrevWeek.setOnClickListener {
            if (currentWeek > 1 && !isAnimating) {
                animateWeekChange(false)
            }
        }

        binding.btnNextWeek.setOnClickListener {
            if (currentWeek < maxWeek && !isAnimating) {
                animateWeekChange(true)
            }
        }

        binding.btnSports.setOnClickListener {
            showSportsDialog()
        }

        binding.tvCurrentWeek.setOnClickListener {
            showWeekPickerDialog()
        }

        binding.gridSchedule.setOnTouchListener { _, event ->
            handleSwipeGesture(event)
        }
        
        // 双击添加自定义日程
        binding.gridSchedule.setOnClickListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime < doubleClickThreshold) {
                showAddCustomScheduleDialog()
                lastClickTime = 0L
            } else {
                lastClickTime = currentTime
            }
        }
    }

    private fun showWeekPickerDialog() {
        val dialogBinding = DialogWeekPickerBinding.inflate(layoutInflater)
        
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setBackgroundInsetStart(40)
            .setBackgroundInsetEnd(40)
            .setBackgroundInsetTop(20)
            .setBackgroundInsetBottom(20)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        val weeks = (1..maxWeek).toList()
        val adapter = WeekPickerAdapter(weeks) { }
        
        dialogBinding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        dialogBinding.recyclerView.adapter = adapter
        
        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(dialogBinding.recyclerView)
        
        dialogBinding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val centerView = snapHelper.findSnapView(layoutManager)
                    centerView?.let {
                        val position = layoutManager.getPosition(it)
                        adapter.setSelectedPosition(position)
                    }
                }
            }
        })
        
        val initialPosition = currentWeek - 1
        dialogBinding.recyclerView.post {
            dialogBinding.recyclerView.scrollToPosition(initialPosition)
            adapter.setSelectedPosition(initialPosition)
        }
        
        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        dialogBinding.btnConfirm.setOnClickListener {
            val selectedWeek = adapter.getSelectedWeek()
            if (selectedWeek != currentWeek) {
                jumpToWeek(selectedWeek)
            }
            dialog.dismiss()
        }
        
        dialog.show()
    }

    private fun jumpToWeek(targetWeek: Int) {
        if (isAnimating || targetWeek == currentWeek) return
        
        isAnimating = true
        val gridLayout = binding.gridSchedule
        val direction = if (targetWeek > currentWeek) -1 else 1
        val width = gridLayout.width.toFloat()
        
        gridLayout.animate()
            .translationX(direction * width)
            .alpha(0f)
            .setDuration(250)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    currentWeek = targetWeek
                    updateWeekDisplay()
                    renderSchedule()
                    
                    gridLayout.translationX = -direction * width
                    gridLayout.alpha = 0f
                    
                    gridLayout.animate()
                        .translationX(0f)
                        .alpha(1f)
                        .setDuration(250)
                        .setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                isAnimating = false
                            }
                        })
                        .start()
                }
            })
            .start()
    }

    private fun showSportsDialog() {
        val dialogBinding = DialogSportsBinding.inflate(layoutInflater)
        
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setBackgroundInsetStart(30)
            .setBackgroundInsetEnd(30)
            .setBackgroundInsetTop(20)
            .setBackgroundInsetBottom(20)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        dialogBinding.btnClose.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
        
        loadSportsData(dialogBinding)
    }

    private fun loadSportsData(dialogBinding: DialogSportsBinding) {
        val token = cache.getAccessToken()
        
        if (token == null) {
            dialogBinding.noLoginView.visibility = View.VISIBLE
            dialogBinding.emptyView.visibility = View.GONE
            dialogBinding.progressBar.visibility = View.GONE
            return
        }
        
        dialogBinding.progressBar.visibility = View.VISIBLE
        dialogBinding.noLoginView.visibility = View.GONE
        dialogBinding.emptyView.visibility = View.GONE
        dialogBinding.recordContainer.removeAllViews()
        
        lifecycleScope.launch {
            val result = networkService.fetchSportsResult(token)
            dialogBinding.progressBar.visibility = View.GONE
            
            result.fold(
                onSuccess = { response ->
                    val records = response.data.data.list
                    val totalCount = response.data.data.totalCount
                    
                    if (records.isEmpty()) {
                        dialogBinding.emptyView.visibility = View.VISIBLE
                    } else {
                        displaySportsRecords(dialogBinding, records, totalCount)
                    }
                },
                onFailure = { error ->
                    Toast.makeText(
                        requireContext(),
                        "加载失败: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    dialogBinding.emptyView.visibility = View.VISIBLE
                }
            )
        }
    }

    private fun displaySportsRecords(
        dialogBinding: DialogSportsBinding,
        records: List<SportsRecord>,
        totalCount: Int
    ) {
        var validRunCount = 0
        var validOtherCount = 0
        
        records.forEach { record ->
            if (record.isValid == "1") {
                if (record.sportsType == "1") {
                    validRunCount++
                } else {
                    validOtherCount++
                }
            }
        }
        
        val validTotalCount = validRunCount + validOtherCount
        
        dialogBinding.tvTotalCount.text = validTotalCount.toString()
        dialogBinding.tvRunCount.text = validRunCount.toString()
        dialogBinding.tvOtherCount.text = validOtherCount.toString()
        
        records.forEach { record ->
            val recordView = createSportsRecordView(record)
            dialogBinding.recordContainer.addView(recordView)
        }
    }

    private fun createSportsRecordView(record: SportsRecord): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dpToPx(6))
            }
            
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 10f
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
                textSize = 11f
                setTextColor(Color.parseColor(typeColor))
                setPadding(dpToPx(6), dpToPx(3), dpToPx(6), dpToPx(3))
                val chipDrawable = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 6f
                    setColor(Color.parseColor(if (record.sportsType == "1") "#E8F5E9" else "#FFF3E0"))
                }
                background = chipDrawable
            }
            headerLayout.addView(typeView)
            
            val dateView = TextView(context).apply {
                text = formatSportsDate(record.sportsDateStr)
                textSize = 11f
                setTextColor(Color.parseColor("#888888"))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(dpToPx(6), 0, 0, 0)
                }
            }
            headerLayout.addView(dateView)
            
            val validText = getValidText(record.isValid)
            val validColor = if (record.isValid == "1") "#4CAF50" else "#E53935"
            
            val validView = TextView(context).apply {
                text = validText
                textSize = 11f
                setTextColor(Color.parseColor(validColor))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                gravity = android.view.Gravity.END
            }
            headerLayout.addView(validView)
            
            addView(headerLayout)
            
            val timeText = formatSportsTimeRange(record.sportsStartTime, record.sportsEndTime)
            val timeView = TextView(context).apply {
                text = "时间: $timeText"
                textSize = 13f
                setTextColor(Color.parseColor("#333333"))
                setPadding(0, dpToPx(6), 0, 0)
            }
            addView(timeView)
            
            val placeView = TextView(context).apply {
                text = "地点: ${record.placeName}"
                textSize = 13f
                setTextColor(Color.parseColor("#333333"))
                setPadding(0, dpToPx(3), 0, 0)
            }
            addView(placeView)
            
            if (record.isValid != "1" && record.reason != null) {
                val reasonView = TextView(context).apply {
                    text = "原因: ${getReasonText(record.reason)}"
                    textSize = 11f
                    setTextColor(Color.parseColor("#E53935"))
                    setPadding(0, dpToPx(3), 0, 0)
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

    private fun formatSportsDate(dateStr: String): String {
        return if (dateStr.length == 8) {
            "${dateStr.substring(0, 4)}-${dateStr.substring(4, 6)}-${dateStr.substring(6, 8)}"
        } else {
            dateStr
        }
    }

    private fun formatSportsTimeRange(startTime: String, endTime: String): String {
        val start = startTime.substring(11, minOf(16, startTime.length))
        val end = endTime.substring(11, minOf(16, endTime.length))
        return "$start - $end"
    }

    private fun handleSwipeGesture(event: MotionEvent): Boolean {
        if (isAnimating) return false
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStartX = event.x
                touchStartY = event.y
                return false // 不消费事件，让点击事件能触发
            }
            MotionEvent.ACTION_UP -> {
                val deltaX = event.x - touchStartX
                val deltaY = event.y - touchStartY
                
                if (Math.abs(deltaX) > swipeThreshold && Math.abs(deltaX) > Math.abs(deltaY)) {
                    if (deltaX < 0) {
                        if (currentWeek < maxWeek) {
                            animateWeekChange(true)
                        }
                    } else {
                        if (currentWeek > 1) {
                            animateWeekChange(false)
                        }
                    }
                    return true // 消费滑动事件
                }
            }
        }
        return false
    }

    private fun animateWeekChange(goNext: Boolean) {
        isAnimating = true
        val gridLayout = binding.gridSchedule
        val width = gridLayout.width.toFloat()
        
        val exitDirection = if (goNext) -width else width
        val enterDirection = if (goNext) width else -width
        
        gridLayout.animate()
            .translationX(exitDirection)
            .alpha(0f)
            .setDuration(250)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (goNext) {
                        currentWeek++
                    } else {
                        currentWeek--
                    }
                    updateWeekDisplay()
                    renderSchedule()
                    
                    gridLayout.translationX = enterDirection
                    gridLayout.alpha = 0f
                    
                    gridLayout.animate()
                        .translationX(0f)
                        .alpha(1f)
                        .setDuration(250)
                        .setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                isAnimating = false
                            }
                        })
                        .start()
                }
            })
            .start()
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
        
        // 过滤当前周的自定义日程
        val weekCustomSchedules = customSchedules.filter { schedule ->
            schedule.weeks.isEmpty() || currentWeek in schedule.weeks
        }

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
        
        // 添加自定义日程到映射
        val customScheduleStartMap = mutableMapOf<Pair<Int, Int>, Int>()
        for ((index, schedule) in weekCustomSchedules.withIndex()) {
            val key = schedule.day to schedule.startPeriod
            customScheduleStartMap[key] = index
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
                val customScheduleIndex = customScheduleStartMap[day + 1 to period + 1]
                
                if (courseIndex != null) {
                    val course = weekCourses[courseIndex]
                    val periodCount = course.periods.size

                    for (p in 0 until periodCount) {
                        if (period + p < 12) {
                            occupiedCells[period + p][day] = true
                        }
                    }

                    val courseView = createCourseView(course, periodCount)
                    
                    courseView.setOnTouchListener { _, event ->
                        val handled = handleSwipeGesture(event)
                        if (handled) {
                            true
                        } else {
                            if (event.action == MotionEvent.ACTION_UP) {
                                showCourseDetailDialog(course)
                            }
                            true
                        }
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
                } else if (customScheduleIndex != null) {
                    val schedule = weekCustomSchedules[customScheduleIndex]
                    val periodCount = schedule.endPeriod - schedule.startPeriod + 1

                    for (p in 0 until periodCount) {
                        if (period + p < 12) {
                            occupiedCells[period + p][day] = true
                        }
                    }

                    val scheduleView = createCustomScheduleView(schedule, periodCount)
                    
                    scheduleView.setOnTouchListener { _, event ->
                        val handled = handleSwipeGesture(event)
                        if (handled) {
                            true
                        } else {
                            if (event.action == MotionEvent.ACTION_UP) {
                                showCustomScheduleDetailDialog(schedule)
                            }
                            true
                        }
                    }
                    
                    val layoutParams = GridLayout.LayoutParams(
                        GridLayout.spec(period, periodCount, periodCount.toFloat()),
                        GridLayout.spec(day + 1, 1, 1f)
                    ).apply {
                        width = 0
                        height = 0
                        setMargins(1, 1, 1, 1)
                    }
                    scheduleView.layoutParams = layoutParams
                    gridLayout.addView(scheduleView)
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

    private fun createCustomScheduleView(schedule: CustomSchedule, periodCount: Int): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2))
            gravity = Gravity.CENTER

            // 使用与正常课程相同的颜色
            val color = courseColorMap[schedule.title] ?: run {
                val color = Color.parseColor(courseColors[colorIndex % courseColors.size])
                courseColorMap[schedule.title] = color
                colorIndex++
                color
            }
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 6f
                setColor(color)
            }
            background = drawable

            val nameView = TextView(context).apply {
                text = schedule.title
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

            if (periodCount >= 2 && schedule.location.isNotEmpty()) {
                val locationView = TextView(context).apply {
                    text = schedule.location
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

    private fun showCustomScheduleDetailDialog(schedule: CustomSchedule) {
        val startTime = periodStartTimes.getOrElse(schedule.startPeriod - 1) { "" }
        val endTime = periodEndTimes.getOrElse(schedule.endPeriod - 1) { "" }
        val dayName = dayNames.getOrElse(schedule.day - 1) { "" }

        val timeRange = if (schedule.startPeriod == schedule.endPeriod) {
            "$dayName 第${schedule.startPeriod}节 (${startTime}-${endTime})"
        } else {
            "$dayName 第${schedule.startPeriod}-${schedule.endPeriod}节 (${startTime}-${endTime})"
        }

        val weeksText = if (schedule.weeks.isEmpty()) {
            "所有周"
        } else {
            schedule.weeks.joinToString(", ") { "第${it}周" }
        }

        val dialogBinding = DialogCourseDetailBinding.inflate(layoutInflater)
        
        dialogBinding.tvCourseName.text = schedule.title
        dialogBinding.tvTeacher.text = "自定义"
        dialogBinding.tvLocation.text = schedule.location.ifEmpty { "无" }
        dialogBinding.tvTime.text = timeRange
        dialogBinding.tvType.text = weeksText

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

        // 添加删除按钮
        val deleteButton = TextView(requireContext()).apply {
            text = "删除"
            textSize = 14f
            setTextColor(Color.parseColor("#E53935"))
            setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
            gravity = Gravity.CENTER
            setOnClickListener {
                cache.removeCustomSchedule(schedule.id)
                customSchedules = cache.getCustomSchedules()
                renderSchedule()
                Toast.makeText(requireContext(), "日程已删除", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }
        
        val buttonContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.END
            addView(deleteButton)
        }
        
        (dialogBinding.root as ViewGroup).addView(buttonContainer)

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
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

    private fun showAddCustomScheduleDialog() {
        val dialogBinding = DialogAddCustomScheduleBinding.inflate(layoutInflater)
        
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setBackgroundInsetStart(30)
            .setBackgroundInsetEnd(30)
            .setBackgroundInsetTop(20)
            .setBackgroundInsetBottom(20)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // 设置下拉选项
        val days = arrayOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
        val periods = (1..12).map { "第${it}节" }.toTypedArray()
        
        dialogBinding.etDay.setText(days[0])
        dialogBinding.etDay.setSimpleItems(days)
        dialogBinding.etStartPeriod.setText(periods[0])
        dialogBinding.etStartPeriod.setSimpleItems(periods)
        dialogBinding.etEndPeriod.setText(periods[0])
        dialogBinding.etEndPeriod.setSimpleItems(periods)
        
        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        dialogBinding.btnSave.setOnClickListener {
            val title = dialogBinding.etTitle.text.toString().trim()
            if (title.isEmpty()) {
                Toast.makeText(requireContext(), "请输入日程标题", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val location = dialogBinding.etLocation.text.toString().trim()
            val day = days.indexOf(dialogBinding.etDay.text.toString()) + 1
            val startPeriod = periods.indexOf(dialogBinding.etStartPeriod.text.toString()) + 1
            val endPeriod = periods.indexOf(dialogBinding.etEndPeriod.text.toString()) + 1
            
            if (endPeriod < startPeriod) {
                Toast.makeText(requireContext(), "结束节次不能小于开始节次", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val weeksText = dialogBinding.etWeeks.text.toString().trim()
            val weeks = parseWeeks(weeksText)
            
            val schedule = CustomSchedule(
                title = title,
                location = location,
                day = day,
                startPeriod = startPeriod,
                endPeriod = endPeriod,
                weeks = weeks
            )
            
            cache.addCustomSchedule(schedule)
            customSchedules = cache.getCustomSchedules()
            renderSchedule()
            dialog.dismiss()
            Toast.makeText(requireContext(), "日程已添加", Toast.LENGTH_SHORT).show()
        }
        
        dialog.show()
    }
    
    private fun parseWeeks(text: String): List<Int> {
        if (text.isEmpty()) return emptyList()
        
        val weeks = mutableListOf<Int>()
        val parts = text.split(",")
        
        for (part in parts) {
            val trimmed = part.trim()
            if (trimmed.contains("-")) {
                val range = trimmed.split("-")
                if (range.size == 2) {
                    val start = range[0].trim().toIntOrNull()
                    val end = range[1].trim().toIntOrNull()
                    if (start != null && end != null) {
                        for (w in start..end) {
                            if (w in 1..maxWeek) {
                                weeks.add(w)
                            }
                        }
                    }
                }
            } else {
                val week = trimmed.toIntOrNull()
                if (week != null && week in 1..maxWeek) {
                    weeks.add(week)
                }
            }
        }
        
        return weeks.distinct().sorted()
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
