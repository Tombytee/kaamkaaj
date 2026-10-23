package com.example.ui.scanner

import android.graphics.PointF
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DocumentScannerHandoffTest {

    @Test
    fun `test normalized quad coordinate conversion to bitmap dimensions`() {
        val normQuad = DocumentScanProcessor.NormalizedQuad(
            topLeft = PointF(0.10f, 0.15f),
            topRight = PointF(0.85f, 0.12f),
            bottomRight = PointF(0.90f, 0.88f),
            bottomLeft = PointF(0.08f, 0.92f)
        )

        val imageWidth = 1920f
        val imageHeight = 1080f

        val corners = normQuad.toQuadCorners(imageWidth, imageHeight)

        assertEquals(192f, corners.topLeft.x, 0.01f)
        assertEquals(162f, corners.topLeft.y, 0.01f)

        assertEquals(1632f, corners.topRight.x, 0.01f)
        assertEquals(129.6f, corners.topRight.y, 0.01f)

        assertEquals(1728f, corners.bottomRight.x, 0.01f)
        assertEquals(950.4f, corners.bottomRight.y, 0.01f)

        assertEquals(153.6f, corners.bottomLeft.x, 0.01f)
        assertEquals(993.6f, corners.bottomLeft.y, 0.01f)
    }

    @Test
    fun `test frozen snapshot state immutability`() {
        val originalQuad = DocumentScanProcessor.NormalizedQuad(
            topLeft = PointF(0.1f, 0.1f),
            topRight = PointF(0.9f, 0.1f),
            bottomRight = PointF(0.9f, 0.9f),
            bottomLeft = PointF(0.1f, 0.9f)
        )

        val liveState = DocumentScanProcessor.LiveDetectionState(
            isDetected = true,
            normalizedQuad = originalQuad,
            confidence = 0.85f,
            timestamp = 1000L,
            isStable = true
        )

        // Freeze snapshot
        val frozenQuad = liveState.normalizedQuad?.let { quad ->
            DocumentScanProcessor.NormalizedQuad(
                topLeft = PointF(quad.topLeft.x, quad.topLeft.y),
                topRight = PointF(quad.topRight.x, quad.topRight.y),
                bottomRight = PointF(quad.bottomRight.x, quad.bottomRight.y),
                bottomLeft = PointF(quad.bottomLeft.x, quad.bottomLeft.y)
            )
        }
        val frozenState = DocumentScanProcessor.LiveDetectionState(
            isDetected = liveState.isDetected,
            normalizedQuad = frozenQuad,
            confidence = liveState.confidence,
            timestamp = liveState.timestamp,
            isStable = liveState.isStable
        )

        // Mutate original quad points
        originalQuad.topLeft.x = 0.5f
        originalQuad.topLeft.y = 0.5f

        // Frozen state should remain completely unaffected
        assertEquals(0.1f, frozenState.normalizedQuad!!.topLeft.x, 0.001f)
        assertEquals(0.1f, frozenState.normalizedQuad!!.topLeft.y, 0.001f)
    }
}
