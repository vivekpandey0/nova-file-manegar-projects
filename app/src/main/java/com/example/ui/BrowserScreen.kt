package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.NoteAdd
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.ui.components.FileListItem
import com.example.ui.theme.*
import com.example.ui.components.rememberInterstitialAd
import java.io.File

@Composable
fun BrowserScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val showAd = rememberInterstitialAd()
    val currentDirectory by viewModel.currentDirectory.collectAsState()
    val files by viewModel.files.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()
    val viewingFile by viewModel.viewingFile.collectAsState()
    val aiResult by viewModel.aiResult.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()

    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    var fileToRename by remember { mutableStateOf<File?>(null) }
    var renameQuery by remember { mutableStateOf("") }
    
    var showSortFilterDialog by remember { mutableStateOf(false) }

    val filteredFiles = remember(searchQuery, files) {
        if (searchQuery.isBlank()) files else files.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    if (viewingFile != null) {
        FileViewerDialog(file = viewingFile!!, onDismiss = { viewModel.closeFile() })
    }
    
    if (fileToRename != null) {
        AlertDialog(
            onDismissRequest = { fileToRename = null },
            title = { Text("Rename File") },
            text = {
                OutlinedTextField(
                    value = renameQuery,
                    onValueChange = { renameQuery = it },
                    singleLine = true,
                    label = { Text("New name") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (renameQuery.isNotBlank()) {
                        viewModel.renameFile(fileToRename!!, renameQuery)
                    }
                    fileToRename = null
                }) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { fileToRename = null }) { Text("Cancel") }
            }
        )
    }

    if (isAiLoading) {
        Dialog(onDismissRequest = {}) {
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

    if (showSortFilterDialog) {
        val currentSort by viewModel.currentSortOption.collectAsState()
        val currentFilterType by viewModel.currentFilterType.collectAsState()
        val currentFilterDate by viewModel.currentFilterDate.collectAsState()

        AlertDialog(
            onDismissRequest = { showSortFilterDialog = false },
            title = { Text("Sort and Filter", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn {
                    item {
                        Text("Sort By", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                    }
                    val sortOptions = listOf(
                        "Name (A-Z)" to SortOption.NAME_ASC,
                        "Name (Z-A)" to SortOption.NAME_DESC,
                        "Newest First" to SortOption.DATE_NEWEST,
                        "Oldest First" to SortOption.DATE_OLDEST,
                        "Largest First" to SortOption.SIZE_LARGEST,
                        "Smallest First" to SortOption.SIZE_SMALLEST,
                        "File Type" to SortOption.TYPE
                    )
                    items(sortOptions) { (label, option) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setSortOption(option) }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = currentSort == option,
                                onClick = { viewModel.setSortOption(option) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label)
                        }
                    }
                    
                    item {
                        Text("Filter by Type", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
                    }
                    val typeOptions = listOf(
                        "All Files" to FilterType.ALL,
                        "Images" to FilterType.IMAGES,
                        "Documents" to FilterType.DOCUMENTS,
                        "Videos" to FilterType.VIDEOS,
                        "Audio" to FilterType.AUDIO,
                        "Archives" to FilterType.ARCHIVES
                    )
                    items(typeOptions) { (label, option) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setFilterType(option) }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = currentFilterType == option,
                                onClick = { viewModel.setFilterType(option) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label)
                        }
                    }

                    item {
                        Text("Filter by Date", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
                    }
                    val dateOptions = listOf(
                        "All Time" to FilterDate.ALL,
                        "Today" to FilterDate.TODAY,
                        "Last 7 Days" to FilterDate.LAST_7_DAYS,
                        "Last 30 Days" to FilterDate.LAST_30_DAYS
                    )
                    items(dateOptions) { (label, option) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setFilterDate(option) }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = currentFilterDate == option,
                                onClick = { viewModel.setFilterDate(option) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSortFilterDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            var showCreateMenu by remember { mutableStateOf(false) }
            Column(horizontalAlignment = Alignment.End) {
                if (showCreateMenu) {
                    Column(
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .shadow(4.dp, RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(8.dp)
                    ) {
                        TextButton(onClick = { 
                            showCreateMenu = false
                            viewModel.createFolder("New_Folder_${System.currentTimeMillis()}")
                        }) {
                            Icon(Icons.Rounded.CreateNewFolder, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create Folder")
                        }
                        TextButton(onClick = { 
                            showCreateMenu = false
                            viewModel.createTextFile("New_File_${System.currentTimeMillis()}.txt", "Hello World!")
                        }) {
                            Icon(Icons.Rounded.NoteAdd, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create Text File")
                        }
                    }
                }
                FloatingActionButton(
                    onClick = { showCreateMenu = !showCreateMenu },
                    containerColor = Primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add")
                }
            }
        },
        bottomBar = {
            com.example.ui.components.AdBanner()
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
        // Purple Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(Brush.linearGradient(listOf(Primary, Secondary)))
                .padding(top = 48.dp, bottom = 24.dp, start = 24.dp, end = 24.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (isSearching) {
                            isSearching = false
                            searchQuery = ""
                        } else if (!viewModel.navigateUp()) {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    if (isSearching) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search...", color = Color.White.copy(alpha=0.5f)) },
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                cursorColor = Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )
                    } else {
                        Text(
                            text = "Nova Browser",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = {
                        isSearching = !isSearching
                        if (!isSearching) searchQuery = ""
                    }) {
                        Icon(Icons.Rounded.Search, contentDescription = "Search", tint = Color.White)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentDirectory.name.ifEmpty { "Internal Storage" },
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row {
                        IconButton(onClick = { showSortFilterDialog = true }) {
                            Icon(Icons.Rounded.FilterList, contentDescription = "Sort and Filter", tint = Color.White)
                        }
                        IconButton(onClick = { if (!isGridView) viewModel.toggleGridView() }) {
                            Icon(Icons.Rounded.GridView, contentDescription = "Grid", tint = if (isGridView) Color.White else Color.White.copy(alpha=0.5f))
                        }
                        IconButton(onClick = { if (isGridView) viewModel.toggleGridView() }) {
                            Icon(Icons.AutoMirrored.Rounded.List, contentDescription = "List", tint = if (!isGridView) Color.White else Color.White.copy(alpha=0.5f))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Storage", color = Color.White.copy(alpha=0.7f), fontSize = 12.sp)
                    Text("  >  ", color = Color.White.copy(alpha=0.7f), fontSize = 12.sp)
                    Text(currentDirectory.name.ifEmpty { "Emulated" }, color = Color.White, fontSize = 12.sp)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        if (filteredFiles.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isNotEmpty()) "No results found" else "Folder is empty",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredFiles) { file ->
                        val isFav = favorites.any { it.file.absolutePath == file.file.absolutePath }
                        FileListItem(
                            file = file,
                            onClick = {
                                if (file.isDirectory) {
                                    viewModel.loadDirectory(file.file)
                                } else {
                                    viewModel.openFile(file.file)
                                }
                            },
                            onDelete = { 
                                viewModel.deleteFile(file.file)
                                showAd()
                            },
                            onCompress = { viewModel.compressFile(file.file) },
                            onExtract = { viewModel.extractFile(file.file) },
                            onRename = { fileToRename = file.file; renameQuery = file.name },
                            onCopy = { viewModel.copyFile(file.file) },
                            showCheckbox = false,
                            isSelected = false,
                            isFavorite = isFav,
                            onToggleFavorite = { viewModel.toggleFavorite(file.file) },
                            onAiSummarize = { viewModel.analyzeFileAi(file.file) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredFiles) { file ->
                        val isFav = favorites.any { it.file.absolutePath == file.file.absolutePath }
                        FileListItem(
                            file = file,
                            onClick = {
                                if (file.isDirectory) {
                                    viewModel.loadDirectory(file.file)
                                } else {
                                    viewModel.openFile(file.file)
                                }
                            },
                            onDelete = { 
                                viewModel.deleteFile(file.file)
                                showAd()
                            },
                            onCompress = { viewModel.compressFile(file.file) },
                            onExtract = { viewModel.extractFile(file.file) },
                            onRename = { fileToRename = file.file; renameQuery = file.name },
                            onCopy = { viewModel.copyFile(file.file) },
                            showCheckbox = false,
                            isSelected = false,
                            isFavorite = isFav,
                            onToggleFavorite = { viewModel.toggleFavorite(file.file) },
                            onAiSummarize = { viewModel.analyzeFileAi(file.file) }
                        )
                    }
                }
            }
        }
        }
    }
}

@Composable
fun FileViewerDialog(file: File, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Toolbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Close")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(file.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }

                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (file.extension.lowercase()) {
                        "jpg", "jpeg", "png", "webp" -> {
                            AsyncImage(
                                model = file,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                        "txt", "csv", "xml", "json", "md" -> {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                item {
                                    Text(
                                        text = runCatching { file.readText() }.getOrDefault("Could not read file."),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        else -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Rounded.InsertDriveFile, contentDescription = null, modifier = Modifier.size(100.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Preview not available", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}


