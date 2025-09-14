package com.example.mlkit_posedetection_jetpack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mlkit_posedetection_jetpack.R
import com.example.mlkit_posedetection_jetpack.camera_usecase.CameraScreen

data class CameraMode(
    val name: String,
    val iconRes: Int,
    val description: String
)

@Composable
fun CameraModesScreen() {
    var selectedMode by remember { mutableStateOf(0) }
    
    val cameraModes = listOf(
        CameraMode("PHOTO", R.drawable.ic_photo_camera, "Capture photos"),
        CameraMode("VIDEO", R.drawable.ic_videocam, "Record videos"),
        CameraMode("AI DOC", R.drawable.ic_description, "AI Document scanner"),
        CameraMode("INJURY", R.drawable.ic_local_hospital, "Injury detection"),
        CameraMode("POSE", R.drawable.ic_accessibility, "Pose detection")
    )
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview background
        CameraScreen()
        
        // Top bar with settings and flash
        TopAppBar(
            modifier = Modifier.align(Alignment.TopCenter)
        )
        
        // Camera modes selection
        CameraModeSelector(
            modes = cameraModes,
            selectedMode = selectedMode,
            onModeSelected = { selectedMode = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp)
        )
        
        // Bottom controls
        BottomCameraControls(
            selectedMode = cameraModes[selectedMode],
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { /* Settings */ },
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                .size(48.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_settings),
                contentDescription = "Settings",
                tint = Color.White
            )
        }
        
        Row {
            IconButton(
                onClick = { /* Flash toggle */ },
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                    .size(48.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_flash_off),
                    contentDescription = "Flash",
                    tint = Color.White
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            IconButton(
                onClick = { /* More options */ },
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                    .size(48.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_more_vert),
                    contentDescription = "More",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun CameraModeSelector(
    modes: List<CameraMode>,
    selectedMode: Int,
    onModeSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(horizontal = 32.dp)
    ) {
        itemsIndexed(modes) { index, mode ->
            CameraModeItem(
                mode = mode,
                isSelected = index == selectedMode,
                onClick = { onModeSelected(index) }
            )
        }
    }
}

@Composable
fun CameraModeItem(
    mode: CameraMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = mode.name,
            color = if (isSelected) Color.Yellow else Color.White,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
        
        if (isSelected) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(2.dp)
                    .background(Color.Yellow)
                    .padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun BottomCameraControls(
    selectedMode: CameraMode,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
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
                painter = painterResource(R.drawable.ic_photo_library),
                contentDescription = "Gallery",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        
        // Capture button
        CaptureButton(
            mode = selectedMode,
            onClick = { /* Handle capture based on mode */ }
        )
        
        // Camera switch button
        IconButton(
            onClick = { /* Switch camera */ },
            modifier = Modifier
                .size(48.dp)
                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_camera_front),
                contentDescription = "Switch Camera",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun CaptureButton(
    mode: CameraMode,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .background(Color.White, CircleShape)
            .border(4.dp, Color.Gray, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        when (mode.name) {
            "VIDEO" -> {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color.Red, CircleShape)
                )
            }
            "AI DOC" -> {
                Icon(
                    painter = painterResource(R.drawable.ic_description),
                    contentDescription = "Scan Document",
                    tint = Color.Black,
                    modifier = Modifier.size(32.dp)
                )
            }
            "INJURY" -> {
                Icon(
                    painter = painterResource(R.drawable.ic_local_hospital),
                    contentDescription = "Detect Injury",
                    tint = Color.Red,
                    modifier = Modifier.size(32.dp)
                )
            }
            "POSE" -> {
                Icon(
                    painter = painterResource(R.drawable.ic_accessibility),
                    contentDescription = "Pose Detection",
                    tint = Color.Blue,
                    modifier = Modifier.size(32.dp)
                )
            }
            else -> {
                // Default photo capture
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color.White, CircleShape)
                        .border(2.dp, Color.Gray, CircleShape)
                )
            }
        }
    }
}
