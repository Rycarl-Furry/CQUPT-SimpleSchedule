package com.example.myapplication.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
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

class CurrentCourseWidget : AppWidgetProvider() {
    
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
    
    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val cache = CurriculumCache(context)
            val views = RemoteViews(context.packageName, R.layout.widget_current)
            
            val lastLoginId = cache.getLastLogin()
            if (lastLoginId == null) {
                views.setTextViewText(R.id.tv_widget_course_name, "请先登录")
                views.setTextViewText(R.id.tv_widget_time, "")
                views.setTextViewText(R.id.tv_widget_location, "")
                appWidgetManager.updateAppWidget(appWidgetId, views)
                return
            }
            
            val curriculumData = cache.get(lastLoginId)
            if (curriculumData == null) {
                views.setTextViewText(R.id.tv_widget_course_name, "无课表数据")
                views.setTextViewText(R.id.tv_widget_time, "")
                views.setTextViewText(R.id.tv_widget_location, "")
                appWidgetManager.updateAppWidget(appWidgetId, views)
                return
            }
            
            val currentOrNextCourse = getCurrentOrNextCourse(curriculumData)
            
            if (currentOrNextCourse == null) {
                views.setTextViewText(R.id.tv_widget_course_name, "今日课程已结束")
                views.setTextViewText(R.id.tv_widget_time, "")
                views.setTextViewText(R.id.tv_widget_location, "")
            } else {
                views.setTextViewText(R.id.tv_widget_course_name, currentOrNextCourse.course)
                val timeStr = getTimeRange(currentOrNextCourse.periods.first(), currentOrNextCourse.periods.last())
                views.setTextViewText(R.id.tv_widget_time, timeStr)
                views.setTextViewText(R.id.tv_widget_location, currentOrNextCourse.location)
            }
            
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
        
        private fun getCurrentOrNextCourse(data: CurriculumResponse): CourseInstance? {
            val calendar = Calendar.getInstance()
            val today = calendar.get(Calendar.DAY_OF_WEEK)
            val dayOfWeek = if (today == Calendar.SUNDAY) 7 else today - 1
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)
            val currentTime = currentHour * 60 + currentMinute
            
            val week1Monday = data.week_1_monday?.substring(0, 10) ?: return null
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val startDate = dateFormat.parse(week1Monday) ?: return null
            val todayDate = Date()
            val diffInMillis = todayDate.time - startDate.time
            val diffInDays = diffInMillis / (1000 * 60 * 60 * 24)
            val currentWeek = (diffInDays / 7).toInt() + 1
            
            val todayCourses = data.instances.filter { 
                it.week == currentWeek && it.day == dayOfWeek 
            }.sortedBy { it.periods.minOrNull() }
            
            val periodTimes = listOf(
                8 * 60 to 8 * 60 + 45,
                8 * 60 + 55 to 9 * 60 + 40,
                10 * 60 + 15 to 11 * 60,
                11 * 60 + 10 to 11 * 60 + 55,
                14 * 60 to 14 * 60 + 45,
                14 * 60 + 55 to 15 * 60 + 40,
                16 * 60 + 15 to 17 * 60,
                17 * 60 + 10 to 17 * 60 + 55,
                19 * 60 to 19 * 60 + 45,
                19 * 60 + 55 to 20 * 60 + 40,
                20 * 60 + 50 to 21 * 60 + 35,
                21 * 60 + 45 to 22 * 60 + 30
            )
            
            for (course in todayCourses) {
                val startPeriod = course.periods.minOrNull() ?: continue
                val endPeriod = course.periods.maxOrNull() ?: continue
                
                if (startPeriod > 12) continue
                
                val (periodStart, _) = periodTimes[startPeriod - 1]
                
                if (currentTime < periodStart) {
                    return course
                }
                
                val (_, periodEnd) = periodTimes[minOf(endPeriod, 12) - 1]
                if (currentTime in periodStart..periodEnd) {
                    return course
                }
            }
            
            return null
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
