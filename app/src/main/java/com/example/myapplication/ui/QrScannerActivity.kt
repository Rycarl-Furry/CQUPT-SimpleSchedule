package com.example.myapplication.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.myapplication.databinding.ActivityQrScannerBinding
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.graphics.Point
import android.graphics.Rect
import android.util.Log
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import androidx.camera.core.ImageProxy
import androidx.camera.core.ZoomState
import java.util.concurrent.atomic.AtomicBoolean

class QrScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQrScannerBinding
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraExecutor: ExecutorService
    private var camera: Camera? = null
    private var cameraControl: CameraControl? = null
    private var maxZoomRatio: Float = 1.0f
    private var minZoomRatio: Float = 1.0f
    private val isProcessing = AtomicBoolean(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "需要摄像头权限才能扫描二维码", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            finish()
        }

        setupZoomControls()

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun allPermissionsGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun setupZoomControls() {
        binding.btnZoomIn.setOnClickListener {
            zoomIn()
        }

        binding.btnZoomOut.setOnClickListener {
            zoomOut()
        }

        binding.seekBarZoom.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    setZoom(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun zoomIn() {
        camera?.let { cam ->
            val currentZoom = cam.cameraInfo.zoomState.value?.zoomRatio ?: 1.0f
            val newZoom = (currentZoom + 0.5f).coerceAtMost(maxZoomRatio)
            cameraControl?.setZoomRatio(newZoom)
            updateZoomUI(newZoom)
        }
    }

    private fun zoomOut() {
        camera?.let { cam ->
            val currentZoom = cam.cameraInfo.zoomState.value?.zoomRatio ?: 1.0f
            val newZoom = (currentZoom - 0.5f).coerceAtLeast(minZoomRatio)
            cameraControl?.setZoomRatio(newZoom)
            updateZoomUI(newZoom)
        }
    }

    private fun setZoom(progress: Int) {
        if (maxZoomRatio <= minZoomRatio) return
        
        val zoomRange = maxZoomRatio - minZoomRatio
        val zoomRatio = minZoomRatio + (zoomRange * progress / 100f)
        cameraControl?.setZoomRatio(zoomRatio)
        updateZoomUI(zoomRatio)
    }

    private fun updateZoomUI(zoomRatio: Float) {
        binding.tvZoomLevel.text = String.format("缩放: %.1fx", zoomRatio)
        binding.tvZoomLevel.visibility = android.view.View.VISIBLE
        
        if (maxZoomRatio > minZoomRatio) {
            val progress = ((zoomRatio - minZoomRatio) / (maxZoomRatio - minZoomRatio) * 100).toInt()
            binding.seekBarZoom.setProgress(progress)
        }
    }

    private fun startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxy(imageProxy)
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )
                cameraControl = camera?.cameraControl
                
                setupZoomObserver()
            } catch (e: Exception) {
                Log.e("QrScannerActivity", "绑定相机失败", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun setupZoomObserver() {
        camera?.cameraInfo?.zoomState?.observe(this) { zoomState ->
            maxZoomRatio = zoomState.maxZoomRatio
            minZoomRatio = zoomState.minZoomRatio
            
            if (maxZoomRatio > minZoomRatio) {
                binding.seekBarZoom.isEnabled = true
                binding.btnZoomIn.isEnabled = true
                binding.btnZoomOut.isEnabled = true
            } else {
                binding.seekBarZoom.isEnabled = false
                binding.btnZoomIn.isEnabled = false
                binding.btnZoomOut.isEnabled = false
            }
        }
    }

    private fun processImageProxy(imageProxy: ImageProxy) {
        if (isProcessing.get()) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            isProcessing.set(true)
            
            val inputImage = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            val scanner = BarcodeScanning.getClient()
            scanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        for (barcode in barcodes) {
                            handleBarcode(barcode)
                            break
                        }
                    } else {
                        isProcessing.set(false)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("QrScannerActivity", "条码扫描失败", e)
                    isProcessing.set(false)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun handleBarcode(barcode: Barcode) {
        val rawValue = barcode.rawValue
        if (rawValue != null) {
            runOnUiThread {
                val resultIntent = Intent()
                resultIntent.putExtra("qr_code", rawValue)
                setResult(RESULT_OK, resultIntent)
                finish()
            }
        } else {
            isProcessing.set(false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
