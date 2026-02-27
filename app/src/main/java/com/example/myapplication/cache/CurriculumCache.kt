package com.example.myapplication.cache

import android.content.Context
import com.example.myapplication.model.CurriculumResponse
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
    
    fun clear() {
        prefs.edit().clear().apply()
    }
}
