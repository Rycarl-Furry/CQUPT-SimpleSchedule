package com.example.myapplication.network

import com.example.myapplication.model.CurriculumResponse
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class CurriculumService {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val baseUrl = "https://cqupt.ishub.top/api/curriculum"

    suspend fun fetchCurriculum(studentId: String): Result<CurriculumResponse> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/$studentId/curriculum.json"
            val request = Request.Builder()
                .url(url)
                .build()

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
}
