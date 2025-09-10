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
        // Replace with your actual cloud endpoint
        private const val API_ENDPOINT = "https://your-cloud-endpoint.com/api/injury-detection"
        private const val API_KEY = "YOUR_API_KEY" // Replace with your actual API key
    }

    fun analyzeImage(
        bitmap: Bitmap,
        cacheDir: File,
        onResult: (InjuryDetectionResult?) -> Unit
    ) {
        try {
            // Convert bitmap to file
            val imageFile = bitmapToFile(bitmap, cacheDir)
            
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "image", 
                    imageFile.name,
                    imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                )
                .build()
            
            val request = Request.Builder()
                .url(API_ENDPOINT)
                .post(requestBody)
                .addHeader("Authorization", "Bearer $API_KEY")
                .addHeader("Content-Type", "multipart/form-data")
                .build()
            
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "API call failed", e)
                    onResult(null)
                }
                
                override fun onResponse(call: Call, response: Response) {
                    try {
                        val responseBody = response.body?.string()
                        if (response.isSuccessful && responseBody != null) {
                            val result = parseResponse(responseBody)
                            onResult(result)
                        } else {
                            Log.e(TAG, "API call unsuccessful: ${response.code}")
                            onResult(null)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing response", e)
                        onResult(null)
                    } finally {
                        // Clean up temporary file
                        imageFile.delete()
                    }
                }
            })
            
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing API call", e)
            onResult(null)
        }
    }
    
    private fun bitmapToFile(bitmap: Bitmap, cacheDir: File): File {
        val file = File(cacheDir, "captured_image_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(file)
        
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        outputStream.flush()
        outputStream.close()
        
        return file
    }
    
    private fun parseResponse(responseBody: String): InjuryDetectionResult {
        return try {
            val jsonResponse = JSONObject(responseBody)
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
            Log.e(TAG, "Error parsing JSON response", e)
            InjuryDetectionResult(false, 0.0, "Error", "Failed to parse response")
        }
    }
    
    // Mock response for testing without actual API
    fun mockAnalyzeImage(
        bitmap: Bitmap,
        onResult: (InjuryDetectionResult?) -> Unit
    ) {
        // Simulate API delay
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            // Mock response - randomly determine if injury is detected
            val hasInjury = (0..10).random() > 7 // 30% chance of detecting injury
            val confidence = if (hasInjury) 0.75 + (0..25).random() / 100.0 else 0.15 + (0..35).random() / 100.0
            val injuryType = if (hasInjury) {
                listOf("Bruise", "Cut", "Swelling", "Fracture").random()
            } else "None"
            
            val result = InjuryDetectionResult(
                hasInjury = hasInjury,
                confidence = confidence,
                injuryType = injuryType,
                message = if (hasInjury) {
                    "Injury detected: $injuryType (Confidence: ${(confidence * 100).toInt()}%)"
                } else {
                    "No injury detected (Confidence: ${((1-confidence) * 100).toInt()}%)"
                }
            )
            
            Log.d(TAG, "Mock result: $result")
            onResult(result)
        }, 1500) // 1.5 second delay to simulate API call
    }
}
