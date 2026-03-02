package com.example.myapplication.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.example.myapplication.R

object WidgetUpdater {
    
    fun updateAllWidgets(context: Context) {
        updateTodayWidget(context)
        updateWeekWidget(context)
        updateCurrentCourseWidget(context)
    }
    
    fun updateTodayWidget(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val widgetComponent = ComponentName(context, TodayWidget::class.java)
        val widgetIds = appWidgetManager.getAppWidgetIds(widgetComponent)
        
        for (widgetId in widgetIds) {
            TodayWidget.updateAppWidget(context, appWidgetManager, widgetId)
        }
    }
    
    fun updateWeekWidget(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val widgetComponent = ComponentName(context, WeekWidget::class.java)
        val widgetIds = appWidgetManager.getAppWidgetIds(widgetComponent)
        
        for (widgetId in widgetIds) {
            WeekWidget.updateAppWidget(context, appWidgetManager, widgetId)
        }
    }
    
    fun updateCurrentCourseWidget(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val widgetComponent = ComponentName(context, CurrentCourseWidget::class.java)
        val widgetIds = appWidgetManager.getAppWidgetIds(widgetComponent)
        
        for (widgetId in widgetIds) {
            CurrentCourseWidget.updateAppWidget(context, appWidgetManager, widgetId)
        }
    }
}
