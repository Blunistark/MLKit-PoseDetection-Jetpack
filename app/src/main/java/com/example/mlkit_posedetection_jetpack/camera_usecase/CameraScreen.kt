package com.example.mlkit_posedetection_jetpack.camera_usecase

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mlkit_posedetection_jetpack.R
import com.example.mlkit_posedetection_jetpack.posedetector.graphic.GraphicOverlay
import com.example.mlkit_posedetection_jetpack.posedetector.graphic.PoseGraphic
import com.google.mlkit.vision.pose.Pose

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.P)
@SuppressLint("UnsafeOptInUsageError", "RememberReturnType")
@Composable
fun CameraScreen(
    isFrontCamera: Boolean = false,
    onCameraViewModelReady: ((CameraViewModel) -> Unit)? = null
) {
    val context = LocalContext.current
    val previewView = remember { PreviewView(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val graphicOverlay = remember { GraphicOverlay() }
    val poseResult = remember { mutableStateOf<Pose?>(null) }
    val bitmapImage = remember { mutableStateOf<Bitmap?>(null) }
    val cameraSelector = if (isFrontCamera) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK

    val cameraViewModel: CameraViewModel = remember(isFrontCamera) {
        CameraViewModel(
            graphicOverlay = graphicOverlay,
            lifecycleOwner = lifecycleOwner,
            cameraSelector = cameraSelector,
            onResults = { bitmap, pose ->
                bitmapImage.value?.recycle()
                bitmapImage.value = bitmap
                poseResult.value = pose
            }
        )
    }

    // Provide the CameraViewModel to the parent composable
    LaunchedEffect(cameraViewModel) {
        onCameraViewModelReady?.invoke(cameraViewModel)
    }

    // Initialize speech recognition when the composable is first created
    DisposableEffect(cameraViewModel) {
        cameraViewModel.initializeSpeechRecognition(context)
        onDispose {
            cameraViewModel.stopSpeechRecognition()
        }
    }

    // Trigger camera rebind when camera facing changes
    DisposableEffect(isFrontCamera) {
        cameraViewModel.changeCameraFacing(cameraSelector, context, previewView)
        onDispose { }
    }

    // Bind camera once when composable is first created
    LaunchedEffect(Unit) {
        cameraViewModel.bindAllUseCase(context, previewView)
    }

    Scaffold{ padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .onGloballyPositioned { screen->
                    graphicOverlay.updateGraphicOverlay(
                        width = screen.size.width.toFloat(),
                        height = screen.size.height.toFloat(),
                    )
                    // Removed bindAllUseCase call from here to prevent camera closing/reopening
                }
        ) {
            CameraPreview(
                previewView = previewView,
                modifier = Modifier
                    .fillMaxSize()
            )

            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                if (bitmapImage.value != null && poseResult.value!=null) {
//                    graphicOverlay.add(CameraImageGraphic(graphicOverlay, bitmapImage.value!!))
                    graphicOverlay.add(PoseGraphic(graphicOverlay, poseResult.value!!))
                    graphicOverlay.onDraw(this)
                    graphicOverlay.clear()
                }
            }
            
            // Speech recognition and injury detection status overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Speech listening status
                Card(
                    modifier = Modifier.padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (cameraViewModel.isListening.value) 
                            Color.Green.copy(alpha = 0.8f) 
                        else 
                            Color.Gray.copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (cameraViewModel.isListening.value) 
                            "🎤 Listening... Say 'process img'" 
                        else 
                            "🎤 Speech recognition inactive",
                        modifier = Modifier.padding(12.dp),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Processing status
                if (cameraViewModel.isProcessingImage.value) {
                    Card(
                        modifier = Modifier.padding(bottom = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Blue.copy(alpha = 0.8f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "🔍 Analyzing image for injuries...",
                            modifier = Modifier.padding(12.dp),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Last detection result
                cameraViewModel.lastDetectionResult.value?.let { result ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (result.hasInjury) 
                                Color.Red.copy(alpha = 0.9f) 
                            else 
                                Color.Green.copy(alpha = 0.9f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (result.hasInjury) "⚠️ INJURY DETECTED" else "✅ NO INJURY",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (result.hasInjury) {
                                Text(
                                    text = "Type: ${result.injuryType}",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            Text(
                                text = "Confidence: ${(result.confidence * 100).toInt()}%",
                                color = Color.White,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }

}
