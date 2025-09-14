package com.example.mlkit_posedetection_jetpack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import com.example.mlkit_posedetection_jetpack.R
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class CapturedMedia(
    val id: String,
    val uri: String,
    val type: MediaType,
    val timestamp: Long,
    val mode: String // PHOTO, VIDEO, AI DOC, INJURY, POSE
)

enum class MediaType {
    PHOTO, VIDEO
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    onBackPressed: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Photos", "Videos", "AI Docs", "Injuries", "Poses")
    
    // Sample data - in real app, this would come from ViewModel
    val mediaItems = remember {
        listOf(
            CapturedMedia("1", "uri1", MediaType.PHOTO, System.currentTimeMillis(), "PHOTO"),
            CapturedMedia("2", "uri2", MediaType.VIDEO, System.currentTimeMillis(), "VIDEO"),
            CapturedMedia("3", "uri3", MediaType.PHOTO, System.currentTimeMillis(), "AI DOC"),
            CapturedMedia("4", "uri4", MediaType.PHOTO, System.currentTimeMillis(), "INJURY"),
            CapturedMedia("5", "uri5", MediaType.PHOTO, System.currentTimeMillis(), "POSE")
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Top app bar
        TopAppBar(
            title = { 
                Text(
                    "Gallery", 
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                ) 
            },
            navigationIcon = {
                IconButton(onClick = onBackPressed) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_revert),
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            },
            actions = {
                IconButton(onClick = { /* Search */ }) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_search),
                        contentDescription = "Search",
                        tint = Color.White
                    )
                }
                IconButton(onClick = { /* More options */ }) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_more),
                        contentDescription = "More",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.Black
            )
        )
        
        // Filter chips
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 80.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filters) { filter ->
                FilterChip(
                    onClick = { selectedFilter = filter },
                    label = { Text(filter, fontSize = 12.sp) },
                    selected = selectedFilter == filter,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color.White,
                        selectedLabelColor = Color.Black,
                        containerColor = Color.Gray.copy(alpha = 0.3f),
                        labelColor = Color.White
                    )
                )
            }
        }
        
        // Media grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(mediaItems.filter { item ->
                when (selectedFilter) {
                    "All" -> true
                    "Photos" -> item.type == MediaType.PHOTO && item.mode == "PHOTO"
                    "Videos" -> item.type == MediaType.VIDEO
                    "AI Docs" -> item.mode == "AI DOC"
                    "Injuries" -> item.mode == "INJURY"
                    "Poses" -> item.mode == "POSE"
                    else -> true
                }
            }) { item ->
                MediaGridItem(
                    item = item,
                    onClick = { /* Open media viewer */ }
                )
            }
        }
    }
}

@Composable
fun MediaGridItem(
    item: CapturedMedia,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Gray.copy(alpha = 0.3f))
            .clickable { onClick() }
    ) {
        // Placeholder for actual image/video thumbnail
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = 
                    if (item.type == MediaType.VIDEO) android.R.drawable.ic_media_play
                    else when (item.mode) {
                        "AI DOC" -> R.drawable.ic_description
                        "INJURY" -> R.drawable.ic_local_hospital
                        "POSE" -> R.drawable.ic_accessibility
                        else -> R.drawable.ic_photo
                    }
                ),
                contentDescription = item.mode,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(32.dp)
            )
        }
        
        // Mode indicator
        Card(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                text = item.mode,
                color = Color.White,
                fontSize = 8.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
        
        // Video duration indicator
        if (item.type == MediaType.VIDEO) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "0:30", // Sample duration
                    color = Color.White,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun MediaViewerScreen(
    media: CapturedMedia,
    onBackPressed: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Media content (placeholder)
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(id = 
                        when (media.mode) {
                            "AI DOC" -> R.drawable.ic_description
                            "INJURY" -> R.drawable.ic_local_hospital
                            "POSE" -> R.drawable.ic_accessibility
                            "VIDEO" -> android.R.drawable.ic_media_play
                            else -> R.drawable.ic_photo
                        }
                    ),
                    contentDescription = media.mode,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(120.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "${media.mode} Preview",
                    color = Color.White,
                    fontSize = 18.sp
                )
            }
        }
        
        // Top bar
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(16.dp)
                .statusBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBackPressed,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_revert),
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            
            Row {
                IconButton(
                    onClick = { /* Share */ },
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_share),
                        contentDescription = "Share",
                        tint = Color.White
                    )
                }
                
                IconButton(
                    onClick = { /* Delete */ },
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_delete),
                        contentDescription = "Delete",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
