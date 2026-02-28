package com.example.myapplication

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

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var curriculumJson: String? = null
    private var studentId: String? = null
    private var curriculumData: CurriculumResponse? = null
    private var currentNavId: Int = 0
    
    private val networkService = NetworkService()
    private lateinit var cache: CurriculumCache

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
                R.id.nav_exam -> ExamFragment.newInstance()
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
        val fragment = ExamFragment.newInstance()
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
