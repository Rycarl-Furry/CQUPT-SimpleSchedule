package com.example.myapplication.network

import com.example.myapplication.model.CurriculumResponse
import com.example.myapplication.model.ExamResponse
import com.example.myapplication.model.Notice
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class NetworkService {
    
    private val client = OkHttpClient()
    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    
    suspend fun fetchCurriculum(studentId: String): Result<CurriculumResponse> = withContext(Dispatchers.IO) {
        try {
            val url = "https://cqupt.ishub.top/api/curriculum/$studentId/curriculum.json"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("请求失败: ${response.code}"))
            }
            val responseBody = response.body?.string()
                ?: return@withContext Result.failure(IOException("响应体为空"))
            val curriculum = gson.fromJson(responseBody, CurriculumResponse::class.java)
            Result.success(curriculum)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun fetchNotices(): Result<List<Notice>> = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.rycarl.cn/notices"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("请求失败: ${response.code}"))
            }
            val responseBody = response.body?.string()
                ?: return@withContext Result.failure(IOException("响应体为空"))
            val notices = gson.fromJson(responseBody, Array<Notice>::class.java).toList()
            Result.success(notices)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun fetchExams(studentId: String): Result<ExamResponse> = withContext(Dispatchers.IO) {
        try {
            val url = "http://exam.rycarl.cn/api/exam"
            val jsonBody = gson.toJson(mapOf("student_id" to studentId))
            val body = jsonBody.toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("请求失败: ${response.code}"))
            }
            val responseBody = response.body?.string()
                ?: return@withContext Result.failure(IOException("响应体为空"))
            val examResponse = gson.fromJson(responseBody, ExamResponse::class.java)
            Result.success(examResponse)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
