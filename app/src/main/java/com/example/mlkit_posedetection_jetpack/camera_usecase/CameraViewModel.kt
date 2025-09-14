package com.example.mlkit_posedetection_jetpack.camera_usecase

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mlkit_posedetection_jetpack.posedetector.graphic.GraphicOverlay
import com.example.mlkit_posedetection_jetpack.posedetector.mlkit.PoseDetectorProcessor
import com.example.mlkit_posedetection_jetpack.posedetector.utils.BitmapUtils
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.pose.Pose
import kotlinx.coroutines.launch
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.ExecutionException

/** View model for interacting with CameraX. */
class CameraViewModel(
    private val graphicOverlay: GraphicOverlay,
    private val lifecycleOwner: LifecycleOwner,
    private val cameraSelector: Int,
    private val onResults: (bitmap: android.graphics.Bitmap, Pose) -> Unit

) : ViewModel() {
    private val poseDetectorProcessor by lazy {
        PoseDetectorProcessor()
    }
    private val TAG = "CameraXViewModel"
    private var cameraProviderLiveData = MutableLiveData<ProcessCameraProvider>()
    private var recording: Recording? = null
    private var needUpdateGraphicOverlayImageSourceInfo: Boolean = true
    private var bitmap: android.graphics.Bitmap? = null
    private var lensFacing: Int = cameraSelector
    private var imageAnalysis: ImageAnalysis? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var imageCapture: ImageCapture? = null

    // Speech recognition and injury detection
    private var speechRecognitionManager: SpeechRecognitionManager? = null
    private val injuryDetectionAPI = InjuryDetectionAPI()

    // State for UI
    val isListening = mutableStateOf(false)
    val lastDetectionResult = mutableStateOf<InjuryDetectionResult?>(null)
    val isProcessingImage = mutableStateOf(false)

    fun initializeSpeechRecognition(context: Context) {
        Log.d(TAG, "Initializing speech recognition")
        speechRecognitionManager = SpeechRecognitionManager(
            context = context,
            onSpeechResult = { result ->
                Log.d(TAG, "Speech recognition received: '$result'")
                handleSpeechResult(context, result)
            },
            onListeningStateChanged = { listening ->
                Log.d(TAG, "Speech recognition listening state changed: $listening")
                isListening.value = listening
            }
        )
        speechRecognitionManager?.startListening()
        Log.d(TAG, "Speech recognition initialized and started")
    }

    fun stopSpeechRecognition() {
        Log.d(TAG, "Stopping speech recognition")
        speechRecognitionManager?.stopListening()
    }

    private fun handleSpeechResult(context: Context, result: String) {
        val lowerResult = result.lowercase()
        Log.d(TAG, "Processing speech result: '$result' (lowercase: '$lowerResult')")

        val triggerWords = listOf("process img", "process image", "analyze", "detect injury", "check injury", "scan")
        val triggered = triggerWords.any { lowerResult.contains(it) }

        if (triggered) {
            Log.d(TAG, "Speech command recognized, triggering image capture")
            Toast.makeText(context, "Command recognized: '$result'", Toast.LENGTH_SHORT).show()
            captureAndAnalyzeImage(context)
        } else {
            Log.d(TAG, "Speech result does not match any trigger words")
        }
    }

    private fun captureAndAnalyzeImage(context: Context) {
        if (isProcessingImage.value) {
            Log.d(TAG, "Already processing an image, ignoring request")
            return
        }

        val imageCapture = this.imageCapture ?: run {
            Log.e(TAG, "ImageCapture not initialized")
            Toast.makeText(context, "Camera not ready for capture", Toast.LENGTH_SHORT).show()
            return
        }

        isProcessingImage.value = true

        val outputFile = File(context.cacheDir, "captured_image_${System.currentTimeMillis()}.jpg")
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

        imageCapture.takePicture(
            outputFileOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.d(TAG, "Image captured successfully to: ${outputFile.absolutePath}")

                    try {
                        // Read the captured image file and convert to bitmap
                        if (!outputFile.exists()) {
                            Log.e(TAG, "Captured file does not exist: ${outputFile.absolutePath}")
                            isProcessingImage.value = false
                            Toast.makeText(context, "Failed to capture image", Toast.LENGTH_SHORT).show()
                            return
                        }

                        val capturedBitmap = android.graphics.BitmapFactory.decodeFile(outputFile.absolutePath)
                        if (capturedBitmap == null) {
                            Log.e(TAG, "Failed to decode captured image from: ${outputFile.absolutePath}")
                            isProcessingImage.value = false
                            Toast.makeText(context, "Failed to process captured image", Toast.LENGTH_SHORT).show()
                            return
                        }

                        Log.d(TAG, "Captured bitmap size: ${capturedBitmap.width}x${capturedBitmap.height}")

                        // Use real API for injury detection
                        injuryDetectionAPI.analyzeImage(capturedBitmap, context.cacheDir, null, null) { result ->
                            isProcessingImage.value = false
                            lastDetectionResult.value = result

                            result?.let {
                                Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
                                Log.d(TAG, "Injury detection result: $it")
                            } ?: run {
                                Toast.makeText(context, "Failed to analyze image", Toast.LENGTH_SHORT).show()
                                Log.e(TAG, "No result received from injury detection")
                            }
                        }

                        // Clean up the temporary file
                        try {
                            if (outputFile.exists()) {
                                val deleted = outputFile.delete()
                                Log.d(TAG, "Temporary file cleanup: ${if (deleted) "success" else "failed"}")
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to cleanup captured file", e)
                        }

                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing captured image", e)
                        isProcessingImage.value = false
                        Toast.makeText(context, "Error processing image: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    isProcessingImage.value = false
                    Log.e(TAG, "Image capture failed", exception)
                    Toast.makeText(context, "Image capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // Add method to manually trigger image analysis without capture (useful for testing)
    fun manuallyAnalyzeCurrentImage(context: Context) {
        if (isProcessingImage.value) {
            Toast.makeText(context, "Already processing an image", Toast.LENGTH_SHORT).show()
            return
        }

        bitmap?.let { currentBitmap ->
            isProcessingImage.value = true
            Log.d(TAG, "Manually analyzing current pose detection image")

            injuryDetectionAPI.analyzeImage(currentBitmap, context.cacheDir, null, null) { result ->
                isProcessingImage.value = false
                lastDetectionResult.value = result

                result?.let {
                    Toast.makeText(context, "Manual Analysis: ${it.message}", Toast.LENGTH_LONG).show()
                    Log.d(TAG, "Manual analysis result: $it")
                } ?: run {
                    Toast.makeText(context, "Manual analysis failed", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Manual analysis returned no result")
                }
            }
        } ?: run {
            Toast.makeText(context, "No image available for analysis", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "No bitmap available for manual analysis")
        }
    }

    /**
     * Create an instance which interacts with the camera service via the given application context.
     */
    private fun getProcessCameraProvider(context: Context): MutableLiveData<ProcessCameraProvider> {
        if (cameraProviderLiveData.value == null) {
            val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> =
                ProcessCameraProvider.getInstance(context)

            viewModelScope.launch {
                try {
                    cameraProviderLiveData.value = cameraProviderFuture.get()
                    Log.d("CameraProvider", "Camera provider initialized successfully")
                } catch (e: ExecutionException) {
                    // Handle any errors (including cancellation) here.
                    Log.e(TAG, "Unhandled exception", e)
                } catch (e: InterruptedException) {
                    Log.e(TAG, "InterruptedException", e)
                }
            }
        }
        Log.d("CameraProviderX", "${cameraProviderLiveData.value}")
        return cameraProviderLiveData
    }

    fun changeCameraFacing(lensFacing: Int, context: Context, previewView: PreviewView) {
        this.lensFacing = lensFacing
        this.videoCapture = null
        this.imageCapture = null
        bindAllUseCase(context = context, previewView = previewView)
    }

    fun bindAllUseCase(
        context: Context,
        previewView: PreviewView,
    ) {
        // check camera permission
        requestAllPermission(context)
        if (cameraProviderLiveData.value == null) {
            cameraProviderLiveData = getProcessCameraProvider(context)
        }
        cameraProviderLiveData.value?.unbindAll()
        val preview = Preview.Builder().build().apply {
            setSurfaceProvider(previewView.surfaceProvider)
        }
        imageAnalysis = bindAnalysisUseCase(context)
        imageCapture = ImageCapture.Builder().build()

        if (videoCapture == null) {
            videoCapture = createVideoCaptureUseCase(context)
        }

        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        // bind life cycle with image capture
        cameraProviderLiveData.value?.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis, imageCapture)
        Log.d(TAG, "All use cases bound successfully")
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    private fun bindAnalysisUseCase(context: Context): ImageAnalysis {
        val analysisUseCase = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // Process only latest frame
            .setImageQueueDepth(1) // Keep only 1 frame in queue
            .setTargetResolution(android.util.Size(640, 480)) // Lower resolution for better performance
            .build()
        needUpdateGraphicOverlayImageSourceInfo = true
        var lastProcessedTime = 0L
        val PROCESS_INTERVAL_MS = 100L // Process at most every 100ms (10 FPS)

        analysisUseCase.setAnalyzer(
            ContextCompat.getMainExecutor(context),
            ImageAnalysis.Analyzer { imageProxy: ImageProxy ->
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastProcessedTime < PROCESS_INTERVAL_MS) {
                    imageProxy.close() // Skip this frame
                    return@Analyzer
                }
                lastProcessedTime = currentTime

                if (needUpdateGraphicOverlayImageSourceInfo) {
                    val isImageFlipped = lensFacing == CameraSelector.LENS_FACING_FRONT
                    Log.d("CameraViewModel", "isImageFlipped: $isImageFlipped")
                    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                    if (rotationDegrees == 0 || rotationDegrees == 180) {
                        graphicOverlay.setImageSourceInfo(imageProxy.width, imageProxy.height, isImageFlipped)
                    } else {
                        graphicOverlay.setImageSourceInfo(imageProxy.height, imageProxy.width, isImageFlipped)
                    }
                    needUpdateGraphicOverlayImageSourceInfo = false
                }
                try {
                    poseDetectorProcessor.processImageProxy(
                        image = imageProxy
                    ) { results ->
                        bitmap = BitmapUtils.getBitmap(imageProxy, graphicOverlay)
                        if (bitmap != null && results != null) {
                            onResults(bitmap!!, results)
                        }
                    }
                } catch (e: MlKitException) {
                    Log.e("Camera", "Failed to process image. Error: " + e.localizedMessage)
                    Toast.makeText(context, e.localizedMessage, Toast.LENGTH_SHORT).show()
                }
            }
        )
        return analysisUseCase
    }

    private fun createVideoCaptureUseCase(context: Context): VideoCapture<Recorder> {
        val qualitySelector = QualitySelector.from(
            Quality.LOWEST,
            FallbackStrategy.lowerQualityOrHigherThan(Quality.FHD)
        )
        val recorder = Recorder.Builder()
            .setExecutor(ContextCompat.getMainExecutor(context))
            .build()

        return VideoCapture.withOutput(recorder)
    }

    @SuppressLint("MissingPermission")
    fun startRecordingVideo(
        context: Context,
        videoCapture: VideoCapture<Recorder>,
        file_name: String,
        recordingStatus: (isCompleted: Boolean) -> Unit
    ) {
        if (recording != null) {
            recording?.stop()
            recording = null
            return
        }
        val audioEnabled = false
        val videoFile = File(context.filesDir, "$file_name.mp4")
        val outputOptions = FileOutputOptions.Builder(videoFile).build()

        recording = videoCapture.output
            .prepareRecording(context, outputOptions)
            .apply { if (audioEnabled) withAudioEnabled() }
            .start(ContextCompat.getMainExecutor(context)) { event ->

                when (event) {
                    is VideoRecordEvent.Finalize -> {
                        if (event.hasError()) {
                            recording?.close()
                            recording = null
                            Toast.makeText(
                                context,
                                "Video capture failed",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            val uri = event.outputResults.outputUri
                            if (uri != Uri.EMPTY) {
                                val uriEncoded = URLEncoder.encode(
                                    uri.toString(),
                                    StandardCharsets.UTF_8.toString()
                                )
                            }
                            Toast.makeText(
                                context,
                                "Video capture Succeeded",
                                Toast.LENGTH_LONG
                            ).show()
                            recordingStatus(true)
                        }
                    }
                    is VideoRecordEvent.Start -> {
                        Toast.makeText(
                            context,
                            "Video recording Started...",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    is VideoRecordEvent.Pause -> {}

                    is VideoRecordEvent.Resume -> {}
                }
            }
    }

    fun requestAllPermission(context: Context) {
        if (!hasRequiredPermissions(context)) {
            Log.d("Permission1", "permission check")
            ActivityCompat.requestPermissions(
                context as Activity, CAMERAX_PERMISSION, 0
            )
        }
    }

    private fun hasRequiredPermissions(context: Context): Boolean {
        return CAMERAX_PERMISSION.all {
            ContextCompat.checkSelfPermission(
                context,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun setMockApiMode(useMock: Boolean) {
        injuryDetectionAPI.setUseMockApi(useMock)
        Log.d(TAG, "Switched to ${if (useMock) "MOCK" else "REAL"} API mode")
    }

    fun testWebhookConnectivity(onResult: (Boolean, String) -> Unit) {
        injuryDetectionAPI.testWebhookConnectivity(onResult)
    }

    fun checkFilePermissions(context: Context): Boolean {
        val hasWritePermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            // For Android 10+, we can write to cache without permissions
            true
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }

        val hasReadPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            // For Android 10+, we can read from cache without permissions
            true
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }

        val cacheDir = context.cacheDir
        val cacheWritable = cacheDir?.canWrite() ?: false
        val cacheReadable = cacheDir?.canRead() ?: false

        Log.d(TAG, "File permissions check:")
        Log.d(TAG, "  - WRITE_EXTERNAL_STORAGE: $hasWritePermission")
        Log.d(TAG, "  - READ_EXTERNAL_STORAGE: $hasReadPermission")
        Log.d(TAG, "  - Cache directory writable: $cacheWritable")
        Log.d(TAG, "  - Cache directory readable: $cacheReadable")
        Log.d(TAG, "  - Cache directory path: ${cacheDir?.absolutePath}")
        Log.d(TAG, "  - Android API level: ${android.os.Build.VERSION.SDK_INT}")

        return hasWritePermission && hasReadPermission && cacheWritable && cacheReadable
    }

    fun manualTriggerImageCapture(context: Context) {
        Log.d(TAG, "Manual trigger for image capture initiated")

        // Check file permissions before proceeding
        val hasPermissions = checkFilePermissions(context)
        if (!hasPermissions) {
            Log.e(TAG, "File permissions check failed - requesting permissions")
            Toast.makeText(context, "File permissions required for image capture", Toast.LENGTH_LONG).show()
            requestAllPermission(context)
            return
        }

        Log.d(TAG, "File permissions OK - proceeding with image capture")
        captureAndAnalyzeImage(context)
    }

    companion object {
        val CAMERAX_PERMISSION = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            // File permissions for older Android versions
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }

    override fun onCleared() {
        super.onCleared()
        stopSpeechRecognition()
    }
}