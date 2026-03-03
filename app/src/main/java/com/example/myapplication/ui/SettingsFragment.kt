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
import com.example.myapplication.databinding.DialogAboutBinding
import com.example.myapplication.databinding.DialogDownloadProgressBinding
import com.example.myapplication.databinding.DialogIdsLoginBinding
import com.example.myapplication.databinding.DialogXzcyLoginBinding
import com.example.myapplication.databinding.FragmentSettingsBinding
import com.example.myapplication.Constants
import com.example.myapplication.network.NetworkService
import com.example.myapplication.model.XzcyLoginRequest
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    
    private val networkService = NetworkService()
    private lateinit var cache: CurriculumCache
    
    private val currentVersion = Constants.VERSION_NAME

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
        setupXzcyLogin()
        setupCheckUpdate()
        setupAbout()
        setupLogout()
        setupFontSize()
        updateIdsStatus()
        updateXzcyStatus()
        updateUpdateStatus()
    }
    
    private fun setupFontSize() {
        binding.btnFontSize.setOnClickListener {
            showFontSizeDialog()
        }
        updateFontSizeDisplay()
    }
    
    private fun updateFontSizeDisplay() {
        val fontSize = cache.getFontSize()
        binding.tvFontSize.text = when (fontSize) {
            0.8f -> "小"
            1.0f -> "标准"
            1.2f -> "大"
            1.4f -> "特大"
            else -> "标准"
        }
    }
    
    private fun showFontSizeDialog() {
        val options = arrayOf("小", "标准", "大", "特大")
        val currentFontSize = cache.getFontSize()
        val currentIndex = when (currentFontSize) {
            0.8f -> 0
            1.0f -> 1
            1.2f -> 2
            1.4f -> 3
            else -> 1
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("选择字体大小")
            .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                val selectedSize = when (which) {
                    0 -> 0.8f
                    1 -> 1.0f
                    2 -> 1.2f
                    3 -> 1.4f
                    else -> 1.0f
                }
                cache.saveFontSize(selectedSize)
                updateFontSizeDisplay()
                dialog.dismiss()
                // 提示用户重启应用生效
                android.widget.Toast.makeText(requireContext(), "字体大小已更新，重启应用后生效", android.widget.Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun updateUpdateStatus() {
        if (cache.isUpdateAvailable()) {
            val latestVersion = cache.getLatestVersion()
            binding.tvCurrentVersion.text = "$currentVersion (有更新: $latestVersion)"
            binding.tvCurrentVersion.setTextColor(android.graphics.Color.parseColor("#E53935"))
        } else {
            binding.tvCurrentVersion.text = currentVersion
            binding.tvCurrentVersion.setTextColor(android.graphics.Color.parseColor("#888888"))
        }
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
                Toast.makeText(requireContext(), "请输入统一认证码", Toast.LENGTH_SHORT).show()
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

    private fun setupXzcyLogin() {
        binding.btnXzcyLogin.setOnClickListener {
            showXzcyLoginDialog()
        }
    }

    private fun showXzcyLoginDialog() {
        val dialogBinding = DialogXzcyLoginBinding.inflate(layoutInflater)
        
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
            val uid = dialogBinding.etUid.text.toString().trim()
            val password = dialogBinding.etPassword.text.toString().trim()
            val autoLogin = dialogBinding.cbAutoLogin.isChecked
            
            if (uid.isEmpty()) {
                Toast.makeText(requireContext(), "请输入统一认证码", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (password.isEmpty()) {
                Toast.makeText(requireContext(), "请输入密码", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            performXzcyLogin(uid, password, autoLogin, dialog, dialogBinding)
        }
        
        dialog.show()
    }

     private fun performXzcyLogin(
        uid: String, 
        password: String,
        autoLogin: Boolean,
        dialog: android.app.Dialog,
        dialogBinding: DialogXzcyLoginBinding
    ) {
        dialogBinding.btnLogin.isEnabled = false
        dialogBinding.btnLogin.text = "登录中..."
        
        lifecycleScope.launch {
            val result = networkService.xzcyLogin(XzcyLoginRequest(uid, password))
            
            dialogBinding.btnLogin.isEnabled = true
            dialogBinding.btnLogin.text = "登录"
            
            result.fold(
                onSuccess = { response ->
                    if (response.success) {
                        cache.saveXzcySession(response.session ?: "")
                        if (autoLogin) {
                            cache.saveXzcyAutoLoginCredentials(uid, password)
                        }
                        dialog.dismiss()
                        updateXzcyStatus()
                        Toast.makeText(requireContext(), "登录成功", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), response.message, Toast.LENGTH_SHORT).show()
                    }
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

    private fun updateXzcyStatus() {
        val session = cache.getXzcySession()
        if (session != null) {
            binding.tvXzcyStatus.text = "已登录"
            binding.tvXzcyStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
        } else {
            binding.tvXzcyStatus.text = "未登录"
            binding.tvXzcyStatus.setTextColor(android.graphics.Color.parseColor("#888888"))
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
                onSuccess = { versionInfo ->
                    if (versionInfo.version != currentVersion) {
                        // 保存更新状态
                        cache.saveUpdateAvailable(true)
                        cache.saveLatestVersion(versionInfo.version)
                        updateUpdateStatus()
                        showUpdateDialog(versionInfo)
                    } else {
                        // 清除更新状态
                        cache.saveUpdateAvailable(false)
                        updateUpdateStatus()
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

    private fun showUpdateDialog(versionInfo: com.example.myapplication.network.NetworkService.VersionInfo) {
        val message = buildString {
            append("有可用更新: ${versionInfo.version}\n")
            append("当前版本: $currentVersion\n\n")
            if (versionInfo.updateContent.isNotEmpty()) {
                append("更新内容:\n")
                append(versionInfo.updateContent)
                append("\n\n")
            }
            append("是否自动下载并安装更新?")
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("发现新版本")
            .setMessage(message)
            .setPositiveButton("下载安装") { _, _ ->
                downloadAndInstallUpdate()
            }
            .setNegativeButton("稍后提醒", null)
            .show()
    }

    private val notificationId = 1001
    private lateinit var notificationManager: android.app.NotificationManager
    private lateinit var notificationBuilder: android.app.Notification.Builder
    private var downloadDialog: android.app.Dialog? = null

    private fun downloadAndInstallUpdate() {
        notificationManager = requireContext().getSystemService(android.app.NotificationManager::class.java)
        
        // 创建通知渠道
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                "update_channel",
                "更新通知",
                android.app.NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
        
        // 创建通知
        notificationBuilder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.Notification.Builder(requireContext(), "update_channel")
        } else {
            android.app.Notification.Builder(requireContext())
        }
        
        notificationBuilder
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("应用更新")
            .setContentText("正在下载更新...")
            .setProgress(100, 0, false)
            .setOngoing(true)
        
        // 显示通知
        notificationManager.notify(notificationId, notificationBuilder.build())
        
        // 创建并显示进度对话框
        val dialogBinding = DialogDownloadProgressBinding.inflate(layoutInflater)
        downloadDialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setBackgroundInsetStart(40)
            .setBackgroundInsetEnd(40)
            .setBackgroundInsetTop(20)
            .setBackgroundInsetBottom(20)
            .setCancelable(false)
            .create()
        
        downloadDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        downloadDialog?.show()
        
        lifecycleScope.launch {
            try {
                val result = networkService.downloadApk(requireContext()) { progress ->
                    // 在主线程中更新UI
                    activity?.runOnUiThread {
                        // 更新通知进度
                        notificationBuilder.setProgress(100, progress, false)
                        notificationBuilder.setContentText("正在下载更新... $progress%")
                        notificationManager.notify(notificationId, notificationBuilder.build())
                        
                        // 更新对话框进度
                        dialogBinding.progressBar.progress = progress
                        dialogBinding.tvProgress.text = "$progress%"
                    }
                }
                
                result.fold(
                    onSuccess = { apkFile ->
                        // 在主线程中更新UI
                        activity?.runOnUiThread {
                            // 关闭进度对话框
                            downloadDialog?.dismiss()
                            
                            // 下载完成，更新通知
                            notificationBuilder.setContentText("下载完成，正在安装...")
                            notificationBuilder.setProgress(0, 0, false)
                            notificationBuilder.setOngoing(false)
                            notificationManager.notify(notificationId, notificationBuilder.build())
                            
                            // 安装APK
                            networkService.installApk(requireContext(), apkFile)
                            
                            // 清除更新状态
                            cache.saveUpdateAvailable(false)
                            updateUpdateStatus()
                        }
                    },
                    onFailure = { error ->
                        // 在主线程中更新UI
                        activity?.runOnUiThread {
                            // 关闭进度对话框
                            downloadDialog?.dismiss()
                            
                            // 下载失败，更新通知
                            notificationBuilder.setContentText("下载失败: ${error.message}")
                            notificationBuilder.setProgress(0, 0, false)
                            notificationBuilder.setOngoing(false)
                            notificationManager.notify(notificationId, notificationBuilder.build())
                            
                            Toast.makeText(
                                requireContext(),
                                "下载失败: ${error.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            } catch (e: Exception) {
                // 在主线程中更新UI
                activity?.runOnUiThread {
                    // 关闭进度对话框
                    downloadDialog?.dismiss()
                    
                    // 捕获异常，防止闪退
                    notificationBuilder.setContentText("下载失败: ${e.message}")
                    notificationBuilder.setProgress(0, 0, false)
                    notificationBuilder.setOngoing(false)
                    notificationManager.notify(notificationId, notificationBuilder.build())
                    
                    Toast.makeText(
                        requireContext(),
                        "下载失败: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun setupAbout() {
        binding.btnAbout.setOnClickListener {
            showAboutDialog()
        }
    }

    private fun showAboutDialog() {
        val dialogBinding = DialogAboutBinding.inflate(layoutInflater)
        
        // 设置版本号
        dialogBinding.tvVersion.text = Constants.VERSION_NAME
        
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setBackgroundInsetStart(40)
            .setBackgroundInsetEnd(40)
            .setBackgroundInsetTop(20)
            .setBackgroundInsetBottom(20)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        dialogBinding.btnGithub.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Rycarl-Furry/CQUPT-SimpleSchedule"))
            startActivity(intent)
        }
        
        dialogBinding.btnBlog.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://rycarl.cn"))
            startActivity(intent)
        }
        
        dialogBinding.btnClose.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
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
