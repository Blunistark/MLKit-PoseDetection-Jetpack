package com.example.mlkit_posedetection_jetpack.posedetector.graphic

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.max

/** Enhanced PoseGraphic with Bézier curves, dynamic thickness, glow, and all landmarks */
class PoseGraphic(
    overlay: GraphicOverlay,
    private val pose: Pose
) : GraphicOverlay.Graphic(overlay) {

    private val dotRadius = 10f
    private val glowRadius = 15f
    private val baseStroke = 6f

    private val leftGradient = Brush.linearGradient(listOf(Color(0xFF4CAF50), Color(0xFF81C784)))
    private val rightGradient = Brush.linearGradient(listOf(Color(0xFFFFC107), Color(0xFFFFD54F)))
    private val faceBrush = SolidColor(Color.White)

    override fun draw(canvas: DrawScope) {
        val landmarks = pose.allPoseLandmarks
        if (landmarks.isEmpty()) return

        // Helper: Get landmark safely
        fun get(id: Int) = pose.getPoseLandmark(id)

        // Draw limbs with Bézier curves
        fun drawCurve(start: PoseLandmark?, mid: PoseLandmark?, end: PoseLandmark?, brush: Brush) {
            if (start == null || mid == null || end == null) return
            val path = Path().apply {
                moveTo(translateX(start.position.x), translateY(start.position.y))
                quadraticBezierTo(
                    translateX(mid.position.x),
                    translateY(mid.position.y),
                    translateX(end.position.x),
                    translateY(end.position.y)
                )
            }
            val thickness = max(baseStroke, (start.inFrameLikelihood + end.inFrameLikelihood) * 8f)
            canvas.drawPath(
                path = path,
                brush = brush,
                style = Stroke(width = thickness)
            )
        }

        // Arms
        drawCurve(get(PoseLandmark.LEFT_SHOULDER), get(PoseLandmark.LEFT_ELBOW), get(PoseLandmark.LEFT_WRIST), leftGradient)
        drawCurve(get(PoseLandmark.RIGHT_SHOULDER), get(PoseLandmark.RIGHT_ELBOW), get(PoseLandmark.RIGHT_WRIST), rightGradient)

        // Legs
        drawCurve(get(PoseLandmark.LEFT_HIP), get(PoseLandmark.LEFT_KNEE), get(PoseLandmark.LEFT_ANKLE), leftGradient)
        drawCurve(get(PoseLandmark.RIGHT_HIP), get(PoseLandmark.RIGHT_KNEE), get(PoseLandmark.RIGHT_ANKLE), rightGradient)

        // Torso
        canvas.drawLine(
            start = Offset(translateX(get(PoseLandmark.LEFT_SHOULDER)?.position?.x ?: 0f),
                translateY(get(PoseLandmark.LEFT_SHOULDER)?.position?.y ?: 0f)),
            end = Offset(translateX(get(PoseLandmark.RIGHT_SHOULDER)?.position?.x ?: 0f),
                translateY(get(PoseLandmark.RIGHT_SHOULDER)?.position?.y ?: 0f)),
            brush = faceBrush,
            strokeWidth = baseStroke
        )
        canvas.drawLine(
            start = Offset(translateX(get(PoseLandmark.LEFT_HIP)?.position?.x ?: 0f),
                translateY(get(PoseLandmark.LEFT_HIP)?.position?.y ?: 0f)),
            end = Offset(translateX(get(PoseLandmark.RIGHT_HIP)?.position?.x ?: 0f),
                translateY(get(PoseLandmark.RIGHT_HIP)?.position?.y ?: 0f)),
            brush = faceBrush,
            strokeWidth = baseStroke
        )

        // Face connections
        val facePoints = listOf(
            Pair(get(PoseLandmark.NOSE), get(PoseLandmark.LEFT_EYE_INNER)),
            Pair(get(PoseLandmark.LEFT_EYE_INNER), get(PoseLandmark.LEFT_EYE)),
            Pair(get(PoseLandmark.LEFT_EYE), get(PoseLandmark.LEFT_EYE_OUTER)),
            Pair(get(PoseLandmark.LEFT_EYE_OUTER), get(PoseLandmark.LEFT_EAR)),
            Pair(get(PoseLandmark.NOSE), get(PoseLandmark.RIGHT_EYE_INNER)),
            Pair(get(PoseLandmark.RIGHT_EYE_INNER), get(PoseLandmark.RIGHT_EYE)),
            Pair(get(PoseLandmark.RIGHT_EYE), get(PoseLandmark.RIGHT_EYE_OUTER)),
            Pair(get(PoseLandmark.RIGHT_EYE_OUTER), get(PoseLandmark.RIGHT_EAR)),
            Pair(get(PoseLandmark.LEFT_MOUTH), get(PoseLandmark.RIGHT_MOUTH))
        )
        facePoints.forEach { (start, end) ->
            if (start != null && end != null)
                canvas.drawLine(
                    start = Offset(translateX(start.position.x), translateY(start.position.y)),
                    end = Offset(translateX(end.position.x), translateY(end.position.y)),
                    brush = faceBrush,
                    strokeWidth = baseStroke
                )
        }

        // Draw all 33 landmarks with glow
        for (landmark in landmarks) {
            val center = Offset(translateX(landmark.position.x), translateY(landmark.position.y))
            val alpha = landmark.inFrameLikelihood
            // Glow
            canvas.drawCircle(
                center = center,
                radius = glowRadius,
                brush = SolidColor(Color.White.copy(alpha = 0.2f * alpha))
            )
            // Dot
            canvas.drawCircle(
                center = center,
                radius = dotRadius,
                brush = SolidColor(Color.White.copy(alpha = alpha))
            )
        }
    }
}
