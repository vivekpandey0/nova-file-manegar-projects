package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CategoryGrid
import com.example.ui.components.FileListItem
import com.example.ui.components.StorageCard
import com.example.ui.theme.*
import com.example.ui.components.rememberInterstitialAd
import java.io.File
import com.example.ui.FileViewerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToBrowser: () -> Unit
) {
    val showAd = rememberInterstitialAd()
    val recentFiles by viewModel.recentFiles.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val viewingFile by viewModel.viewingFile.collectAsState()
    val storageStats by viewModel.storageStats.collectAsState()
    val categoryCounts by viewModel.categoryCounts.collectAsState()
    val aiResult by viewModel.aiResult.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()

    val storageVolumes by viewModel.storageVolumes.collectAsState()

    if (viewingFile != null) {
        FileViewerDialog(file = viewingFile!!, onDismiss = { viewModel.closeFile() })
    }
    
    if (isAiLoading) {
        androidx.compose.ui.window.Dialog(onDismissRequest = {}) {
            Box(modifier = Modifier.size(100.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        }
    }
    
    if (aiResult != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearAiResult() },
            title = { Text("AI Analysis", fontWeight = FontWeight.Bold) },
            text = { Text(aiResult!!) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearAiResult() }) {
                    Text("Close")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Nova ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Manager",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = Primary
                        )
                    }
                },
                actions = {},
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            com.example.ui.components.AdBanner()
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                StorageCard(
                    totalSpace = storageStats.first,
                    usedSpace = storageStats.second,
                    freeSpace = storageStats.third,
                    storageVolumes = storageVolumes,
                    onVolumeClick = { volumeDir ->
                        viewModel.loadDirectory(volumeDir)
                        onNavigateToBrowser()
                    }
                )
            }

            item {
                CategoryGrid(
                    categoryCounts = categoryCounts,
                    onCategoryClick = { categoryName -> 
                        val dirName = when(categoryName) {
                            "Documents" -> "Documents"
                            "Images" -> "DCIM"
                            "Videos" -> "Movies"
                            "Audio" -> "Music"
                            "Downloads", "APK", "Archives" -> "Download"
                            else -> ""
                        }
                        if (dirName.isNotEmpty()) {
                            viewModel.loadDirectory(File(viewModel.getRoot(), dirName))
                            onNavigateToBrowser()
                        } else {
                            viewModel.loadDirectory(viewModel.getRoot())
                            onNavigateToBrowser()
                        }
                    }
                )
            }

            if (favorites.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Favorites",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                
                items(favorites) { file ->
                    FileListItem(
                        file = file,
                        onClick = { viewModel.openFile(file.file) },
                        onDelete = { 
                            viewModel.deleteFile(file.file)
                            showAd()
                        },
                        onCompress = { viewModel.compressFile(file.file) },
                        onExtract = { viewModel.extractFile(file.file) },
                        isFavorite = true,
                        onToggleFavorite = { viewModel.toggleFavorite(file.file) },
                        onAiSummarize = { viewModel.analyzeFileAi(file.file) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Recent Files",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            items(recentFiles) { file ->
                val isFav = favorites.any { it.file.absolutePath == file.file.absolutePath }
                FileListItem(
                    file = file,
                    onClick = { viewModel.openFile(file.file) },
                    onDelete = { 
                        viewModel.deleteFile(file.file)
                        showAd()
                    },
                    onCompress = { viewModel.compressFile(file.file) },
                    onExtract = { viewModel.extractFile(file.file) },
                    isFavorite = isFav,
                    onToggleFavorite = { viewModel.toggleFavorite(file.file) },
                    onAiSummarize = { viewModel.analyzeFileAi(file.file) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            item {
                Button(
                    onClick = { viewModel.analyzeStorageAi() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(Primary, Secondary))),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues()
                ) {
                    Text("Analyze Storage (AI)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}


