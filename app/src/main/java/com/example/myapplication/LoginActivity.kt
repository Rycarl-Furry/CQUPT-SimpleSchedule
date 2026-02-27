package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.cache.CurriculumCache
import com.example.myapplication.databinding.ActivityLoginBinding
import com.example.myapplication.network.CurriculumService
import com.google.gson.Gson
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val curriculumService = CurriculumService()
    private lateinit var cache: CurriculumCache

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cache = CurriculumCache(this)
        
        val lastLoginId = cache.getLastLogin()
        if (lastLoginId != null && cache.hasCache(lastLoginId)) {
            autoLogin(lastLoginId)
            return
        }
        
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            val studentId = binding.etStudentId.text.toString().trim()
            if (studentId.isEmpty()) {
                Toast.makeText(this, "请输入学号", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            loadCurriculum(studentId)
        }
    }

    private fun autoLogin(studentId: String) {
        val cachedData = cache.get(studentId)
        if (cachedData != null) {
            cache.saveLastLogin(studentId)
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("curriculum_data", Gson().toJson(cachedData))
                putExtra("student_id", studentId)
                putExtra("from_cache", true)
            }
            startActivity(intent)
            finish()
        } else {
            binding = ActivityLoginBinding.inflate(layoutInflater)
            setContentView(binding.root)
            binding.etStudentId.setText(studentId)
            
            binding.btnLogin.setOnClickListener {
                val id = binding.etStudentId.text.toString().trim()
                if (id.isEmpty()) {
                    Toast.makeText(this, "请输入学号", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                loadCurriculum(id)
            }
        }
    }

    private fun loadCurriculum(studentId: String) {
        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.btnLogin.isEnabled = false

        val cachedData = cache.get(studentId)
        
        if (cachedData != null) {
            cache.saveLastLogin(studentId)
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("curriculum_data", Gson().toJson(cachedData))
                putExtra("student_id", studentId)
                putExtra("from_cache", true)
            }
            startActivity(intent)
            finish()
            return
        }

        lifecycleScope.launch {
            val result = curriculumService.fetchCurriculum(studentId)
            binding.progressBar.visibility = android.view.View.GONE
            binding.btnLogin.isEnabled = true

            result.fold(
                onSuccess = { curriculum ->
                    cache.save(studentId, curriculum)
                    cache.saveLastLogin(studentId)
                    val intent = Intent(this@LoginActivity, MainActivity::class.java).apply {
                        putExtra("curriculum_data", Gson().toJson(curriculum))
                        putExtra("student_id", studentId)
                        putExtra("from_cache", false)
                    }
                    startActivity(intent)
                    finish()
                },
                onFailure = { error ->
                    Toast.makeText(
                        this@LoginActivity,
                        "加载失败: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }
}
