package com.example.model

import java.io.File

data class FileModel(
    val file: File,
    val name: String = file.name,
    val isDirectory: Boolean = file.isDirectory,
    val size: Long = file.length(),
    val lastModified: Long = file.lastModified(),
    val extension: String = file.extension
)
