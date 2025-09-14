package com.example.mlkit_posedetection_jetpack.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import com.example.mlkit_posedetection_jetpack.R

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

@Composable
fun CameraUIOverlay(
    isFlashOn: Boolean,
    isFrontCamera: Boolean,
    zoomLevel: Float,
    onFlashToggle: () -> Unit,
    onCameraSwitch: () -> Unit,
    onZoomChange: (Float) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Top controls
        TopCameraControls(
            isFlashOn = isFlashOn,
            isFrontCamera = isFrontCamera,
            onFlashToggle = onFlashToggle,
            onCameraSwitch = onCameraSwitch,
            onSettingsClick = onSettingsClick,
            modifier = Modifier.align(Alignment.TopCenter)
        )
        
        // Side zoom control
        ZoomControl(
            zoomLevel = zoomLevel,
            onZoomChange = onZoomChange,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
        )
        
        // Side camera flip button
        CameraFlipButton(
            isFrontCamera = isFrontCamera,
            onCameraSwitch = onCameraSwitch,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 100.dp)
        )
        
        // Camera focus indicator (when tapping to focus)
        FocusIndicator(
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun TopCameraControls(
    isFlashOn: Boolean,
    isFrontCamera: Boolean,
    onFlashToggle: () -> Unit,
    onCameraSwitch: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .statusBarsPadding(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Settings button
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                .size(48.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_settings),
                contentDescription = "Settings",
                tint = Color.White
            )
        }
        
        // Flash controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onFlashToggle,
                modifier = Modifier
                    .background(
                        if (isFlashOn) Color.Yellow.copy(alpha = 0.8f) 
                        else Color.Black.copy(alpha = 0.5f), 
                        CircleShape
                    )
                    .size(48.dp)
            ) {
                Icon(
                    painter = painterResource(id = if (isFlashOn) R.drawable.ic_flash_on else R.drawable.ic_flash_off),
                    contentDescription = "Flash",
                    tint = if (isFlashOn) Color.Black else Color.White
                )
            }
            
            IconButton(
                onClick = { /* Timer */ },
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .size(48.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_timer),
                    contentDescription = "Timer",
                    tint = Color.White
                )
            }
            
            // Camera flip button
            IconButton(
                onClick = onCameraSwitch,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .size(48.dp)
            ) {
                Icon(
                    painter = painterResource(
                        id = if (isFrontCamera) R.drawable.ic_camera_front else R.drawable.ic_camera_rear
                    ),
                    contentDescription = "Switch Camera",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun ZoomControl(
    zoomLevel: Float,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var isDragging by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .height(200.dp)
            .width(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Zoom level indicator
        Card(
            modifier = Modifier.padding(bottom = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "${(zoomLevel * 10).toInt() / 10f}x",
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        
        // Zoom slider
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(160.dp)
                .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { isDragging = false }
                    ) { _, dragAmount ->
                        val newZoom = (zoomLevel - dragAmount.y / density.density / 100f)
                            .coerceIn(1f, 10f)
                        onZoomChange(newZoom)
                    }
                }
        ) {
            // Zoom indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(Color.White, CircleShape)
                    .align(Alignment.TopCenter)
                    .offset(y = (160.dp * (1f - (zoomLevel - 1f) / 9f)))
            )
        }
    }
}

@Composable
fun FocusIndicator(
    modifier: Modifier = Modifier,
    isVisible: Boolean = false
) {
    if (isVisible) {
        Canvas(
            modifier = modifier.size(80.dp)
        ) {
            drawFocusSquare()
        }
    }
}

private fun DrawScope.drawFocusSquare() {
    val strokeWidth = 2.dp.toPx()
    val cornerLength = 20.dp.toPx()
    
    // Top-left corner
    drawLine(
        color = Color.White,
        start = Offset(0f, 0f),
        end = Offset(cornerLength, 0f),
        strokeWidth = strokeWidth
    )
    drawLine(
        color = Color.White,
        start = Offset(0f, 0f),
        end = Offset(0f, cornerLength),
        strokeWidth = strokeWidth
    )
    
    // Top-right corner
    drawLine(
        color = Color.White,
        start = Offset(size.width - cornerLength, 0f),
        end = Offset(size.width, 0f),
        strokeWidth = strokeWidth
    )
    drawLine(
        color = Color.White,
        start = Offset(size.width, 0f),
        end = Offset(size.width, cornerLength),
        strokeWidth = strokeWidth
    )
    
    // Bottom-left corner
    drawLine(
        color = Color.White,
        start = Offset(0f, size.height - cornerLength),
        end = Offset(0f, size.height),
        strokeWidth = strokeWidth
    )
    drawLine(
        color = Color.White,
        start = Offset(0f, size.height),
        end = Offset(cornerLength, size.height),
        strokeWidth = strokeWidth
    )
    
    // Bottom-right corner
    drawLine(
        color = Color.White,
        start = Offset(size.width, size.height - cornerLength),
        end = Offset(size.width, size.height),
        strokeWidth = strokeWidth
    )
    drawLine(
        color = Color.White,
        start = Offset(size.width - cornerLength, size.height),
        end = Offset(size.width, size.height),
        strokeWidth = strokeWidth
    )
}

@Composable
fun ModeSpecificOverlay(
    mode: String,
    modifier: Modifier = Modifier
) {
    when (mode) {
        "AI DOC" -> DocumentScanOverlay(modifier)
        "INJURY" -> InjuryDetectionOverlay(modifier)
        "POSE" -> PoseDetectionOverlay(modifier)
    }
}

@Composable
fun DocumentScanOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .aspectRatio(0.7f)
        ) {
            val strokeWidth = 3.dp.toPx()
            val cornerLength = 30.dp.toPx()
            
            // Draw document frame corners
            drawDocumentCorners(cornerLength, strokeWidth)
        }
        
        Text(
            text = "Position document within frame",
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp)
                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun InjuryDetectionOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_mic),
                contentDescription = "Voice activation",
                tint = Color.Red,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Say 'process img' to analyze",
                color = Color.White,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
fun PoseDetectionOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Stand in full view for pose analysis",
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp)
                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

private fun DrawScope.drawDocumentCorners(cornerLength: Float, strokeWidth: Float) {
    val color = Color.White
    
    // Top-left corner
    drawLine(color, Offset(0f, 0f), Offset(cornerLength, 0f), strokeWidth)
    drawLine(color, Offset(0f, 0f), Offset(0f, cornerLength), strokeWidth)
    
    // Top-right corner
    drawLine(color, Offset(size.width - cornerLength, 0f), Offset(size.width, 0f), strokeWidth)
    drawLine(color, Offset(size.width, 0f), Offset(size.width, cornerLength), strokeWidth)
    
    // Bottom-left corner
    drawLine(color, Offset(0f, size.height - cornerLength), Offset(0f, size.height), strokeWidth)
    drawLine(color, Offset(0f, size.height), Offset(cornerLength, size.height), strokeWidth)
    
    // Bottom-right corner
    drawLine(color, Offset(size.width, size.height - cornerLength), Offset(size.width, size.height), strokeWidth)
    drawLine(color, Offset(size.width - cornerLength, size.height), Offset(size.width, size.height), strokeWidth)
}

@Composable
fun CameraFlipButton(
    isFrontCamera: Boolean,
    onCameraSwitch: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onCameraSwitch,
        modifier = modifier
            .size(64.dp)
            .background(
                Color.Black.copy(alpha = 0.6f), 
                CircleShape
            )
            .border(
                2.dp, 
                Color.White.copy(alpha = 0.3f), 
                CircleShape
            )
    ) {
        Icon(
            painter = painterResource(
                id = if (isFrontCamera) R.drawable.ic_camera_front else R.drawable.ic_camera_rear
            ),
            contentDescription = if (isFrontCamera) "Switch to Back Camera" else "Switch to Front Camera",
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
    }
}
