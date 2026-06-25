package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FileRepository
import com.example.data.ZipUtils
import com.example.data.db.AppDatabase
import com.example.data.db.RecentFileEntity
import com.example.model.FileModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.io.File

import com.example.data.repository.GeminiRepository
import com.example.data.api.RetrofitClient

enum class SortOption {
    NAME_ASC, NAME_DESC, DATE_NEWEST, DATE_OLDEST, SIZE_LARGEST, SIZE_SMALLEST, TYPE
}

enum class FilterType {
    ALL, IMAGES, DOCUMENTS, VIDEOS, AUDIO, ARCHIVES
}

enum class FilterDate {
    ALL, TODAY, LAST_7_DAYS, LAST_30_DAYS
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FileRepository(application)
    private val geminiRepository = GeminiRepository(RetrofitClient.service)
    private val appDb = AppDatabase.getDatabase(application)
    private val recentFileDao = appDb.recentFileDao()
    private val favoriteDao = appDb.favoriteDao()

    private val _currentSortOption = MutableStateFlow(SortOption.NAME_ASC)
    val currentSortOption: StateFlow<SortOption> = _currentSortOption.asStateFlow()

    private val _currentFilterType = MutableStateFlow(FilterType.ALL)
    val currentFilterType: StateFlow<FilterType> = _currentFilterType.asStateFlow()

    private val _currentFilterDate = MutableStateFlow(FilterDate.ALL)
    val currentFilterDate: StateFlow<FilterDate> = _currentFilterDate.asStateFlow()

    private val _currentDirectory = MutableStateFlow(repository.getRoot())
    val currentDirectory: StateFlow<File> = _currentDirectory.asStateFlow()

    private val _files = MutableStateFlow<List<FileModel>>(emptyList())
    val files: StateFlow<List<FileModel>> = _files.asStateFlow()

    private val _recentFiles = MutableStateFlow<List<FileModel>>(emptyList())
    val recentFiles: StateFlow<List<FileModel>> = _recentFiles.asStateFlow()

    private val _favorites = MutableStateFlow<List<FileModel>>(emptyList())
    val favorites: StateFlow<List<FileModel>> = _favorites.asStateFlow()

    private val _isGridView = MutableStateFlow(false)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()
    
    private val _viewingFile = MutableStateFlow<File?>(null)
    val viewingFile: StateFlow<File?> = _viewingFile.asStateFlow()

    private val _aiResult = MutableStateFlow<String?>(null)
    val aiResult: StateFlow<String?> = _aiResult.asStateFlow()
    
    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _storageStats = MutableStateFlow(Triple(0L, 0L, 0L)) // Total, Used, Free
    val storageStats: StateFlow<Triple<Long, Long, Long>> = _storageStats.asStateFlow()

    private val _storageVolumes = MutableStateFlow<List<Pair<String, File>>>(emptyList())
    val storageVolumes: StateFlow<List<Pair<String, File>>> = _storageVolumes.asStateFlow()

    private val _categoryCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val categoryCounts: StateFlow<Map<String, Int>> = _categoryCounts.asStateFlow()

    init {
        _storageVolumes.value = repository.getStorageVolumes()
        loadDirectory(repository.getRoot())
        loadRecentFiles()
        loadFavorites()
        updateStorageStats()
        updateCategoryCounts()
    }
    
    fun updateCategoryCounts() {
        viewModelScope.launch(Dispatchers.IO) {
            val root = repository.getRoot()
            val counts = mutableMapOf(
                "Documents" to 0,
                "Images" to 0,
                "Videos" to 0,
                "Audio" to 0,
                "APK" to 0,
                "Archives" to 0,
                "Downloads" to 0
            )
            
            // For a real app, scanning the whole SD card might be slow, so we can check standard directories
            val standardDirs = listOf(
                File(root, "Documents"),
                File(root, "DCIM"),
                File(root, "Pictures"),
                File(root, "Movies"),
                File(root, "Music"),
                File(root, "Download")
            )
            
            fun scan(file: File) {
                if (file.isDirectory) {
                    file.listFiles()?.forEach { scan(it) }
                } else {
                    val ext = file.extension.lowercase()
                    when (ext) {
                        "pdf", "doc", "docx", "txt", "xls", "xlsx" -> counts["Documents"] = counts["Documents"]!! + 1
                        "jpg", "jpeg", "png", "webp", "gif" -> counts["Images"] = counts["Images"]!! + 1
                        "mp4", "mkv", "avi" -> counts["Videos"] = counts["Videos"]!! + 1
                        "mp3", "wav", "m4a", "ogg" -> counts["Audio"] = counts["Audio"]!! + 1
                        "apk" -> counts["APK"] = counts["APK"]!! + 1
                        "zip", "rar", "7z" -> counts["Archives"] = counts["Archives"]!! + 1
                    }
                }
            }
            
            standardDirs.forEach { dir ->
                if (dir.exists()) {
                    scan(dir)
                }
            }
            
            val downloadsDir = File(root, "Download")
            if (downloadsDir.exists()) {
                counts["Downloads"] = downloadsDir.listFiles()?.size ?: 0
            }
            
            _categoryCounts.value = counts
        }
    }
    
    fun updateStorageStats() {
        val path = repository.getRoot()
        val stat = android.os.StatFs(path.path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong
        
        val total = totalBlocks * blockSize
        val free = availableBlocks * blockSize
        val used = total - free
        
        _storageStats.value = Triple(total, used, free)
    }

    fun getRoot(): File = repository.getRoot()

    fun toggleGridView() {
        _isGridView.value = !_isGridView.value
    }

    private var currentRawFiles = emptyList<FileModel>()

    fun setSortOption(option: SortOption) {
        _currentSortOption.value = option
        applySortAndFilter()
    }

    fun setFilterType(type: FilterType) {
        _currentFilterType.value = type
        applySortAndFilter()
    }

    fun setFilterDate(date: FilterDate) {
        _currentFilterDate.value = date
        applySortAndFilter()
    }

    private fun applySortAndFilter() {
        var result = currentRawFiles

        // Apply Filter Type
        if (_currentFilterType.value != FilterType.ALL) {
            result = result.filter { fileModel ->
                if (fileModel.isDirectory) return@filter true
                val ext = fileModel.file.extension.lowercase()
                when (_currentFilterType.value) {
                    FilterType.IMAGES -> ext in listOf("jpg", "jpeg", "png", "webp", "gif")
                    FilterType.DOCUMENTS -> ext in listOf("pdf", "doc", "docx", "txt", "xls", "xlsx")
                    FilterType.VIDEOS -> ext in listOf("mp4", "mkv", "avi")
                    FilterType.AUDIO -> ext in listOf("mp3", "wav", "m4a", "ogg")
                    FilterType.ARCHIVES -> ext in listOf("zip", "rar", "7z")
                    else -> true
                }
            }
        }

        // Apply Filter Date
        if (_currentFilterDate.value != FilterDate.ALL) {
            val now = System.currentTimeMillis()
            val dayMs = 24 * 60 * 60 * 1000L
            result = result.filter { fileModel ->
                if (fileModel.isDirectory) return@filter true
                val ageMs = now - fileModel.file.lastModified()
                when (_currentFilterDate.value) {
                    FilterDate.TODAY -> ageMs <= dayMs
                    FilterDate.LAST_7_DAYS -> ageMs <= 7 * dayMs
                    FilterDate.LAST_30_DAYS -> ageMs <= 30 * dayMs
                    else -> true
                }
            }
        }

        // Apply Sorting
        result = when (_currentSortOption.value) {
            SortOption.NAME_ASC -> result.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            SortOption.NAME_DESC -> result.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })).reversed()
            SortOption.DATE_NEWEST -> result.sortedWith(compareBy({ !it.isDirectory }, { -it.file.lastModified() }))
            SortOption.DATE_OLDEST -> result.sortedWith(compareBy({ !it.isDirectory }, { it.file.lastModified() }))
            SortOption.SIZE_LARGEST -> result.sortedWith(compareBy({ !it.isDirectory }, { -it.file.length() }))
            SortOption.SIZE_SMALLEST -> result.sortedWith(compareBy({ !it.isDirectory }, { it.file.length() }))
            SortOption.TYPE -> result.sortedWith(compareBy({ !it.isDirectory }, { it.file.extension.lowercase() }, { it.name.lowercase() }))
        }

        // Ensure folders always stay at the top if sorting accidentally reversed it (like NAME_DESC)
        // Wait, reversed() will put folders at bottom. Let's fix that.
        val (folders, files) = result.partition { it.isDirectory }
        
        _files.value = folders + files
    }

    fun loadDirectory(directory: File) {
        viewModelScope.launch {
            _currentDirectory.value = directory
            currentRawFiles = repository.getFiles(directory)
            applySortAndFilter()
        }
    }

    fun loadRecentFiles() {
        viewModelScope.launch {
            val entities = recentFileDao.getRecentFiles()
            val recentList = entities.mapNotNull {
                val f = File(it.absolutePath)
                if (f.exists()) FileModel(f) else null
            }
            _recentFiles.value = recentList
        }
    }

    fun loadFavorites() {
        viewModelScope.launch {
            val entities = favoriteDao.getAllFavorites()
            val favoriteList = entities.mapNotNull {
                val f = File(it.absolutePath)
                if (f.exists()) FileModel(f) else null
            }
            _favorites.value = favoriteList
        }
    }
    
    fun toggleFavorite(file: File) {
        viewModelScope.launch {
            if (favoriteDao.isFavorite(file.absolutePath)) {
                favoriteDao.deleteFavoriteByPath(file.absolutePath)
            } else {
                favoriteDao.insertFavorite(com.example.data.db.FavoriteEntity(file.absolutePath, System.currentTimeMillis()))
            }
            loadFavorites()
        }
    }
    
    fun openFile(file: File) {
        viewModelScope.launch {
            recentFileDao.insertRecentFile(RecentFileEntity(file.absolutePath, System.currentTimeMillis()))
            val ext = file.extension.lowercase()
            if (ext in listOf("jpg", "jpeg", "png", "webp", "txt", "csv", "xml", "json", "md")) {
                _viewingFile.value = file
            } else {
                openWithIntent(file)
            }
            loadRecentFiles()
        }
    }
    
    private fun openWithIntent(file: File) {
        val context = getApplication<Application>()
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(uri, getMimeType(file.absolutePath))
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun getMimeType(url: String): String {
        var type = "*/*"
        val extension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(url)
        if (extension != null) {
            type = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
        }
        return type
    }
    
    fun closeFile() {
        _viewingFile.value = null
    }
    
    fun clearAiResult() {
        _aiResult.value = null
    }

    fun analyzeFileAi(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            _isAiLoading.value = true
            val result = geminiRepository.analyzeFile(file)
            _aiResult.value = result.getOrElse { "Error analyzing file: ${it.message}" }
            _isAiLoading.value = false
        }
    }

    fun analyzeStorageAi() {
        viewModelScope.launch(Dispatchers.IO) {
            _isAiLoading.value = true
            val stats = _storageStats.value
            val totalGb = stats.first / (1024L * 1024L * 1024L)
            val usedGb = stats.second / (1024L * 1024L * 1024L)
            val freeGb = stats.third / (1024L * 1024L * 1024L)
            val categories = _categoryCounts.value
            
            val result = geminiRepository.analyzeStorage(totalGb, usedGb, freeGb, categories)
            _aiResult.value = result.getOrElse { "Error analyzing storage: ${it.message}" }
            _isAiLoading.value = false
        }
    }

    fun navigateUp(): Boolean {
        val parent = _currentDirectory.value.parentFile
        if (parent != null && parent.absolutePath.startsWith(repository.getRoot().absolutePath)) {
            loadDirectory(parent)
            return true
        }
        return false
    }

    fun createFolder(name: String) {
        val dir = File(_currentDirectory.value, name)
        if (!dir.exists()) {
            dir.mkdirs()
            loadDirectory(_currentDirectory.value)
        }
    }

    fun createTextFile(name: String, content: String) {
        val file = File(_currentDirectory.value, name)
        if (!file.exists()) {
            file.writeText(content)
            loadDirectory(_currentDirectory.value)
        }
    }

    fun deleteFile(file: File) {
        if (repository.deleteFile(file)) {
            viewModelScope.launch {
                recentFileDao.deleteRecentFile(file.absolutePath)
                loadDirectory(_currentDirectory.value)
                loadRecentFiles()
            }
        }
    }

    fun renameFile(file: File, newName: String) {
        val dest = File(file.parentFile, newName)
        if (file.renameTo(dest)) {
            loadDirectory(_currentDirectory.value)
        }
    }
    
    fun copyFile(file: File) {
        val dest = File(file.parentFile, "Copy_of_${file.name}")
        if (file.isDirectory) {
            file.copyRecursively(dest, overwrite = true)
        } else {
            file.copyTo(dest, overwrite = true)
        }
        loadDirectory(_currentDirectory.value)
    }

    fun moveFile(file: File, destDir: File) {
        val dest = File(destDir, file.name)
        if (file.renameTo(dest)) {
            loadDirectory(_currentDirectory.value)
        }
    }

    fun compressFile(file: File) {
        viewModelScope.launch {
            val dest = File(file.parentFile, "${file.nameWithoutExtension}.zip")
            if (ZipUtils.compressFile(file, dest)) {
                loadDirectory(_currentDirectory.value)
            }
        }
    }

    fun extractFile(file: File) {
        viewModelScope.launch {
            val destDir = File(file.parentFile, file.nameWithoutExtension)
            if (ZipUtils.extractZip(file, destDir)) {
                loadDirectory(_currentDirectory.value)
            }
        }
    }
}
