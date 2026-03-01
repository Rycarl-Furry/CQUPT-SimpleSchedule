package com.example.myapplication.cache

import android.content.Context
import com.example.myapplication.model.CurriculumResponse
import com.example.myapplication.model.ExamResponse
import com.google.gson.Gson

class CurriculumCache(private val context: Context) {
    
    private val prefs = context.getSharedPreferences("curriculum_cache", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    fun save(studentId: String, data: CurriculumResponse) {
        val json = gson.toJson(data)
        prefs.edit()
            .putString("cache_$studentId", json)
            .putLong("time_$studentId", System.currentTimeMillis())
            .apply()
    }
    
    fun get(studentId: String): CurriculumResponse? {
        val json = prefs.getString("cache_$studentId", null) ?: return null
        return try {
            gson.fromJson(json, CurriculumResponse::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    fun getCacheTime(studentId: String): Long {
        return prefs.getLong("time_$studentId", 0)
    }
    
    fun hasCache(studentId: String): Boolean {
        return prefs.contains("cache_$studentId")
    }
    
    fun saveLastLogin(studentId: String) {
        prefs.edit()
            .putString("last_login_student_id", studentId)
            .apply()
    }
    
    fun getLastLogin(): String? {
        return prefs.getString("last_login_student_id", null)
    }
    
    fun saveAccessToken(token: String) {
        prefs.edit()
            .putString("access_token", token)
            .apply()
    }
    
    fun getAccessToken(): String? {
        return prefs.getString("access_token", null)
    }
    
    fun saveAutoLoginCredentials(username: String, password: String) {
        prefs.edit()
            .putString("ids_username", username)
            .putString("ids_password", password)
            .putBoolean("auto_login_enabled", true)
            .apply()
    }
    
    fun getAutoLoginCredentials(): Pair<String, String>? {
        val username = prefs.getString("ids_username", null) ?: return null
        val password = prefs.getString("ids_password", null) ?: return null
        return Pair(username, password)
    }
    
    fun isAutoLoginEnabled(): Boolean {
        return prefs.getBoolean("auto_login_enabled", false)
    }
    
    fun disableAutoLogin() {
        prefs.edit()
            .remove("ids_username")
            .remove("ids_password")
            .putBoolean("auto_login_enabled", false)
            .apply()
    }
    
    fun clearIdsCredentials() {
        prefs.edit()
            .remove("access_token")
            .remove("ids_username")
            .remove("ids_password")
            .putBoolean("auto_login_enabled", false)
            .apply()
    }
    
    fun saveExamCache(studentId: String, data: ExamResponse) {
        val json = gson.toJson(data)
        prefs.edit()
            .putString("exam_cache_$studentId", json)
            .putLong("exam_time_$studentId", System.currentTimeMillis())
            .apply()
    }
    
    fun getExamCache(studentId: String): ExamResponse? {
        val json = prefs.getString("exam_cache_$studentId", null) ?: return null
        return try {
            gson.fromJson(json, ExamResponse::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    fun hasExamCache(studentId: String): Boolean {
        return prefs.contains("exam_cache_$studentId")
    }
    
    fun clear() {
        prefs.edit().clear().apply()
    }

    fun saveUpdateAvailable(available: Boolean) {
        prefs.edit()
            .putBoolean("update_available", available)
            .apply()
    }

    fun isUpdateAvailable(): Boolean {
        return prefs.getBoolean("update_available", false)
    }

    fun saveLatestVersion(version: String) {
        prefs.edit()
            .putString("latest_version", version)
            .apply()
    }

    fun getLatestVersion(): String? {
        return prefs.getString("latest_version", null)
    }

    fun saveCustomSchedules(schedules: List<com.example.myapplication.model.CustomSchedule>) {
        val json = gson.toJson(schedules.map { it.toMap() })
        prefs.edit()
            .putString("custom_schedules", json)
            .apply()
    }

    fun getCustomSchedules(): List<com.example.myapplication.model.CustomSchedule> {
        val json = prefs.getString("custom_schedules", null) ?: return emptyList()
        return try {
            val list = gson.fromJson(json, List::class.java)
            list.mapNotNull { item ->
                @Suppress("UNCHECKED_CAST")
                com.example.myapplication.model.CustomSchedule.fromMap(item as Map<String, Any>)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addCustomSchedule(schedule: com.example.myapplication.model.CustomSchedule) {
        val schedules = getCustomSchedules().toMutableList()
        schedules.add(schedule)
        saveCustomSchedules(schedules)
    }

    fun removeCustomSchedule(scheduleId: String) {
        val schedules = getCustomSchedules().filter { it.id != scheduleId }
        saveCustomSchedules(schedules)
    }
    
    fun saveFontSize(size: Float) {
        prefs.edit()
            .putFloat("font_size", size)
            .apply()
    }
    
    fun getFontSize(): Float {
        return prefs.getFloat("font_size", 1.0f)
    }
}