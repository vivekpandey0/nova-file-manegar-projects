package com.example.data.repository

import com.example.BuildConfig
import com.example.data.api.Content
import com.example.data.api.GeminiApiService
import com.example.data.api.GenerateContentRequest
import com.example.data.api.Part
import kotlinx.coroutines.delay
import retrofit2.HttpException
import java.io.IOException

class GeminiRepository(
    private val geminiApiService: GeminiApiService
) {
    suspend fun analyzeStorage(
        totalGb: Long,
        usedGb: Long,
        freeGb: Long,
        categories: Map<String, Int>
    ): Result<String> {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "YOUR_API_KEY") {
            return Result.failure(Exception("Gemini API key is not configured."))
        }

        var prompt = "Please act as an intelligent Android storage analyzer. I will provide you with the storage statistics and the count of files by category. Provide a summary of the storage usage, tell me what is taking up the most space, and give 3 actionable tips to free up space.\n\n"
        prompt += "Storage Statistics:\n"
        prompt += "Total Space: $totalGb GB\n"
        prompt += "Used Space: $usedGb GB\n"
        prompt += "Free Space: $freeGb GB\n\n"
        
        prompt += "File Counts by Category:\n"
        categories.forEach { (category, count) ->
            prompt += "- $category: $count files\n"
        }
        
        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)))
            )
        )

        return executeWithExponentialBackoff {
            val response = geminiApiService.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("No content in response")
        }
    }

    suspend fun analyzeFile(file: java.io.File): Result<String> {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "YOUR_API_KEY") {
            return Result.failure(Exception("Gemini API key is not configured."))
        }

        var contentToSend = "Please analyze this file named: ${file.name}\n"
        val ext = file.extension.lowercase()
        if (ext in listOf("txt", "md", "csv", "json", "xml")) {
            val content = runCatching { file.readText().take(5000) }.getOrDefault("")
            contentToSend += "\nFile Content Preview:\n$content"
        } else {
            contentToSend += "It is a ${file.extension.uppercase()} file with size ${file.length()} bytes. Provide a general summary of what this file type is used for."
        }

        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = contentToSend)))
            )
        )

        return executeWithExponentialBackoff {
            val response = geminiApiService.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("No content in response")
        }
    }

    private suspend fun <T> executeWithExponentialBackoff(
        maxRetries: Int = 3,
        initialDelayMillis: Long = 1000,
        factor: Double = 2.0,
        block: suspend () -> T
    ): Result<T> {
        var currentDelay = initialDelayMillis
        for (attempt in 0 until maxRetries) {
            try {
                return Result.success(block())
            } catch (e: Exception) {
                val shouldRetry = when (e) {
                    is HttpException -> e.code() == 503 || e.code() == 429
                    is IOException -> true // Network issues
                    else -> false
                }
                
                if (shouldRetry && attempt < maxRetries - 1) {
                    delay(currentDelay)
                    currentDelay = (currentDelay * factor).toLong()
                } else {
                    return Result.failure(e)
                }
            }
        }
        return Result.failure(Exception("Max retries exceeded"))
    }
}
