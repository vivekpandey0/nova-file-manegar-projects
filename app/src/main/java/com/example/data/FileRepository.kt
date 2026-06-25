package com.example.data

import android.content.Context
import android.os.Environment
import android.os.storage.StorageManager
import com.example.model.FileModel
import java.io.File

class FileRepository(private val context: Context) {

    private val rootDir: File = Environment.getExternalStorageDirectory()

    init {
    }

    fun getStorageVolumes(): List<Pair<String, File>> {
        val volumes = mutableListOf<Pair<String, File>>()
        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            storageManager.storageVolumes.forEach { volume ->
                val dir = volume.directory
                if (dir != null) {
                    val name = if (volume.isPrimary) "Internal Storage" else volume.getDescription(context)
                    volumes.add(Pair(name, dir))
                }
            }
        } else {
            volumes.add(Pair("Internal Storage", rootDir))
            // On older APIs, we might need a workaround for SD card, but for now just internal
        }
        return volumes
    }

    fun getFiles(directory: File = rootDir): List<FileModel> {
        return directory.listFiles()?.map { FileModel(it) }?.sortedWith(
            compareBy({ !it.isDirectory }, { it.name.lowercase() })
        ) ?: emptyList()
    }
    
    fun getRoot() = rootDir

    fun deleteFile(file: File): Boolean {
        return if (file.isDirectory) {
            file.deleteRecursively()
        } else {
            file.delete()
        }
    }
}
