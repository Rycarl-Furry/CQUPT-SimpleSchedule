package com.example.myapplication.network

import com.example.myapplication.model.CurriculumResponse
import com.example.myapplication.model.Notice
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class NetworkService {
    
    private val client = OkHttpClient()
    private val gson = Gson()
    
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
}
