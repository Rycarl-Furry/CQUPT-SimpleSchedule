package com.example.myapplication.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.LoginActivity
import com.example.myapplication.cache.CurriculumCache
import com.example.myapplication.databinding.DialogIdsLoginBinding
import com.example.myapplication.databinding.FragmentSettingsBinding
import com.example.myapplication.network.NetworkService
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    
    private val networkService = NetworkService()
    private lateinit var cache: CurriculumCache
    
    private val currentVersion = "v1.0.3"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cache = CurriculumCache(requireContext())
        
        binding.tvCurrentVersion.text = currentVersion
        binding.tvVersion.text = "SimpleSchedule $currentVersion"
        
        setupIdsLogin()
        setupCheckUpdate()
        setupLogout()
        updateIdsStatus()
    }

    private fun setupIdsLogin() {
        binding.btnIdsLogin.setOnClickListener {
            showIdsLoginDialog()
        }
    }

    private fun showIdsLoginDialog() {
        val dialogBinding = DialogIdsLoginBinding.inflate(layoutInflater)
        
        val credentials = cache.getAutoLoginCredentials()
        if (credentials != null) {
            dialogBinding.etUsername.setText(credentials.first)
            dialogBinding.etPassword.setText(credentials.second)
            dialogBinding.cbAutoLogin.isChecked = true
        }
        
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setBackgroundInsetStart(40)
            .setBackgroundInsetEnd(40)
            .setBackgroundInsetTop(20)
            .setBackgroundInsetBottom(20)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        dialogBinding.btnLogin.setOnClickListener {
            val username = dialogBinding.etUsername.text.toString().trim()
            val password = dialogBinding.etPassword.text.toString().trim()
            
            if (username.isEmpty()) {
                Toast.makeText(requireContext(), "请输入学号", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (password.isEmpty()) {
                Toast.makeText(requireContext(), "请输入密码", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            performIdsLogin(username, password, dialogBinding.cbAutoLogin.isChecked, dialog, dialogBinding)
        }
        
        dialog.show()
    }

    private fun performIdsLogin(
        username: String, 
        password: String, 
        autoLogin: Boolean,
        dialog: android.app.Dialog,
        dialogBinding: DialogIdsLoginBinding
    ) {
        dialogBinding.btnLogin.isEnabled = false
        dialogBinding.btnLogin.text = "登录中..."
        
        lifecycleScope.launch {
            val result = networkService.login(username, password)
            
            dialogBinding.btnLogin.isEnabled = true
            dialogBinding.btnLogin.text = "登录"
            
            result.fold(
                onSuccess = { response ->
                    cache.saveAccessToken(response.access_token)
                    
                    if (autoLogin) {
                        cache.saveAutoLoginCredentials(username, password)
                    } else {
                        cache.disableAutoLogin()
                    }
                    
                    dialog.dismiss()
                    updateIdsStatus()
                    Toast.makeText(requireContext(), "登录成功", Toast.LENGTH_SHORT).show()
                },
                onFailure = { error ->
                    Toast.makeText(
                        requireContext(),
                        "登录失败: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }

    private fun updateIdsStatus() {
        val token = cache.getAccessToken()
        if (token != null) {
            binding.tvIdsStatus.text = "已登录"
            binding.tvIdsStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
        } else {
            binding.tvIdsStatus.text = "未登录"
            binding.tvIdsStatus.setTextColor(android.graphics.Color.parseColor("#888888"))
        }
    }

    private fun setupCheckUpdate() {
        binding.btnCheckUpdate.setOnClickListener {
            checkForUpdate()
        }
    }

    private fun checkForUpdate() {
        Toast.makeText(requireContext(), "正在检查更新...", Toast.LENGTH_SHORT).show()
        
        lifecycleScope.launch {
            val result = networkService.fetchLatestVersion()
            
            result.fold(
                onSuccess = { latestVersion ->
                    if (latestVersion != currentVersion) {
                        showUpdateDialog(latestVersion)
                    } else {
                        Toast.makeText(requireContext(), "已是最新版本", Toast.LENGTH_SHORT).show()
                    }
                },
                onFailure = { error ->
                    Toast.makeText(
                        requireContext(),
                        "检查更新失败: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }

    private fun showUpdateDialog(latestVersion: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("发现新版本")
            .setMessage("有可用更新: $latestVersion\n当前版本: $currentVersion\n\n是否前往GitHub下载?")
            .setPositiveButton("前往下载") { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/rycarl/SimpleSchedule/releases"))
                startActivity(intent)
            }
            .setNegativeButton("稍后提醒", null)
            .show()
    }

    private fun setupLogout() {
        binding.btnLogout.setOnClickListener {
            cache.clear()
            
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): SettingsFragment {
            return SettingsFragment()
        }
    }
}
