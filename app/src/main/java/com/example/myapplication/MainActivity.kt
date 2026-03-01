package com.example.myapplication

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.cache.CurriculumCache
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.model.CurriculumResponse
import com.example.myapplication.network.NetworkService
import com.example.myapplication.ui.ExamFragment
import com.example.myapplication.ui.NoticeFragment
import com.example.myapplication.ui.ScheduleFragment
import com.example.myapplication.ui.SettingsFragment
import com.google.android.material.navigation.NavigationBarView
import com.google.gson.Gson
import kotlinx.coroutines.launch
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var curriculumJson: String? = null
    private var studentId: String? = null
    private var curriculumData: CurriculumResponse? = null
    private var currentNavId: Int = 0
    
    private val networkService = NetworkService()
    private lateinit var cache: CurriculumCache
    private val currentVersion = "v1.0.6"

    private val PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cache = CurriculumCache(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        curriculumJson = intent.getStringExtra("curriculum_data")
        studentId = intent.getStringExtra("student_id")
        val fromCache = intent.getBooleanExtra("from_cache", false)

        curriculumData = curriculumJson?.let { Gson().fromJson(it, CurriculumResponse::class.java) }

        setupBottomNavigation()

        if (savedInstanceState == null) {
            currentNavId = R.id.nav_schedule
            showScheduleFragment()
        }

        if (fromCache && studentId != null) {
            refreshInBackground(studentId!!)
        }
        
        performAutoIdsLogin()
        
        // 检查权限
        checkPermissions()
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()
        
        // 检查安装未知来源应用的权限
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (!packageManager.canRequestPackageInstalls()) {
                permissions.add(Manifest.permission.REQUEST_INSTALL_PACKAGES)
            }
        }
        
        // 检查存储权限（仅在Android 13以下）
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            // 权限已获取，开始后台检测更新
            checkUpdateInBackground()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // 无论权限是否获取，都开始后台检测更新
            // 即使没有权限，也可以检测更新，只是下载和安装可能会失败
            checkUpdateInBackground()
        }
    }

    private fun performAutoIdsLogin() {
        if (cache.isAutoLoginEnabled()) {
            val credentials = cache.getAutoLoginCredentials()
            if (credentials != null) {
                lifecycleScope.launch {
                    val result = networkService.login(credentials.first, credentials.second)
                    result.fold(
                        onSuccess = { response ->
                            cache.saveAccessToken(response.access_token)
                        },
                        onFailure = { }
                    )
                }
            }
        }
    }

    private fun checkUpdateInBackground() {
        lifecycleScope.launch {
            val result = networkService.fetchLatestVersion()
            result.fold(
                onSuccess = { versionInfo ->
                    if (versionInfo.version != currentVersion) {
                        // 保存更新状态
                        cache.saveUpdateAvailable(true)
                        cache.saveLatestVersion(versionInfo.version)
                        // 不自动下载，仅保存状态
                    } else {
                        // 清除更新状态
                        cache.saveUpdateAvailable(false)
                    }
                },
                onFailure = { }
            )
        }
    }

    private fun downloadAndInstallUpdate() {
        lifecycleScope.launch {
            val result = networkService.downloadApk(this@MainActivity)
            result.fold(
                onSuccess = { apkFile ->
                    networkService.installApk(this@MainActivity, apkFile)
                },
                onFailure = { }
            )
        }
    }

    private fun refreshInBackground(studentId: String) {
        lifecycleScope.launch {
            val result = networkService.fetchCurriculum(studentId)
            result.fold(
                onSuccess = { newCurriculum ->
                    val cachedData = cache.get(studentId)
                    if (cachedData != null && hasDataChanged(cachedData, newCurriculum)) {
                        cache.save(studentId, newCurriculum)
                        curriculumData = newCurriculum
                        curriculumJson = Gson().toJson(newCurriculum)
                        updateCurrentFragment()
                        Toast.makeText(this@MainActivity, "课表已更新", Toast.LENGTH_SHORT).show()
                    } else if (cachedData == null) {
                        cache.save(studentId, newCurriculum)
                    }
                },
                onFailure = { }
            )
        }
    }

    private fun hasDataChanged(old: CurriculumResponse, new: CurriculumResponse): Boolean {
        return old.instances.size != new.instances.size ||
                old.instances.zip(new.instances).any { (o, n) ->
                    o.course != n.course || o.location != n.location ||
                    o.teacher != n.teacher || o.week != n.week ||
                    o.day != n.day || o.periods != n.periods
                }
    }

    private fun updateCurrentFragment() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (currentFragment is ScheduleFragment) {
            showScheduleFragment()
        }
    }

    fun getCurriculumJson(): String? = curriculumJson

    private fun setupBottomNavigation() {
        binding.bottomNavigation.labelVisibilityMode = NavigationBarView.LABEL_VISIBILITY_LABELED
        
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            if (item.itemId == currentNavId) {
                return@setOnItemSelectedListener true
            }
            
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_schedule -> ScheduleFragment.newInstance(curriculumJson ?: "")
                R.id.nav_notice -> NoticeFragment.newInstance()
                R.id.nav_exam -> ExamFragment.newInstance(studentId ?: "")
                R.id.nav_settings -> SettingsFragment.newInstance()
                else -> return@setOnItemSelectedListener false
            }
            
            currentNavId = item.itemId
            replaceFragmentWithAnimation(fragment)
            true
        }
    }

    private fun showScheduleFragment() {
        val fragment = ScheduleFragment.newInstance(curriculumJson ?: "")
        replaceFragment(fragment)
    }

    private fun showNoticeFragment() {
        val fragment = NoticeFragment.newInstance()
        replaceFragment(fragment)
    }

    private fun showExamFragment() {
        val fragment = ExamFragment.newInstance(studentId ?: "")
        replaceFragment(fragment)
    }

    private fun showSettingsFragment() {
        val fragment = SettingsFragment.newInstance()
        replaceFragment(fragment)
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun replaceFragmentWithAnimation(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.fade_in, 0, 0, 0)
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
