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

class WeekWidget : AppWidgetProvider() {
    
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Log.d("WeekWidget", "onUpdate called, widgetIds: ${appWidgetIds.joinToString()}")
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
    
    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            Log.d("WeekWidget", "updateAppWidget called for widgetId: $appWidgetId")
            val cache = CurriculumCache(context)
            val views = RemoteViews(context.packageName, R.layout.widget_week)
            
            val lastLoginId = cache.getLastLogin()
            Log.d("WeekWidget", "lastLoginId: $lastLoginId")
            
            if (lastLoginId == null) {
                views.setTextViewText(R.id.tv_widget_title, "本周课程")
                views.setTextViewText(R.id.tv_widget_content, "请先登录")
                appWidgetManager.updateAppWidget(appWidgetId, views)
                return
            }
            
            val curriculumData = cache.get(lastLoginId)
            Log.d("WeekWidget", "curriculumData: ${curriculumData != null}")
            
            if (curriculumData == null) {
                views.setTextViewText(R.id.tv_widget_title, "本周课程")
                views.setTextViewText(R.id.tv_widget_content, "无课表数据")
                appWidgetManager.updateAppWidget(appWidgetId, views)
                return
            }
            
            val currentWeek = calculateCurrentWeek(curriculumData)
            Log.d("WeekWidget", "currentWeek: $currentWeek")
            
            val weekCourses = getWeekCourses(curriculumData, currentWeek)
            Log.d("WeekWidget", "weekCourses count: ${weekCourses.size}")
            
            views.setTextViewText(R.id.tv_widget_title, "第${currentWeek}周课程")
            
            if (weekCourses.isEmpty()) {
                views.setTextViewText(R.id.tv_widget_content, "本周没有课程")
            } else {
                val content = buildString {
                    val days = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
                    for (day in 1..7) {
                        val dayCourses = weekCourses.filter { it.day == day }
                        if (dayCourses.isNotEmpty()) {
                            append("${days[day - 1]}:\n")
                            dayCourses.sortedBy { it.periods.minOrNull() }.forEach { course ->
                                append("  ${course.course} (${course.location})\n")
                            }
                            append("\n")
                        }
                    }
                }.trim()
                Log.d("WeekWidget", "content: $content")
                views.setTextViewText(R.id.tv_widget_content, content)
            }
            
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
        
        private fun calculateCurrentWeek(data: CurriculumResponse): Int {
            val week1Monday = data.week_1_monday?.substring(0, 10)
            Log.d("WeekWidget", "week1Monday: $week1Monday")
            
            if (week1Monday == null) return 1
            
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val startDate = dateFormat.parse(week1Monday) ?: return 1
            val today = Date()
            val diffInMillis = today.time - startDate.time
            val diffInDays = diffInMillis / (1000 * 60 * 60 * 24)
            return (diffInDays / 7).toInt() + 1
        }
        
        private fun getWeekCourses(data: CurriculumResponse, week: Int): List<CourseInstance> {
            Log.d("WeekWidget", "getWeekCourses for week: $week, total instances: ${data.instances.size}")
            return data.instances.filter { 
                Log.d("WeekWidget", "checking course: ${it.course}, week: ${it.week}")
                it.week == week 
            }
        }
    }
}
