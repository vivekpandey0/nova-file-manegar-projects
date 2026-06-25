package com.example.data

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipUtils {
    fun compressFile(sourceFile: File, zipFile: File): Boolean {
        return try {
            FileOutputStream(zipFile).use { fos ->
                ZipOutputStream(BufferedOutputStream(fos)).use { zos ->
                    val filesToZip = if (sourceFile.isDirectory) {
                        sourceFile.listFiles()?.toList() ?: emptyList()
                    } else {
                        listOf(sourceFile)
                    }

                    for (file in filesToZip) {
                        zipFile(file, file.name, zos)
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun zipFile(fileToZip: File, fileName: String, zos: ZipOutputStream) {
        if (fileToZip.isHidden) return
        if (fileToZip.isDirectory) {
            val children = fileToZip.listFiles()
            if (children != null) {
                for (childFile in children) {
                    zipFile(childFile, fileName + "/" + childFile.name, zos)
                }
            }
            return
        }
        FileInputStream(fileToZip).use { fis ->
            val zipEntry = ZipEntry(fileName)
            zos.putNextEntry(zipEntry)
            val bytes = ByteArray(1024)
            var length: Int
            while (fis.read(bytes).also { length = it } >= 0) {
                zos.write(bytes, 0, length)
            }
        }
    }

    fun extractZip(zipFile: File, destinationDir: File): Boolean {
        return try {
            if (!destinationDir.exists()) destinationDir.mkdirs()
            FileInputStream(zipFile).use { fis ->
                ZipInputStream(BufferedInputStream(fis)).use { zis ->
                    var entry: ZipEntry?
                    while (zis.nextEntry.also { entry = it } != null) {
                        val file = File(destinationDir, entry!!.name)
                        val dir = if (entry!!.isDirectory) file else file.parentFile
                        if (!dir.exists()) dir.mkdirs()
                        if (!entry!!.isDirectory) {
                            FileOutputStream(file).use { fos ->
                                val buffer = ByteArray(1024)
                                var count: Int
                                while (zis.read(buffer).also { count = it } != -1) {
                                    fos.write(buffer, 0, count)
                                }
                            }
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
