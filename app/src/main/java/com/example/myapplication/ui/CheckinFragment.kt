package com.example.myapplication.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.cache.CurriculumCache
import com.example.myapplication.databinding.DialogNumberCheckinBinding
import com.example.myapplication.databinding.FragmentCheckinBinding
import com.example.myapplication.model.LocationData
import com.example.myapplication.model.RadarCheckinRequest
import com.example.myapplication.model.Rollcall
import com.example.myapplication.network.NetworkService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class CheckinFragment : Fragment() {

    private var _binding: FragmentCheckinBinding? = null
    private val binding get() = _binding!!

    private val networkService = NetworkService()
    private lateinit var cache: CurriculumCache
    private lateinit var rollcallAdapter: RollcallAdapter
    private var currentRollcall: Rollcall? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val qrScannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val qrCode = result.data?.getStringExtra("qr_code")
            if (qrCode != null) {
                performQrCheckin(qrCode)
            }
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocation = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocation = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        if (fineLocation || coarseLocation) {
            performRadarCheckin()
        } else {
            Toast.makeText(requireContext(), "需要位置权限才能进行雷达签到", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCheckinBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        cache = CurriculumCache(requireContext())
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        
        setupRecyclerView()
        setupClickListeners()
        checkLoginStatus()
    }

    private fun setupRecyclerView() {
        rollcallAdapter = RollcallAdapter { rollcall ->
            if (!rollcall.is_checked_in) {
                currentRollcall = rollcall
                when (rollcall.type) {
                    "number" -> showNumberCheckinDialog(rollcall)
                    "qr" -> startQrScanner()
                    "radar" -> checkLocationPermissionAndCheckin()
                    else -> Toast.makeText(requireContext(), "未知的签到类型", Toast.LENGTH_SHORT).show()
                }
            }
        }
        binding.recyclerRollcalls.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerRollcalls.adapter = rollcallAdapter
    }

    private fun setupClickListeners() {
        binding.btnGoToSettings.setOnClickListener {
            (activity as? MainActivity)?.navigateToSettings()
        }

        binding.btnRefresh.setOnClickListener {
            fetchRollcalls()
        }

        binding.btnNumberCheckin.setOnClickListener {
            val pendingRollcall = rollcallAdapter.getItems().firstOrNull { !it.is_checked_in }
            if (pendingRollcall != null) {
                currentRollcall = pendingRollcall
                showNumberCheckinDialog(pendingRollcall)
            } else {
                Toast.makeText(requireContext(), "暂无待签到课程", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnQrCheckin.setOnClickListener {
            val pendingRollcall = rollcallAdapter.getItems().firstOrNull { !it.is_checked_in }
            if (pendingRollcall != null) {
                currentRollcall = pendingRollcall
                startQrScanner()
            } else {
                Toast.makeText(requireContext(), "暂无待签到课程", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnRadarCheckin.setOnClickListener {
            val pendingRollcall = rollcallAdapter.getItems().firstOrNull { !it.is_checked_in }
            if (pendingRollcall != null) {
                currentRollcall = pendingRollcall
                checkLocationPermissionAndCheckin()
            } else {
                Toast.makeText(requireContext(), "暂无待签到课程", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkLoginStatus() {
        val session = cache.getXzcySession()
        if (session != null) {
            binding.layoutNotLoggedIn.visibility = View.GONE
            binding.layoutLoggedIn.visibility = View.VISIBLE
            fetchRollcalls()
        } else {
            binding.layoutNotLoggedIn.visibility = View.VISIBLE
            binding.layoutLoggedIn.visibility = View.GONE
        }
    }

    private fun fetchRollcalls() {
        val session = cache.getXzcySession()
        if (session == null) {
            checkLoginStatus()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerRollcalls.visibility = View.GONE
        binding.tvEmptyRollcalls.visibility = View.GONE

        lifecycleScope.launch {
            val result = networkService.fetchRollcalls(session)
            
            binding.progressBar.visibility = View.GONE
            
            result.fold(
                onSuccess = { response ->
                    if (response.success) {
                        rollcallAdapter.submitList(response.rollcalls)
                        if (response.rollcalls.isEmpty()) {
                            binding.tvEmptyRollcalls.visibility = View.VISIBLE
                            binding.recyclerRollcalls.visibility = View.GONE
                        } else {
                            binding.recyclerRollcalls.visibility = View.VISIBLE
                            binding.tvEmptyRollcalls.visibility = View.GONE
                        }
                    } else {
                        Toast.makeText(requireContext(), "获取签到列表失败", Toast.LENGTH_SHORT).show()
                    }
                },
                onFailure = { error ->
                    Toast.makeText(requireContext(), "获取签到列表失败: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun showNumberCheckinDialog(rollcall: Rollcall) {
        val dialogBinding = DialogNumberCheckinBinding.inflate(layoutInflater)
        dialogBinding.tvCourseInfo.text = "${rollcall.name} - ${rollcall.teacher_name}"

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

        dialogBinding.btnConfirm.setOnClickListener {
            val number = dialogBinding.etNumber.text.toString().trim()
            if (number.isEmpty()) {
                Toast.makeText(requireContext(), "请输入签到数字", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            dialogBinding.btnConfirm.isEnabled = false
            dialogBinding.btnConfirm.text = "签到中..."

            performNumberCheckin(rollcall.id, number, dialog, dialogBinding)
        }

        dialog.show()
    }

    private fun performNumberCheckin(rollcallId: Int, number: String, dialog: android.app.Dialog, dialogBinding: DialogNumberCheckinBinding) {
        val session = cache.getXzcySession() ?: return

        lifecycleScope.launch {
            val result = networkService.numberCheckin(session, rollcallId, number)

            dialogBinding.btnConfirm.isEnabled = true
            dialogBinding.btnConfirm.text = "签到"

            result.fold(
                onSuccess = { response ->
                    if (response.success) {
                        dialog.dismiss()
                        Toast.makeText(requireContext(), "签到成功", Toast.LENGTH_SHORT).show()
                        fetchRollcalls()
                    } else {
                        Toast.makeText(requireContext(), response.message, Toast.LENGTH_SHORT).show()
                    }
                },
                onFailure = { error ->
                    Toast.makeText(requireContext(), "签到失败: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun startQrScanner() {
        val intent = Intent(requireContext(), QrScannerActivity::class.java)
        qrScannerLauncher.launch(intent)
    }

    private fun performQrCheckin(qrCode: String) {
        val rollcall = currentRollcall ?: return
        val session = cache.getXzcySession() ?: return

        lifecycleScope.launch {
            val result = networkService.qrCheckin(session, rollcall.id, qrCode)

            result.fold(
                onSuccess = { response ->
                    if (response.success) {
                        Toast.makeText(requireContext(), "签到成功", Toast.LENGTH_SHORT).show()
                        fetchRollcalls()
                    } else {
                        Toast.makeText(requireContext(), response.message, Toast.LENGTH_SHORT).show()
                    }
                },
                onFailure = { error ->
                    Toast.makeText(requireContext(), "签到失败: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun checkLocationPermissionAndCheckin() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            performRadarCheckin()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun performRadarCheckin() {
        val rollcall = currentRollcall ?: return
        val session = cache.getXzcySession() ?: return

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "没有位置权限", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(requireContext(), "正在获取位置...", Toast.LENGTH_SHORT).show()

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val locationData = LocationData(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    altitude = location.altitude,
                    accuracy = location.accuracy.toDouble(),
                    verticalAccuracy = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        location.verticalAccuracyMeters.toDouble()
                    } else {
                        0.0
                    },
                    speed = location.speed.toDouble()
                )

                val request = RadarCheckinRequest(
                    session = session,
                    rollcall_id = rollcall.id,
                    location = locationData
                )

                lifecycleScope.launch {
                    val result = networkService.radarCheckin(request)

                    result.fold(
                        onSuccess = { response ->
                            if (response.success) {
                                Toast.makeText(requireContext(), "签到成功", Toast.LENGTH_SHORT).show()
                                fetchRollcalls()
                            } else {
                                Toast.makeText(requireContext(), response.message, Toast.LENGTH_SHORT).show()
                            }
                        },
                        onFailure = { error ->
                            Toast.makeText(requireContext(), "签到失败: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            } else {
                Toast.makeText(requireContext(), "无法获取位置信息，请确保已开启GPS", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(requireContext(), "获取位置失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): CheckinFragment {
            return CheckinFragment()
        }
    }
}
