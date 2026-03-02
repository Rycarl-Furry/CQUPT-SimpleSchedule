package com.example.myapplication

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.cache.CurriculumCache
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.model.CurriculumResponse
import com.example.myapplication.network.NetworkService
import com.example.myapplication.ui.CheckinFragment
import com.example.myapplication.ui.ExamFragment
import com.example.myapplication.ui.MoreFragment
import com.example.myapplication.ui.NoticeFragment
import com.example.myapplication.ui.ScheduleFragment
import com.example.myapplication.Constants
import com.example.myapplication.ui.SettingsFragment
import com.example.myapplication.widget.WidgetUpdater
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
    private val currentVersion = Constants.VERSION_NAME

    private val PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_MyApplication)
        super.onCreate(savedInstanceState)
        
        cache = CurriculumCache(this)
        
        val lastLoginId = cache.getLastLogin()
        if (lastLoginId != null && cache.hasCache(lastLoginId)) {
            val cachedData = cache.get(lastLoginId)
            if (cachedData != null) {
                curriculumData = cachedData
                curriculumJson = Gson().toJson(cachedData)
                studentId = lastLoginId
                
                binding = ActivityMainBinding.inflate(layoutInflater)
                setContentView(binding.root)
                
                setupBottomNavigation()
                
                if (savedInstanceState == null) {
                    currentNavId = R.id.nav_schedule
                    showScheduleFragment()
                }
                
                Handler(Looper.getMainLooper()).post {
                    refreshInBackground(studentId!!)
                    performAutoIdsLogin()
                    checkPermissions()
                    WidgetUpdater.updateAllWidgets(this@MainActivity)
                }
                return
            }
        }
        
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (!packageManager.canRequestPackageInstalls()) {
                permissions.add(Manifest.permission.REQUEST_INSTALL_PACKAGES)
            }
        }
        
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            checkUpdateInBackground()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
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
                        cache.saveUpdateAvailable(true)
                        cache.saveLatestVersion(versionInfo.version)
                    } else {
                        cache.saveUpdateAvailable(false)
                    }
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
                        WidgetUpdater.updateAllWidgets(this@MainActivity)
                        Toast.makeText(this@MainActivity, "课表已更新", Toast.LENGTH_SHORT).show()
                    } else if (cachedData == null) {
                        cache.save(studentId, newCurriculum)
                        WidgetUpdater.updateAllWidgets(this@MainActivity)
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
    fun getStudentId(): String? = studentId

    private fun setupBottomNavigation() {
        binding.bottomNavigation.labelVisibilityMode = NavigationBarView.LABEL_VISIBILITY_LABELED
        
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            if (item.itemId == currentNavId) {
                return@setOnItemSelectedListener true
            }
            
            val oldNavId = currentNavId
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_schedule -> ScheduleFragment.newInstance(curriculumJson ?: "")
                R.id.nav_checkin -> CheckinFragment.newInstance()
                R.id.nav_more -> MoreFragment.newInstance()
                R.id.nav_settings -> SettingsFragment.newInstance()
                else -> return@setOnItemSelectedListener false
            }
            
            currentNavId = item.itemId
            replaceFragmentWithSlideAnimation(fragment, oldNavId, item.itemId)
            true
        }
    }

    private fun getNavOrder(navId: Int): Int {
        return when (navId) {
            R.id.nav_schedule -> 0
            R.id.nav_checkin -> 1
            R.id.nav_more -> 2
            R.id.nav_settings -> 3
            else -> 0
        }
    }

    private fun replaceFragmentWithSlideAnimation(fragment: Fragment, oldNavId: Int, newNavId: Int) {
        val oldOrder = getNavOrder(oldNavId)
        val newOrder = getNavOrder(newNavId)
        
        val enterAnim: Int
        val exitAnim: Int
        
        if (newOrder > oldOrder) {
            enterAnim = R.anim.slide_in_right
            exitAnim = R.anim.slide_out_left
        } else {
            enterAnim = R.anim.slide_in_left
            exitAnim = R.anim.slide_out_right
        }
        
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(enterAnim, exitAnim, enterAnim, exitAnim)
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun showScheduleFragment() {
        val fragment = ScheduleFragment.newInstance(curriculumJson ?: "")
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.fade_in_smooth, 0, 0, 0)
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
