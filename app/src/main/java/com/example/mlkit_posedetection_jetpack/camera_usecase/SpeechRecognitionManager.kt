package com.example.mlkit_posedetection_jetpack.camera_usecase

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.*

class SpeechRecognitionManager(
    private val context: Context,
    private val onSpeechResult: (String) -> Unit,
    private val onListeningStateChanged: (Boolean) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    
    companion object {
        private const val TAG = "SpeechRecognition"
    }

    fun startListening() {
        if (isListening) return
        
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer?.setRecognitionListener(recognitionListener)
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        
        speechRecognizer?.startListening(intent)
        isListening = true
        onListeningStateChanged(true)
        Log.d(TAG, "Started listening for speech")
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
        isListening = false
        onListeningStateChanged(false)
        Log.d(TAG, "Stopped listening for speech")
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "Ready for speech")
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "Beginning of speech")
        }

        override fun onRmsChanged(rmsdB: Float) {
            // RMS value changed - can be used for visual feedback
        }

        override fun onBufferReceived(buffer: ByteArray?) {
            // Audio buffer received
        }

        override fun onEndOfSpeech() {
            Log.d(TAG, "End of speech")
        }

        override fun onError(error: Int) {
            Log.e(TAG, "Speech recognition error: $error")
            // Restart listening after a short delay
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (isListening) {
                    startListening()
                }
            }, 1000)
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            matches?.forEach { result ->
                Log.d(TAG, "Speech result: $result")
                onSpeechResult(result)
            }
            
            // Restart listening for continuous recognition
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (isListening) {
                    startListening()
                }
            }, 500)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            matches?.forEach { result ->
                Log.d(TAG, "Partial speech result: $result")
                onSpeechResult(result)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
            Log.d(TAG, "Speech event: $eventType")
        }
    }
}
