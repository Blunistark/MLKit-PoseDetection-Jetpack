
# MLKit Pose Detection (Jetpack Compose)

This Android project demonstrates real-time pose detection using Google ML Kit and a modern Jetpack Compose UI. CameraX is used to capture frames and feed them into ML Kit for pose estimation.

Key features
- Pose detection via ML Kit (pose-detection)
- CameraX integration (camera preview, capture)
- Jetpack Compose UI with Material3
- Emergency cache fetch + local persistence (fetch JSON from a remote endpoint)
- Glass-overlay chatbot UI that talks to a remote reply API

APIs used for emergency & chatbot features
- Emergency cache (GET): https://unmixed-pseudofinally-shari.ngrok-free.app/api/emergency-cache
- Chat reply (POST): https://unmixed-pseudofinally-shari.ngrok-free.app/api/reply

Quick start (Windows)
1. Install Android Studio and the Android SDK.
2. Install a JDK (11 or 17). Set JAVA_HOME if needed. Example PowerShell:

	 $env:JAVA_HOME = 'C:\\Program Files\\Java\\jdk-17'

3. Open this folder in Android Studio or build from the terminal.

Build from terminal (optional)
- In PowerShell, from the project root run:

	./gradlew assembleDebug

Notes and troubleshooting
- If the build fails with "JAVA_HOME is set to an invalid directory", confirm JAVA_HOME points to a valid JDK installation.
- The project uses Jetpack Compose Material3 APIs; some APIs are experimental and require OptIn annotations in Kotlin files.
- If icons or material components report missing symbols, ensure the Compose material and material-icons dependencies are present in `app/build.gradle.kts` and the project has synced.

Project structure (important files)
- `app/src/main/java/.../EnhancedCameraScreen.kt` — main Compose screen, overlays for emergency-cache and chatbot.
- `app/src/main/java/.../InjuryDetectionAPI.kt` — network helpers for emergency-cache and chat reply.
- `app/src/main/java/.../CameraViewModel.kt` and `CameraModesViewModel.kt` — camera state & emergency cache state.

How to test the emergency/cache + chatbot
1. Run the app on a device or emulator with network access.
2. From the camera screen enable the overlay (chat/emergency) and the app will fetch the emergency-cache JSON from the endpoint and render it.
3. Use the chat UI to send messages — the UI POSTs to the reply endpoint and shows responses in the overlay.

If you want me to: I can now fix the compile errors in `EnhancedCameraScreen.kt` (imports, constants, and icon issues) and run a clean build — tell me to proceed.

---
Small original note: this project is intended as a minimal, easy-to-follow example for integrating ML Kit pose detection into a Compose app.

