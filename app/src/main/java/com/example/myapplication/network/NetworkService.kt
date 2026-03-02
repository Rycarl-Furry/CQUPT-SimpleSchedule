package com.example.myapplication.network

import com.example.myapplication.model.CurriculumResponse
import com.example.myapplication.model.ExamResponse
import com.example.myapplication.model.LoginRequest
import com.example.myapplication.model.LoginResponse
import com.example.myapplication.model.Notice
import com.example.myapplication.model.SportsResponse
import com.example.myapplication.model.XzcyLoginRequest
import com.example.myapplication.model.XzcyLoginResponse
import com.example.myapplication.model.RollcallsRequest
import com.example.myapplication.model.RollcallResponse
import com.example.myapplication.model.QrCheckinRequest
import com.example.myapplication.model.NumberCheckinRequest
import com.example.myapplication.model.RadarCheckinRequest
import com.example.myapplication.model.CheckinResponse
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
    
    suspend fun login(username: String, password: String): Result<LoginResponse> = withContext(Dispatchers.IO) {
        try {
            val url = "https://sport.rycarl.cn/login"
            val loginRequest = LoginRequest(username, password)
            val jsonBody = gson.toJson(loginRequest)
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
            val loginResponse = gson.fromJson(responseBody, LoginResponse::class.java)
            if (loginResponse.success) {
                Result.success(loginResponse)
            } else {
                Result.failure(IOException("登录失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun fetchSportsResult(accessToken: String): Result<SportsResponse> = withContext(Dispatchers.IO) {
        try {
            val url = "https://sport.rycarl.cn/sports/result"
            val jsonBody = gson.toJson(mapOf("access_token" to accessToken))
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
            val sportsResponse = gson.fromJson(responseBody, SportsResponse::class.java)
            if (sportsResponse.success) {
                Result.success(sportsResponse)
            } else {
                Result.failure(IOException("获取体育打卡数据失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    data class VersionInfo(
        val version: String,
        val updateContent: String
    )

    suspend fun fetchLatestVersion(): Result<VersionInfo> = withContext(Dispatchers.IO) {
        try {
            val url = "https://rycarl.cn/ver.txt"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("请求失败: ${response.code}"))
            }
            val responseBody = response.body?.string()
                ?: return@withContext Result.failure(IOException("响应体为空"))
            
            val lines = responseBody.lines().map { it.trim() }
            val version = lines.firstOrNull() ?: ""
            val updateContentIndex = lines.indexOfFirst { it.startsWith("更新内容:") }
            val updateContent = if (updateContentIndex != -1 && updateContentIndex + 1 < lines.size) {
                lines.subList(updateContentIndex + 1, lines.size).joinToString("\n")
            } else {
                ""
            }
            
            Result.success(VersionInfo(version, updateContent))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadApk(context: android.content.Context, progressCallback: ((Int) -> Unit)? = null): Result<java.io.File> = withContext(Dispatchers.IO) {
        try {
            val url = "https://rycarl.cn/SimpleSchedule.apk"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("下载失败: ${response.code}"))
            }
            
            val apkFile = java.io.File(context.externalCacheDir, "SimpleSchedule.apk")
            val outputStream = java.io.FileOutputStream(apkFile)
            val inputStream = response.body?.byteStream() ?: return@withContext Result.failure(IOException("响应体为空"))
            
            val totalSize = response.body?.contentLength() ?: 0
            var downloadedSize = 0L
            val buffer = ByteArray(8192)
            var bytesRead: Int
            
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                downloadedSize += bytesRead
                if (totalSize > 0) {
                    val progress = ((downloadedSize * 100) / totalSize).toInt()
                    progressCallback?.invoke(progress)
                }
            }
            
            outputStream.close()
            inputStream.close()
            
            Result.success(apkFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun installApk(context: android.content.Context, apkFile: java.io.File) {
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
        val uri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            // 使用FileProvider获取URI
            androidx.core.content.FileProvider.getUriForFile(
                context,
                "com.example.myapplication.fileprovider",
                apkFile
            )
        } else {
            // 旧版本使用Uri.fromFile
            android.net.Uri.fromFile(apkFile)
        }
        intent.setDataAndType(uri, "application/vnd.android.package-archive")
        intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
        context.startActivity(intent)
    }
    
    private data class ErrorResponse(
        val success: Boolean?,
        val message: String?
    )

    suspend fun xzcyLogin(request: XzcyLoginRequest): Result<XzcyLoginResponse> = withContext(Dispatchers.IO) {
        try {
            val url = "https://xzcy.rycarl.cn/api/login/password"
            val jsonBody = gson.toJson(request)
            val body = jsonBody.toRequestBody(jsonMediaType)
            val httpRequest = Request.Builder()
                .url(url)
                .post(body)
                .build()
            val response = client.newCall(httpRequest).execute()
            val responseBody = response.body?.string()
                ?: return@withContext Result.failure(IOException("响应体为空"))
            
            if (!response.isSuccessful) {
                val errorResponse = tryParseErrorResponse(responseBody, response.code)
                return@withContext Result.failure(IOException(errorResponse))
            }
            
            val loginResponse = gson.fromJson(responseBody, XzcyLoginResponse::class.java)
            Result.success(loginResponse)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun fetchRollcalls(session: String): Result<RollcallResponse> = withContext(Dispatchers.IO) {
        try {
            val url = "https://xzcy.rycarl.cn/api/rollcalls"
            val jsonBody = gson.toJson(RollcallsRequest(session))
            val body = jsonBody.toRequestBody(jsonMediaType)
            val httpRequest = Request.Builder()
                .url(url)
                .post(body)
                .build()
            val response = client.newCall(httpRequest).execute()
            val responseBody = response.body?.string()
                ?: return@withContext Result.failure(IOException("响应体为空"))
            
            if (!response.isSuccessful) {
                val errorResponse = tryParseErrorResponse(responseBody, response.code)
                return@withContext Result.failure(IOException(errorResponse))
            }
            
            val rollcallResponse = gson.fromJson(responseBody, RollcallResponse::class.java)
            Result.success(rollcallResponse)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun qrCheckin(session: String, rollcallId: Int, code: String): Result<CheckinResponse> = withContext(Dispatchers.IO) {
        try {
            val url = "https://xzcy.rycarl.cn/api/checkin/qr"
            val jsonBody = gson.toJson(QrCheckinRequest(session, rollcallId, code))
            val body = jsonBody.toRequestBody(jsonMediaType)
            val httpRequest = Request.Builder()
                .url(url)
                .post(body)
                .build()
            val response = client.newCall(httpRequest).execute()
            val responseBody = response.body?.string()
                ?: return@withContext Result.failure(IOException("响应体为空"))
            
            if (!response.isSuccessful) {
                val errorResponse = tryParseErrorResponse(responseBody, response.code)
                return@withContext Result.failure(IOException(errorResponse))
            }
            
            val checkinResponse = gson.fromJson(responseBody, CheckinResponse::class.java)
            Result.success(checkinResponse)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun numberCheckin(session: String, rollcallId: Int, number: String): Result<CheckinResponse> = withContext(Dispatchers.IO) {
        try {
            val url = "https://xzcy.rycarl.cn/api/checkin/number"
            val jsonBody = gson.toJson(NumberCheckinRequest(session, rollcallId, number))
            val body = jsonBody.toRequestBody(jsonMediaType)
            val httpRequest = Request.Builder()
                .url(url)
                .post(body)
                .build()
            val response = client.newCall(httpRequest).execute()
            val responseBody = response.body?.string()
                ?: return@withContext Result.failure(IOException("响应体为空"))
            
            if (!response.isSuccessful) {
                val errorResponse = tryParseErrorResponse(responseBody, response.code)
                return@withContext Result.failure(IOException(errorResponse))
            }
            
            val checkinResponse = gson.fromJson(responseBody, CheckinResponse::class.java)
            Result.success(checkinResponse)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun radarCheckin(request: RadarCheckinRequest): Result<CheckinResponse> = withContext(Dispatchers.IO) {
        try {
            val url = "https://xzcy.rycarl.cn/api/checkin/radar"
            val jsonBody = gson.toJson(request)
            val body = jsonBody.toRequestBody(jsonMediaType)
            val httpRequest = Request.Builder()
                .url(url)
                .post(body)
                .build()
            val response = client.newCall(httpRequest).execute()
            val responseBody = response.body?.string()
                ?: return@withContext Result.failure(IOException("响应体为空"))
            
            if (!response.isSuccessful) {
                val errorResponse = tryParseErrorResponse(responseBody, response.code)
                return@withContext Result.failure(IOException(errorResponse))
            }
            
            val checkinResponse = gson.fromJson(responseBody, CheckinResponse::class.java)
            Result.success(checkinResponse)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun tryParseErrorResponse(responseBody: String, code: Int): String {
        return try {
            val errorResponse = gson.fromJson(responseBody, ErrorResponse::class.java)
            errorResponse.message ?: "请求失败: $code"
        } catch (e: Exception) {
            "请求失败: $code"
        }
    }
}
