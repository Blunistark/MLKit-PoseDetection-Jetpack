@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.mlkit_posedetection_jetpack.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import com.example.mlkit_posedetection_jetpack.R
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FloatingActionButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mlkit_posedetection_jetpack.camera_usecase.CameraScreen
import com.example.mlkit_posedetection_jetpack.camera_usecase.CameraViewModel
import android.util.Log
import com.example.mlkit_posedetection_jetpack.ui.CameraModesViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import android.content.Context
import androidx.compose.runtime.LaunchedEffect
import org.json.JSONObject
import com.example.mlkit_posedetection_jetpack.camera_usecase.ChatMessage
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.mlkit_posedetection_jetpack.camera_usecase.EmergencyCacheResponse

// Constants for better maintainability
private object CameraUIConstants {
    val OVERLAY_BACKGROUND_ALPHA = 0.7f
    val BUTTON_SIZE_SMALL = 48.dp
    val BUTTON_SIZE_LARGE = 72.dp
    val BUTTON_ICON_SIZE = 24.dp
    val CAPTURE_ICON_SIZE = 32.dp
    val PROCESSING_INDICATOR_SIZE = 48.dp
    val CORNER_RADIUS_SMALL = 16.dp
    val CORNER_RADIUS_MEDIUM = 20.dp
    val PADDING_SMALL = 8.dp
    val PADDING_MEDIUM = 16.dp
    val PADDING_LARGE = 24.dp
    val ELEVATION_MEDIUM = 8.dp
    val ANIMATION_DURATION_SHORT = 200
    val ANIMATION_DURATION_MEDIUM = 300
    val ANIMATION_DURATION_LONG = 500
    val PADDING_XLARGE = 32.dp
}


// Enhanced theme colors
private object CameraTheme {
    val SurfaceOverlay = Color.Black.copy(alpha = CameraUIConstants.OVERLAY_BACKGROUND_ALPHA)
    val ProcessingOverlay = Color.Black.copy(alpha = 0.8f)
    val ResultsOverlay = Color.Black.copy(alpha = 0.9f)
    val ErrorOverlay = Color.Red.copy(alpha = 0.9f)
    val RecordingIndicator = Color.Red
    val CaptureButtonNormal = Color.White
    val CaptureButtonDisabled = Color.Gray
    val ManualTriggerButton = Color.Red.copy(alpha = 0.8f)
    val SuccessColor = Color.Green
}

/**
 * Enhanced camera screen with improved animations, accessibility, and performance optimizations
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun EnhancedCameraScreen() {
    val viewModel: CameraModesViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    var zoomLevel by remember { mutableStateOf(1f) }
    var cameraViewModel by remember { mutableStateOf<CameraViewModel?>(null) }
    val context = LocalContext.current

    // Store emergency cache in SharedPreferences when it changes
    LaunchedEffect(uiState.emergencyCache) {
        uiState.emergencyCache?.let { cache ->
            val sharedPrefs = context.getSharedPreferences("emergency_prefs", Context.MODE_PRIVATE)
            // For now, just store a simple string representation
            val cacheString = "Call ID: ${cache.callId}, Location: ${cache.location}"
            sharedPrefs.edit().putString("emergency_cache", cacheString).apply()
        }
    }

    // Derived states for better performance
    val cameraModes = remember {
        listOf("PHOTO", "VIDEO", "AI DOC", "INJURY", "POSE")
    }
    val currentMode = remember(uiState.selectedMode) {
        cameraModes.getOrElse(uiState.selectedMode) { "PHOTO" }
    }
    val showModeSpecificOverlay = remember(currentMode) {
        currentMode != "PHOTO" && currentMode != "VIDEO"
    }
    val showModeInfoOverlay = remember(uiState.selectedMode) {
        uiState.selectedMode != 0
    }
    val isInjuryMode = remember(currentMode) {
        currentMode == "INJURY"
    }

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

        // Enhanced manual trigger button with animation (only show in INJURY mode)
        AnimatedVisibility(
            visible = isInjuryMode,
            enter = slideInVertically(
                animationSpec = tween(CameraUIConstants.ANIMATION_DURATION_MEDIUM)
            ) + fadeIn(),
            exit = slideOutVertically(
                animationSpec = tween(CameraUIConstants.ANIMATION_DURATION_MEDIUM)
            ) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 200.dp)
        ) {
            EnhancedManualTriggerButton(
                onTrigger = {
                    cameraViewModel?.let { vm ->
                        Log.d("ManualTrigger", "Manual trigger clicked - calling camera capture")
                        vm.manualTriggerImageCapture(context)
                    } ?: run {
                        Log.e("ManualTrigger", "CameraViewModel not available")
                    }
                },
                isEnabled = !uiState.isProcessing
            )
        }

        // Enhanced mode-specific overlays with animations
        AnimatedVisibility(
            visible = showModeSpecificOverlay,
            enter = fadeIn(animationSpec = tween(CameraUIConstants.ANIMATION_DURATION_SHORT)),
            exit = fadeOut(animationSpec = tween(CameraUIConstants.ANIMATION_DURATION_SHORT))
        ) {
            ModeSpecificOverlay(
                mode = currentMode,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Enhanced top overlay with mode info and animations
        AnimatedVisibility(
            visible = showModeInfoOverlay,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(CameraUIConstants.ANIMATION_DURATION_MEDIUM)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(CameraUIConstants.ANIMATION_DURATION_MEDIUM)
            ) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
        ) {
            EnhancedModeInfoOverlay(
                mode = getModeInfo(uiState.selectedMode)
            )
        }

        // Enhanced processing indicator with pulse animation
        AnimatedVisibility(
            visible = uiState.isProcessing,
            enter = scaleIn(animationSpec = tween(CameraUIConstants.ANIMATION_DURATION_SHORT)) + fadeIn(),
            exit = scaleOut(animationSpec = tween(CameraUIConstants.ANIMATION_DURATION_SHORT)) + fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            EnhancedProcessingOverlay()
        }

        // Enhanced results overlay with slide animation
        uiState.injuryDetectionResult?.let { result ->
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(CameraUIConstants.ANIMATION_DURATION_MEDIUM)
                ) + fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                EnhancedResultsOverlay(
                    result = result,
                    onDismiss = { viewModel.clearResults() }
                )
            }
        }

        // Enhanced error overlay with shake animation
        uiState.errorMessage?.let { error ->
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessHigh
                    )
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = tween(CameraUIConstants.ANIMATION_DURATION_MEDIUM)
                ) + fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                EnhancedErrorOverlay(
                    error = error,
                    onDismiss = { viewModel.clearError() }
                )
            }
        }

        // Emergency cache overlay
        uiState.emergencyCache?.let { cache ->
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(CameraUIConstants.ANIMATION_DURATION_MEDIUM)
                ) + fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                EmergencyCacheOverlay(
                    cache = cache,
                    onDismiss = { viewModel.clearEmergencyCache() }
                )
            }
        }

        // First Aid Chatbot Overlay
        uiState.emergencyCache?.let { cache ->
            AnimatedVisibility(
                visible = true,
                enter = slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(CameraUIConstants.ANIMATION_DURATION_MEDIUM)
                ) + fadeIn(),
                exit = slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(CameraUIConstants.ANIMATION_DURATION_MEDIUM)
                ) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                FirstAidChatbotOverlay(
                    cache = cache,
                    onDismiss = { /* Handle dismiss if needed */ }
                )
            }
        }

        // Enhanced camera modes interface with slide animation
        AnimatedVisibility(
            visible = true,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(CameraUIConstants.ANIMATION_DURATION_MEDIUM)
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(CameraUIConstants.ANIMATION_DURATION_MEDIUM)
            ),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            EnhancedCameraModesInterface(
                viewModel = viewModel,
                uiState = uiState
            )
        }

        // Chat overlay
        AnimatedVisibility(
            visible = uiState.isChatOpen,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(CameraUIConstants.ANIMATION_DURATION_MEDIUM)
            ),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(CameraUIConstants.ANIMATION_DURATION_MEDIUM)
            ),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            ChatOverlay(
                messages = uiState.chatMessages,
                isTyping = uiState.isTyping,
                onSendMessage = { viewModel.sendChatMessage(it) },
                onClose = { viewModel.toggleChat() },
                onClear = { viewModel.clearChat() }
            )
        }

        // Floating chat button
        AnimatedVisibility(
            visible = !uiState.isChatOpen,
            enter = scaleIn(animationSpec = tween(CameraUIConstants.ANIMATION_DURATION_SHORT)),
            exit = scaleOut(animationSpec = tween(CameraUIConstants.ANIMATION_DURATION_SHORT)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 120.dp, end = CameraUIConstants.PADDING_MEDIUM)
        ) {
            FloatingActionButton(
                onClick = { viewModel.toggleChat() },
                containerColor = Color(0xFF007AFF),
                contentColor = Color.White,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Chat,
                    contentDescription = "Open Chat",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Enhanced mode info overlay with improved styling and accessibility
 */
@Composable
private fun EnhancedModeInfoOverlay(
    mode: ModeInfo,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .semantics {
                contentDescription = "Camera mode: ${mode.title}. ${mode.description}"
            },
        colors = CardDefaults.cardColors(
            containerColor = CameraTheme.SurfaceOverlay
        ),
        shape = RoundedCornerShape(CameraUIConstants.CORNER_RADIUS_MEDIUM),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(CameraUIConstants.PADDING_MEDIUM),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = mode.title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = mode.description,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/**
 * Enhanced processing overlay with pulse animation
 */
@Composable
private fun EnhancedProcessingOverlay(modifier: Modifier = Modifier) {
    // Pulse animation for the processing indicator
    val pulseAnimation = rememberInfiniteTransition(label = "pulse")
    val scale by pulseAnimation.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(CameraUIConstants.ANIMATION_DURATION_LONG),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = CameraTheme.ProcessingOverlay
        ),
        shape = RoundedCornerShape(CameraUIConstants.CORNER_RADIUS_SMALL),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(CameraUIConstants.PADDING_LARGE),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier
                    .size(CameraUIConstants.PROCESSING_INDICATOR_SIZE)
                    .scale(scale),
                strokeWidth = 4.dp
            )
            Spacer(modifier = Modifier.height(CameraUIConstants.PADDING_MEDIUM))
            Text(
                text = "Processing...",
                color = Color.White,
                fontSize = 16.sp,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics {
                    contentDescription = "Processing image, please wait"
                }
            )
        }
    }
}

/**
 * Enhanced results overlay with better styling and animations
 */
@Composable
private fun EnhancedResultsOverlay(
    result: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(CameraUIConstants.PADDING_XLARGE)
            .fillMaxWidth(0.9f),
        colors = CardDefaults.cardColors(
            containerColor = CameraTheme.ResultsOverlay
        ),
        shape = RoundedCornerShape(CameraUIConstants.CORNER_RADIUS_SMALL),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(CameraUIConstants.PADDING_LARGE),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Success indicator using text instead of icon
            Text(
                text = "✓",
                color = CameraTheme.SuccessColor,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(CameraUIConstants.PADDING_MEDIUM))
            Text(
                text = "Analysis Result",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = result,
                color = Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics {
                    contentDescription = "Analysis result: $result"
                }
            )
            Spacer(modifier = Modifier.height(CameraUIConstants.PADDING_MEDIUM))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(CameraUIConstants.CORNER_RADIUS_SMALL),
                modifier = Modifier.semantics {
                    contentDescription = "Dismiss result"
                }
            ) {
                Text(
                    text = "OK",
                    color = Color.Black,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Enhanced error overlay with improved accessibility and styling
 */
@Composable
private fun EnhancedErrorOverlay(
    error: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(CameraUIConstants.PADDING_XLARGE)
            .fillMaxWidth(0.9f),
        colors = CardDefaults.cardColors(
            containerColor = CameraTheme.ErrorOverlay
        ),
        shape = RoundedCornerShape(CameraUIConstants.CORNER_RADIUS_SMALL),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(CameraUIConstants.PADDING_LARGE),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Error indicator using text instead of icon
            Text(
                text = "⚠",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(CameraUIConstants.PADDING_MEDIUM))
            Text(
                text = "Error",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = error,
                color = Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics {
                    contentDescription = "Error: $error"
                }
            )
            Spacer(modifier = Modifier.height(CameraUIConstants.PADDING_MEDIUM))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(CameraUIConstants.CORNER_RADIUS_SMALL),
                modifier = Modifier.semantics {
                    contentDescription = "Dismiss error message"
                }
            ) {
                Text(
                    text = "Dismiss",
                    color = Color.Red,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Enhanced camera modes interface with better organization
 */
@Composable
private fun EnhancedCameraModesInterface(
    viewModel: CameraModesViewModel,
    uiState: CameraUiState,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Camera modes selection
        val cameraModes = remember {
            listOf(
                CameraMode("PHOTO", R.drawable.ic_photo_camera, "Capture photos"),
                CameraMode("VIDEO", R.drawable.ic_videocam, "Record videos"),
                CameraMode("AI DOC", R.drawable.ic_description, "AI Document scanner"),
                CameraMode("INJURY", R.drawable.ic_local_hospital, "Injury detection"),
                CameraMode("POSE", R.drawable.ic_accessibility, "Pose detection")
            )
        }

        CameraModeSelector(
            modes = cameraModes,
            selectedMode = uiState.selectedMode,
            onModeSelected = { viewModel.selectMode(it) },
            modifier = Modifier.padding(bottom = CameraUIConstants.PADDING_LARGE)
        )

        // Enhanced bottom controls
        EnhancedBottomControls(
            viewModel = viewModel,
            uiState = uiState,
            selectedMode = cameraModes[uiState.selectedMode]
        )
    }
}

/**
 * Enhanced bottom controls with improved accessibility and animations
 */
@Composable
private fun EnhancedBottomControls(
    viewModel: CameraModesViewModel,
    uiState: CameraUiState,
    selectedMode: CameraMode
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = CameraUIConstants.PADDING_XLARGE, vertical = CameraUIConstants.PADDING_XLARGE),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Emergency cache button
        EnhancedControlButton(
            onClick = { viewModel.fetchEmergencyCache() },
            iconRes = R.drawable.ic_local_hospital, // Reuse hospital icon
            contentDescription = "Fetch emergency cache",
            enabled = !uiState.isProcessing
        )

        // Enhanced gallery button
        EnhancedControlButton(
            onClick = { /* Open gallery */ },
            iconRes = R.drawable.ic_photo_library,
            contentDescription = "Open gallery",
            enabled = true
        )

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

        // Enhanced camera switch button
        EnhancedControlButton(
            onClick = { viewModel.switchCamera() },
            iconRes = if (uiState.isFrontCamera) R.drawable.ic_camera_front else R.drawable.ic_camera_rear,
            contentDescription = if (uiState.isFrontCamera) "Switch to rear camera" else "Switch to front camera",
            enabled = !uiState.isProcessing
        )
    }
}

/**
 * Enhanced control button with consistent styling and animations
 */
@Composable
private fun EnhancedControlButton(
    onClick: () -> Unit,
    iconRes: Int,
    contentDescription: String,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.8f,
        animationSpec = tween(CameraUIConstants.ANIMATION_DURATION_SHORT),
        label = "button_scale"
    )

    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .size(CameraUIConstants.BUTTON_SIZE_SMALL)
            .scale(scale)
            .background(
                CameraTheme.SurfaceOverlay,
                CircleShape
            )
            .semantics {
                this.contentDescription = contentDescription
            }
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = if (enabled) Color.White else Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(CameraUIConstants.BUTTON_ICON_SIZE)
        )
    }
}

/**
 * Enhanced capture button with improved animations and states
 */
@Composable
private fun EnhancedCaptureButton(
    mode: CameraMode,
    isRecording: Boolean,
    isProcessing: Boolean,
    onClick: () -> Unit
) {
    val buttonColor = when {
        isProcessing -> CameraTheme.CaptureButtonDisabled
        isRecording -> CameraTheme.RecordingIndicator
        else -> CameraTheme.CaptureButtonNormal
    }

    val scale by animateFloatAsState(
        targetValue = if (isRecording) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "capture_button_scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isProcessing) 0.7f else 1f,
        animationSpec = tween(CameraUIConstants.ANIMATION_DURATION_SHORT),
        label = "capture_button_alpha"
    )

    Box(
        modifier = Modifier
            .size(CameraUIConstants.BUTTON_SIZE_LARGE)
            .scale(scale)
            .alpha(alpha)
            .background(buttonColor, CircleShape)
            .clip(CircleShape)
            .semantics {
                contentDescription = when {
                    isProcessing -> "Processing, please wait"
                    isRecording -> "Stop recording"
                    mode.name == "VIDEO" -> "Start recording"
                    else -> "Capture ${mode.name.lowercase()}"
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (isProcessing) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(CameraUIConstants.CAPTURE_ICON_SIZE),
                strokeWidth = 3.dp
            )
        } else {
            CaptureButton(mode = mode, onClick = onClick)
        }
    }
}

/**
 * Enhanced manual trigger button with improved animations and accessibility
 */
@Composable
private fun EnhancedManualTriggerButton(
    onTrigger: () -> Unit,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isEnabled) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "manual_trigger_scale"
    )

    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    FloatingActionButton(
        onClick = onTrigger,
        modifier = modifier
            .scale(scale)
            .alpha(if (isEnabled) pulseAlpha else 0.5f)
            .semantics {
                contentDescription = if (isEnabled) {
                    "Manual trigger for injury detection"
                } else {
                    "Manual trigger disabled during processing"
                }
            },
        containerColor = CameraTheme.ManualTriggerButton,
        contentColor = Color.White,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 8.dp,
            pressedElevation = 12.dp
        )
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_photo_camera),
            contentDescription = null,
            modifier = Modifier.size(CameraUIConstants.BUTTON_ICON_SIZE)
        )
    }
}

/**
 * Emergency cache overlay to display fetched emergency data
 */
@Composable
private fun EmergencyCacheOverlay(
    cache: EmergencyCacheResponse,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth(0.9f)
            .fillMaxHeight(0.8f)
            .padding(CameraUIConstants.PADDING_MEDIUM),
        colors = CardDefaults.cardColors(
            containerColor = CameraTheme.ResultsOverlay
        ),
        shape = RoundedCornerShape(CameraUIConstants.CORNER_RADIUS_MEDIUM),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(CameraUIConstants.PADDING_MEDIUM)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Emergency Cache",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(CameraUIConstants.PADDING_MEDIUM))

            // Call details
            Text("Call ID: ${cache.callId}", color = Color.White, fontWeight = FontWeight.Bold)
            Text("Location: ${cache.location}", color = Color.White)
            Text("Reported Time: ${cache.reportedTime}", color = Color.White)
            Text("Call Type: ${cache.callType}", color = Color.White)
            Text("Severity: ${cache.severity}", color = Color.White)

            Spacer(modifier = Modifier.height(CameraUIConstants.PADDING_MEDIUM))

            // Patient info
            Text("Patient:", color = Color.Yellow, fontWeight = FontWeight.Bold)
            Text("Name: ${cache.patient.name}", color = Color.White)
            Text("Age: ${cache.patient.age}", color = Color.White)
            Text("Condition: ${cache.patient.condition}", color = Color.White)

            Spacer(modifier = Modifier.height(CameraUIConstants.PADDING_SMALL))

            Text("Vitals:", color = Color.Cyan, fontWeight = FontWeight.Bold)
            Text("Heart Rate: ${cache.patient.vitals.heartRate}", color = Color.White)
            Text("Blood Pressure: ${cache.patient.vitals.bloodPressure}", color = Color.White)
            Text("Temperature: ${cache.patient.vitals.temperature}", color = Color.White)
            Text("Oxygen: ${cache.patient.vitals.oxygen}", color = Color.White)

            Spacer(modifier = Modifier.height(CameraUIConstants.PADDING_MEDIUM))

            // Medical insights
            Text("Medical Insights:", color = Color.Green, fontWeight = FontWeight.Bold)
            Text("Immediate Concerns:", color = Color.White, fontWeight = FontWeight.Bold)
            cache.medicalInsights.immediateConcerns.forEach { concern ->
                Text("• $concern", color = Color.White)
            }

            Spacer(modifier = Modifier.height(CameraUIConstants.PADDING_SMALL))

            Text("Treatment Priorities:", color = Color.White, fontWeight = FontWeight.Bold)
            cache.medicalInsights.treatmentPriorities.forEach { priority ->
                Text("• $priority", color = Color.White)
            }

            Spacer(modifier = Modifier.height(CameraUIConstants.PADDING_SMALL))

            Text("Recommended Equipment:", color = Color.White, fontWeight = FontWeight.Bold)
            cache.medicalInsights.recommendedEquipment.forEach { equipment ->
                Text("• $equipment", color = Color.White)
            }

            Spacer(modifier = Modifier.height(CameraUIConstants.PADDING_MEDIUM))

            // Location assessment
            Text("Location Assessment:", color = Color.Magenta, fontWeight = FontWeight.Bold)
            Text("Accessibility: ${cache.locationAssessment.accessibility}", color = Color.White)
            Text("Area Type: ${cache.locationAssessment.areaType}", color = Color.White)
            Text("Navigation Concerns: ${cache.locationAssessment.navigationConcerns}", color = Color.White)

            Spacer(modifier = Modifier.height(CameraUIConstants.PADDING_SMALL))

            Text("Hazards:", color = Color.White, fontWeight = FontWeight.Bold)
            cache.locationAssessment.hazards.forEach { hazard ->
                Text("• $hazard", color = Color.White)
            }

            Spacer(modifier = Modifier.height(CameraUIConstants.PADDING_MEDIUM))

            Text("Estimated ETA: ${cache.estimatedETA}", color = Color.Yellow, fontWeight = FontWeight.Bold)
            Text("AI Confidence: ${cache.aiConfidence}", color = Color.Cyan)
            Text("Analysis Complete: ${if (cache.analysisComplete) "Yes" else "No"}", color = Color.White)
        }
    }
}

/**
 * First Aid Chatbot Overlay with glass effect
 */
@Composable
private fun FirstAidChatbotOverlay(
    cache: EmergencyCacheResponse,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(300.dp)
            .height(400.dp)
            .padding(CameraUIConstants.PADDING_MEDIUM),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.7f) // Glass effect
        ),
        shape = RoundedCornerShape(CameraUIConstants.CORNER_RADIUS_MEDIUM),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(CameraUIConstants.PADDING_MEDIUM)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "First Aid Guidance",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(CameraUIConstants.PADDING_MEDIUM))

            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .weight(1f)
            ) {
                Text("Patient Condition:", color = Color.Yellow, fontWeight = FontWeight.Bold)
                Text(cache.patient.condition, color = Color.White)

                Spacer(modifier = Modifier.height(CameraUIConstants.PADDING_SMALL))

                Text("Immediate Concerns:", color = Color.Red, fontWeight = FontWeight.Bold)
                cache.medicalInsights.immediateConcerns.forEach { concern ->
                    Text("• $concern", color = Color.White)
                }

                Spacer(modifier = Modifier.height(CameraUIConstants.PADDING_SMALL))

                Text("Treatment Priorities:", color = Color.Cyan, fontWeight = FontWeight.Bold)
                cache.medicalInsights.treatmentPriorities.forEach { priority ->
                    Text("• $priority", color = Color.White)
                }

                Spacer(modifier = Modifier.height(CameraUIConstants.PADDING_SMALL))

                Text("Recommended Equipment:", color = Color.Green, fontWeight = FontWeight.Bold)
                cache.medicalInsights.recommendedEquipment.forEach { equipment ->
                    Text("• $equipment", color = Color.White)
                }

                Spacer(modifier = Modifier.height(CameraUIConstants.PADDING_SMALL))

                Text("Hospital Prep:", color = Color.Magenta, fontWeight = FontWeight.Bold)
                Text(cache.medicalInsights.hospitalPrep, color = Color.White)

                Spacer(modifier = Modifier.height(CameraUIConstants.PADDING_SMALL))

                Text("Estimated ETA: ${cache.estimatedETA}", color = Color.Yellow)
                Text("AI Confidence: ${cache.aiConfidence}", color = Color.Cyan)
            }
        }
    }
}

@Composable
fun ChatOverlay(
    messages: List<ChatMessage>,
    isTyping: Boolean,
    onSendMessage: (String) -> Unit,
    onClose: () -> Unit,
    onClear: () -> Unit
) {
    var messageText by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .width(320.dp)
            .height(500.dp)
            .padding(CameraUIConstants.PADDING_MEDIUM),
        shape = RoundedCornerShape(CameraUIConstants.CORNER_RADIUS_MEDIUM),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = CameraUIConstants.ELEVATION_MEDIUM
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Chat header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(CameraUIConstants.PADDING_MEDIUM),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "First Aid Assistant",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF007AFF)
                )
                Row {
                    IconButton(onClick = onClear) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = "Clear Chat",
                            tint = Color.Gray
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close Chat",
                            tint = Color.Gray
                        )
                    }
                }
            }

            Divider()

            // Messages list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = CameraUIConstants.PADDING_MEDIUM),
                reverseLayout = true
            ) {
                items(messages.reversed()) { message ->
                    ChatMessageItem(message = message)
                }
                if (isTyping) {
                    item {
                        TypingIndicator()
                    }
                }
            }

            Divider()

            // Message input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(CameraUIConstants.PADDING_MEDIUM),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text("Ask about first aid...") },
                    modifier = Modifier.weight(1f),
                    maxLines = 3,
                    shape = RoundedCornerShape(CameraUIConstants.CORNER_RADIUS_SMALL)
                )
                Spacer(modifier = Modifier.width(CameraUIConstants.PADDING_SMALL))
                IconButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            onSendMessage(messageText)
                            messageText = ""
                        }
                    },
                    enabled = messageText.isNotBlank(),
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = if (messageText.isNotBlank()) Color(0xFF007AFF) else Color.Gray,
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Send,
                        contentDescription = "Send Message",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(message: ChatMessage) {
    val isUser = message.isUser

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = CameraUIConstants.PADDING_SMALL),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = if (isUser) CameraUIConstants.CORNER_RADIUS_MEDIUM else 0.dp,
                topEnd = if (isUser) 0.dp else CameraUIConstants.CORNER_RADIUS_MEDIUM,
                bottomStart = CameraUIConstants.CORNER_RADIUS_MEDIUM,
                bottomEnd = CameraUIConstants.CORNER_RADIUS_MEDIUM
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) Color(0xFF007AFF) else Color(0xFFF0F0F0)
            )
        ) {
            Text(
                text = message.message,
                modifier = Modifier.padding(CameraUIConstants.PADDING_MEDIUM),
                color = if (isUser) Color.White else Color.Black,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun TypingIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = CameraUIConstants.PADDING_SMALL),
        horizontalArrangement = Arrangement.Start
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 0.dp,
                topEnd = CameraUIConstants.CORNER_RADIUS_MEDIUM,
                bottomStart = CameraUIConstants.CORNER_RADIUS_MEDIUM,
                bottomEnd = CameraUIConstants.CORNER_RADIUS_MEDIUM
            ),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF0F0F0)
            )
        ) {
            Row(
                modifier = Modifier.padding(CameraUIConstants.PADDING_MEDIUM),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Assistant is typing",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.width(CameraUIConstants.PADDING_SMALL))
                // Simple typing dots animation
                Row {
                    repeat(3) { index ->
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(Color.Gray, CircleShape)
                        )
                        if (index < 2) {
                            Spacer(modifier = Modifier.width(2.dp))
                        }
                    }
                }
            }
        }
    }
}

// Keep existing data classes and functions unchanged
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
