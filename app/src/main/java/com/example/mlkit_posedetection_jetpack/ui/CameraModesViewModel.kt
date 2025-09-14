package com.example.mlkit_posedetection_jetpack.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CameraUiState(
    val selectedMode: Int = 0,
    val isFlashOn: Boolean = false,
    val isRecording: Boolean = false,
    val isFrontCamera: Boolean = false,
    val isProcessing: Boolean = false,
    val zoomLevel: Float = 1f,
    val capturedImageUri: String? = null,
    val injuryDetectionResult: String? = null,
    val errorMessage: String? = null,
    val showFocusIndicator: Boolean = false
)

class CameraModesViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()
    
    private val cameraModes = listOf("PHOTO", "VIDEO", "AI DOC", "INJURY", "POSE")
    
    fun selectMode(modeIndex: Int) {
        if (modeIndex in cameraModes.indices) {
            _uiState.value = _uiState.value.copy(selectedMode = modeIndex)
        }
    }
    
    fun toggleFlash() {
        _uiState.value = _uiState.value.copy(isFlashOn = !_uiState.value.isFlashOn)
    }
    
    fun switchCamera() {
        _uiState.value = _uiState.value.copy(isFrontCamera = !_uiState.value.isFrontCamera)
    }
    
    fun setZoomLevel(zoom: Float) {
        _uiState.value = _uiState.value.copy(zoomLevel = zoom.coerceIn(1f, 10f))
    }
    
    fun showFocusIndicator(show: Boolean) {
        _uiState.value = _uiState.value.copy(showFocusIndicator = show)
    }
    
    fun capturePhoto() {
        val currentMode = cameraModes[_uiState.value.selectedMode]
        
        when (currentMode) {
            "PHOTO" -> captureRegularPhoto()
            "AI DOC" -> captureDocumentScan()
            "INJURY" -> captureInjuryDetection()
            "POSE" -> capturePoseDetection()
        }
    }
    
    fun startVideoRecording() {
        _uiState.value = _uiState.value.copy(isRecording = true)
        // Implement video recording logic
    }
    
    fun stopVideoRecording() {
        _uiState.value = _uiState.value.copy(isRecording = false)
        // Implement video recording stop logic
    }
    
    private fun captureRegularPhoto() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            try {
                // Implement regular photo capture
                // Update UI state with captured image URI
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    capturedImageUri = "captured_photo_uri"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    errorMessage = "Failed to capture photo: ${e.message}"
                )
            }
        }
    }
    
    private fun captureDocumentScan() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            try {
                // Implement AI document scanning
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    capturedImageUri = "scanned_document_uri"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    errorMessage = "Failed to scan document: ${e.message}"
                )
            }
        }
    }
    
    private fun captureInjuryDetection() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            try {
                // Implement injury detection capture and analysis
                // This would call the InjuryDetectionAPI we created earlier
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    capturedImageUri = "injury_detection_uri",
                    injuryDetectionResult = "Analyzing for injuries..."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    errorMessage = "Failed to detect injuries: ${e.message}"
                )
            }
        }
    }
    
    private fun capturePoseDetection() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            try {
                // Implement pose detection capture
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    capturedImageUri = "pose_detection_uri"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    errorMessage = "Failed to detect pose: ${e.message}"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    fun clearResults() {
        _uiState.value = _uiState.value.copy(
            capturedImageUri = null,
            injuryDetectionResult = null
        )
    }
}
