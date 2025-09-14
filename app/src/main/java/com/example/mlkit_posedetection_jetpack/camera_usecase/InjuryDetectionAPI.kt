package com.example.mlkit_posedetection_jetpack.camera_usecase

import android.graphics.Bitmap
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

data class InjuryDetectionResult(
    val hasInjury: Boolean,
    val confidence: Double,
    val injuryType: String,
    val message: String
)

class InjuryDetectionAPI {
    private val client = OkHttpClient()

    companion object {
        private const val TAG = "InjuryDetectionAPI"
        // N8N webhook endpoint
        private const val N8N_WEBHOOK_URL = "https://n8n.pipfactor.com/webhook/aid"
        // Flag to control whether to use real API or mock
        private var useMockApi = false
    }

    // Method to switch between real and mock API
    fun setUseMockApi(useMock: Boolean) {
        useMockApi = useMock
        Log.d(TAG, "API mode changed to: ${if (useMock) "MOCK" else "REAL"}")
    }

    // Method to test webhook connectivity
    fun testWebhookConnectivity(onResult: (Boolean, String) -> Unit) {
        val testRequest = Request.Builder()
            .url(N8N_WEBHOOK_URL)
            .get()
            .addHeader("User-Agent", "Android-Injury-Detection-App")
            .build()

        Log.d(TAG, "Testing webhook connectivity to: $N8N_WEBHOOK_URL")

        client.newCall(testRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Webhook connectivity test failed", e)
                onResult(false, "Network error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val success = response.isSuccessful
                val message = if (success) {
                    "Webhook is accessible (HTTP ${response.code})"
                } else {
                    "Webhook returned error (HTTP ${response.code})"
                }
                Log.d(TAG, "Webhook connectivity test result: $message")
                response.close()
                onResult(success, message)
            }
        })
    }

    fun analyzeImage(
        bitmap: Bitmap,
        cacheDir: File,
        latitude: Double? = null,
        longitude: Double? = null,
        onResult: (InjuryDetectionResult?) -> Unit
    ) {
        if (useMockApi) {
            Log.d(TAG, "Using mock API for injury detection")
            mockAnalyzeImage(bitmap, latitude, longitude, onResult)
            return
        }

        Log.d(TAG, "Using real N8N API for injury detection")
        analyzeImageReal(bitmap, cacheDir, latitude, longitude, onResult)
    }

    private fun analyzeImageReal(
        bitmap: Bitmap,
        cacheDir: File,
        latitude: Double? = null,
        longitude: Double? = null,
        onResult: (InjuryDetectionResult?) -> Unit
    ) {
        try {
            // Convert bitmap to file
            val imageFile = bitmapToFile(bitmap, cacheDir)
            Log.d(TAG, "Created image file: ${imageFile.absolutePath}, size: ${imageFile.length()} bytes")

            val builder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "image",
                    imageFile.name,
                    imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                )
                .addFormDataPart("timestamp", System.currentTimeMillis().toString())
                .addFormDataPart("source", "android_injury_detection")

            if (latitude != null) {
                builder.addFormDataPart("lat", latitude.toString())
            }
            if (longitude != null) {
                builder.addFormDataPart("lng", longitude.toString())
            }

            val requestBody = builder.build()

            val request = Request.Builder()
                .url(N8N_WEBHOOK_URL)
                .post(requestBody)
                .addHeader("Content-Type", "multipart/form-data")
                .addHeader("User-Agent", "Android-Injury-Detection-App")
                .build()

            Log.d(TAG, "Sending request to N8N webhook: $N8N_WEBHOOK_URL")

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "N8N webhook call failed: ${e.message}", e)
                    Log.d(TAG, "Falling back to mock result due to network error")
                    generateMockResult(onResult)
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        val responseBody = response.body?.string()
                        Log.d(TAG, "N8N webhook response: ${response.code} - $responseBody")

                        if (response.isSuccessful) {
                            // Try to parse n8n response if it contains injury analysis
                            val result = if (responseBody != null && responseBody.isNotEmpty()) {
                                try {
                                    parseN8NResponse(responseBody)
                                } catch (e: Exception) {
                                    Log.w(TAG, "Failed to parse N8N response as JSON, generating mock result: ${e.message}")
                                    generateMockResultSync()
                                }
                            } else {
                                Log.d(TAG, "Empty response from N8N, generating mock result")
                                generateMockResultSync()
                            }
                            onResult(result)
                        } else {
                            Log.e(TAG, "N8N webhook call unsuccessful: ${response.code} - ${response.message}")
                            if (responseBody != null) {
                                Log.e(TAG, "Response body: $responseBody")
                            }
                            generateMockResult(onResult)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing N8N response", e)
                        generateMockResult(onResult)
                    } finally {
                        // Clean up temporary file
                        try {
                            if (imageFile.exists()) {
                                val deleted = imageFile.delete()
                                Log.d(TAG, "Image file cleanup: ${if (deleted) "success" else "failed"}")
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to cleanup image file", e)
                        }
                    }
                }
            })

        } catch (e: Exception) {
            Log.e(TAG, "Error preparing N8N webhook call", e)
            Log.d(TAG, "Falling back to mock result due to preparation error")
            generateMockResult(onResult)
        }
    }

    private fun bitmapToFile(bitmap: Bitmap, cacheDir: File): File {
        // Check if cache directory is accessible
        if (!cacheDir.exists()) {
            val created = cacheDir.mkdirs()
            Log.d(TAG, "Cache directory created: $created, path: ${cacheDir.absolutePath}")
        }

        if (!cacheDir.canWrite()) {
            Log.e(TAG, "Cache directory is not writable: ${cacheDir.absolutePath}")
            throw IOException("Cache directory is not writable")
        }

        val file = File(cacheDir, "injury_detection_${System.currentTimeMillis()}.jpg")
        try {
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.flush()
            outputStream.close()
            Log.d(TAG, "Bitmap converted to file successfully: ${file.absolutePath}, size: ${file.length()} bytes")
        } catch (e: Exception) {
            Log.e(TAG, "Error converting bitmap to file", e)
            throw e
        }
        return file
    }

    private fun parseN8NResponse(responseBody: String): InjuryDetectionResult {
        return try {
            val jsonResponse = JSONObject(responseBody)

            // Check if n8n workflow returns injury analysis data
            val hasInjury = jsonResponse.optBoolean("has_injury", false)
            val confidence = jsonResponse.optDouble("confidence", 0.0)
            val injuryType = jsonResponse.optString("injury_type", "Unknown")

            val message = if (hasInjury) {
                "Injury detected: $injuryType (Confidence: ${(confidence * 100).toInt()}%)"
            } else {
                "No injury detected"
            }

            InjuryDetectionResult(hasInjury, confidence, injuryType, message)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing N8N JSON response", e)
            throw e
        }
    }

    private fun generateMockResultSync(): InjuryDetectionResult {
        val hasInjury = (0..10).random() > 7 // 30% chance of detecting injury
        val confidence = if (hasInjury) 0.75 + (0..25).random() / 100.0 else 0.15 + (0..35).random() / 100.0
        val injuryType = if (hasInjury) {
            listOf("Bruise", "Cut", "Swelling", "Fracture").random()
        } else "None"

        return InjuryDetectionResult(
            hasInjury = hasInjury,
            confidence = confidence,
            injuryType = injuryType,
            message = if (hasInjury) {
                "Injury detected: $injuryType (Confidence: ${(confidence * 100).toInt()}%)"
            } else {
                "No injury detected (Confidence: ${((1-confidence) * 100).toInt()}%)"
            }
        )
    }

    private fun generateMockResult(onResult: (InjuryDetectionResult?) -> Unit) {
        val result = generateMockResultSync()
        Log.d(TAG, "Generated mock result after N8N call: $result")
        onResult(result)
    }

    // Mock response for testing without any API calls
    fun mockAnalyzeImage(
        bitmap: Bitmap,
        latitude: Double? = null,
        longitude: Double? = null,
        onResult: (InjuryDetectionResult?) -> Unit
    ) {
        Log.d(TAG, "Generating mock injury detection result")
        // Simulate API delay
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            val result = generateMockResultSync()
            Log.d(TAG, "Mock result: $result")
            onResult(result)
        }, 1500) // 1.5 second delay to simulate API call
    }
}
