package com.example.myapplication.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.util.Log
import android.widget.RemoteViews
import com.example.myapplication.R
import com.example.myapplication.cache.CurriculumCache
import com.example.myapplication.model.CourseInstance
import com.example.myapplication.model.CurriculumResponse
import com.example.myapplication.model.CustomSchedule
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TodayWidget : AppWidgetProvider() {
    
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Log.d("TodayWidget", "onUpdate called, widgetIds: ${appWidgetIds.joinToString()}")
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
    
    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            Log.d("TodayWidget", "updateAppWidget called for widgetId: $appWidgetId")
            val cache = CurriculumCache(context)
            val views = RemoteViews(context.packageName, R.layout.widget_today)
            
            val lastLoginId = cache.getLastLogin()
            Log.d("TodayWidget", "lastLoginId: $lastLoginId")
            
            if (lastLoginId == null) {
                views.setTextViewText(R.id.tv_widget_title, "今日课程")
                views.setTextViewText(R.id.tv_widget_content, "请先登录")
                appWidgetManager.updateAppWidget(appWidgetId, views)
                return
            }
            
            val curriculumData = cache.get(lastLoginId)
            Log.d("TodayWidget", "curriculumData: ${curriculumData != null}")
            
            if (curriculumData == null) {
                views.setTextViewText(R.id.tv_widget_title, "今日课程")
                views.setTextViewText(R.id.tv_widget_content, "无课表数据")
                appWidgetManager.updateAppWidget(appWidgetId, views)
                return
            }
            
            val todayCourses = getTodayCourses(curriculumData)
            Log.d("TodayWidget", "todayCourses count: ${todayCourses.size}")
            
            views.setTextViewText(R.id.tv_widget_title, "今日课程 (${getTodayString()})")
            
            if (todayCourses.isEmpty()) {
                views.setTextViewText(R.id.tv_widget_content, "今天没有课程")
            } else {
                val content = buildString {
                    todayCourses.forEach { course ->
                        val timeStr = getTimeRange(course.periods.first(), course.periods.last())
                        append("${course.course}\n$timeStr ${course.location}\n\n")
                    }
                }.trim()
                Log.d("TodayWidget", "content: $content")
                views.setTextViewText(R.id.tv_widget_content, content)
            }
            
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
        
        private fun getTodayCourses(data: CurriculumResponse): List<CourseInstance> {
            val calendar = Calendar.getInstance()
            val today = calendar.get(Calendar.DAY_OF_WEEK)
            val dayOfWeek = if (today == Calendar.SUNDAY) 7 else today - 1
            
            Log.d("TodayWidget", "dayOfWeek: $dayOfWeek")
            
            val week1Monday = data.week_1_monday?.substring(0, 10)
            Log.d("TodayWidget", "week1Monday: $week1Monday")
            
            if (week1Monday == null) return emptyList()
            
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val startDate = dateFormat.parse(week1Monday)
            Log.d("TodayWidget", "startDate: $startDate")
            
            if (startDate == null) return emptyList()
            
            val todayDate = Date()
            val diffInMillis = todayDate.time - startDate.time
            val diffInDays = diffInMillis / (1000 * 60 * 60 * 24)
            val currentWeek = (diffInDays / 7).toInt() + 1
            
            Log.d("TodayWidget", "currentWeek: $currentWeek, total instances: ${data.instances.size}")
            
            val todayCourses = data.instances.filter { 
                Log.d("TodayWidget", "checking course: ${it.course}, week: ${it.week}, day: ${it.day}")
                it.week == currentWeek && it.day == dayOfWeek 
            }.sortedBy { it.periods.minOrNull() ?: 0 }
            
            return todayCourses
        }
        
        private fun getTodayString(): String {
            val days = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
            val calendar = Calendar.getInstance()
            val today = calendar.get(Calendar.DAY_OF_WEEK)
            val dayIndex = if (today == Calendar.SUNDAY) 6 else today - 2
            return days.getOrElse(dayIndex) { "" }
        }
        
        private fun getTimeRange(startPeriod: Int, endPeriod: Int): String {
            val startTimes = listOf("08:00", "08:55", "10:15", "11:10", "14:00", "14:55", "16:15", "17:10", "19:00", "19:55", "20:50", "21:45")
            val endTimes = listOf("08:45", "09:40", "11:00", "11:55", "14:45", "15:40", "17:00", "17:55", "19:45", "20:40", "21:35", "22:30")
            
            val start = startTimes.getOrElse(startPeriod - 1) { "" }
            val end = endTimes.getOrElse(endPeriod - 1) { "" }
            return "$start-$end"
        }
    }
}
