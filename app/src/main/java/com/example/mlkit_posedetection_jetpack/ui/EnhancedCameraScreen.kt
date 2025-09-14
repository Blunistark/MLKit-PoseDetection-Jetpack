package com.example.mlkit_posedetection_jetpack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import com.example.mlkit_posedetection_jetpack.R
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mlkit_posedetection_jetpack.camera_usecase.CameraScreen
import com.example.mlkit_posedetection_jetpack.camera_usecase.CameraViewModel
import android.util.Log

@Composable
fun EnhancedCameraScreen() {
    val viewModel: CameraModesViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    var zoomLevel by remember { mutableStateOf(1f) }
    var cameraViewModel by remember { mutableStateOf<CameraViewModel?>(null) }
    val context = LocalContext.current
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview background
        CameraScreen(
            isFrontCamera = uiState.isFrontCamera,
            onCameraViewModelReady = { vm ->
                cameraViewModel = vm
            }
        )
        
        // Camera UI overlay with zoom, flash, etc.
        CameraUIOverlay(
            isFlashOn = uiState.isFlashOn,
            isFrontCamera = uiState.isFrontCamera,
            zoomLevel = zoomLevel,
            onFlashToggle = { viewModel.toggleFlash() },
            onCameraSwitch = { viewModel.switchCamera() },
            onZoomChange = { zoomLevel = it },
            onSettingsClick = { /* Open settings */ }
        )
        
        // Define camera modes
        val cameraModes = listOf("PHOTO", "VIDEO", "AI DOC", "INJURY", "POSE")
        
        // Manual trigger button for testing (only show in INJURY mode)
        if (cameraModes[uiState.selectedMode] == "INJURY") {
            ManualTriggerButton(
                onTrigger = {
                    cameraViewModel?.let { vm ->
                        Log.d("ManualTrigger", "Manual trigger clicked - calling camera capture")
                        vm.manualTriggerImageCapture(context)
                    } ?: run {
                        Log.e("ManualTrigger", "CameraViewModel not available")
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 200.dp)
            )
        }
        
        // Mode-specific overlays
        val currentMode = cameraModes[uiState.selectedMode]
        if (currentMode != "PHOTO" && currentMode != "VIDEO") {
            ModeSpecificOverlay(
                mode = currentMode,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Top overlay with mode info
        if (uiState.selectedMode != 0) { // Don't show for regular photo mode
            ModeInfoOverlay(
                mode = getModeInfo(uiState.selectedMode),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
            )
        }
        
        // Processing indicator
        if (uiState.isProcessing) {
            ProcessingOverlay(
                modifier = Modifier.align(Alignment.Center)
            )
        }
        
        // Results overlay
        uiState.injuryDetectionResult?.let { result ->
            ResultsOverlay(
                result = result,
                onDismiss = { viewModel.clearResults() },
                modifier = Modifier.align(Alignment.Center)
            )
        }
        
        // Error message
        uiState.errorMessage?.let { error ->
            ErrorOverlay(
                error = error,
                onDismiss = { viewModel.clearError() },
                modifier = Modifier.align(Alignment.Center)
            )
        }
        
        // Camera modes interface
        CameraModesInterface(
            viewModel = viewModel,
            uiState = uiState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun ModeInfoOverlay(
    mode: ModeInfo,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = mode.title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = mode.description,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ProcessingOverlay(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Processing...",
                color = Color.White,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun ResultsOverlay(
    result: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Analysis Result",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = result,
                color = Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White
                )
            ) {
                Text(
                    text = "OK",
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
fun ErrorOverlay(
    error: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Red.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Error",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = error,
                color = Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White
                )
            ) {
                Text(
                    text = "Dismiss",
                    color = Color.Red
                )
            }
        }
    }
}

@Composable
fun CameraModesInterface(
    viewModel: CameraModesViewModel,
    uiState: CameraUiState,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Camera modes selection
        val cameraModes = listOf(
            CameraMode("PHOTO", R.drawable.ic_photo_camera, "Capture photos"),
            CameraMode("VIDEO", R.drawable.ic_videocam, "Record videos"),
            CameraMode("AI DOC", R.drawable.ic_description, "AI Document scanner"),
            CameraMode("INJURY", R.drawable.ic_local_hospital, "Injury detection"),
            CameraMode("POSE", R.drawable.ic_accessibility, "Pose detection")
        )
        
        CameraModeSelector(
            modes = cameraModes,
            selectedMode = uiState.selectedMode,
            onModeSelected = { viewModel.selectMode(it) },
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Bottom controls
        EnhancedBottomControls(
            viewModel = viewModel,
            uiState = uiState,
            selectedMode = cameraModes[uiState.selectedMode]
        )
    }
}

@Composable
fun EnhancedBottomControls(
    viewModel: CameraModesViewModel,
    uiState: CameraUiState,
    selectedMode: CameraMode
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 32.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Gallery button
        IconButton(
            onClick = { /* Open gallery */ },
            modifier = Modifier
                .size(48.dp)
                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_photo_library),
                contentDescription = "Gallery",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        
        // Enhanced capture button
        EnhancedCaptureButton(
            mode = selectedMode,
            isRecording = uiState.isRecording,
            isProcessing = uiState.isProcessing,
            onClick = {
                if (selectedMode.name == "VIDEO") {
                    if (uiState.isRecording) {
                        viewModel.stopVideoRecording()
                    } else {
                        viewModel.startVideoRecording()
                    }
                } else {
                    viewModel.capturePhoto()
                }
            }
        )
        
        // Camera switch button
        IconButton(
            onClick = { viewModel.switchCamera() },
            modifier = Modifier
                .size(48.dp)
                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
        ) {
            Icon(
                painter = painterResource(id = if (uiState.isFrontCamera) R.drawable.ic_camera_front else R.drawable.ic_camera_rear),
                contentDescription = "Switch Camera",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun EnhancedCaptureButton(
    mode: CameraMode,
    isRecording: Boolean,
    isProcessing: Boolean,
    onClick: () -> Unit
) {
    val buttonColor = when {
        isProcessing -> Color.Gray
        isRecording -> Color.Red
        else -> Color.White
    }
    
    Box(
        modifier = Modifier
            .size(72.dp)
            .background(buttonColor, CircleShape)
            .then(
                if (!isProcessing) {
                    Modifier.background(Color.Gray.copy(alpha = 0.3f), CircleShape)
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isProcessing) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(32.dp),
                strokeWidth = 3.dp
            )
        } else {
            CaptureButton(mode = mode, onClick = onClick)
        }
    }
}

data class ModeInfo(
    val title: String,
    val description: String
)

fun getModeInfo(modeIndex: Int): ModeInfo {
    return when (modeIndex) {
        1 -> ModeInfo("Video Mode", "Tap to start/stop recording")
        2 -> ModeInfo("AI Document Scanner", "Position document in frame and capture")
        3 -> ModeInfo("Injury Detection", "Point camera at injury and say 'process img'")
        4 -> ModeInfo("Pose Detection", "Stand in frame for pose analysis")
        else -> ModeInfo("Photo Mode", "Tap to capture photo")
    }
}

@Composable
fun ManualTriggerButton(
    onTrigger: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onTrigger,
        modifier = modifier,
        containerColor = Color.Red.copy(alpha = 0.8f),
        contentColor = Color.White
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_photo_camera),
            contentDescription = "Manual Trigger",
            modifier = Modifier.size(24.dp)
        )
    }
}
