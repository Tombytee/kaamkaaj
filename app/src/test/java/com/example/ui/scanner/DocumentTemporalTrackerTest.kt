package com.example.ui.scanner

import android.graphics.PointF
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DocumentTemporalTrackerTest {

    private lateinit var tracker: DocumentTemporalTracker

    @Before
    fun setUp() {
        tracker = DocumentTemporalTracker()
    }

    private fun createSampleQuad(inset: Float = 0.1f): DocumentScanProcessor.NormalizedQuad {
        return DocumentScanProcessor.NormalizedQuad(
            topLeft = PointF(inset, inset),
            topRight = PointF(1f - inset, inset),
            bottomRight = PointF(1f - inset, 1f - inset),
            bottomLeft = PointF(inset, 1f - inset)
        )
    }

    private fun createDivergentQuad(): DocumentScanProcessor.NormalizedQuad {
        // e.g. a foot or clutter in the bottom right corner
        return DocumentScanProcessor.NormalizedQuad(
            topLeft = PointF(0.6f, 0.6f),
            topRight = PointF(0.9f, 0.6f),
            bottomRight = PointF(0.9f, 0.9f),
            bottomLeft = PointF(0.6f, 0.9f)
        )
    }

    @Test
    fun `test initial acquisition requires agreement across frames`() {
        val quad = createSampleQuad(0.1f)

        // Frame 1: Initial candidate seen
        val out1 = tracker.update(quad, 0.70f)
        assertEquals(DocumentTemporalTracker.TrackingState.ACQUIRING, out1.state)
        assertFalse("Single frame should not immediately declare stable detection", out1.isDetected)

        // Frame 2: Candidate persists
        val out2 = tracker.update(quad, 0.72f)
        assertTrue(out2.isDetected)
        assertEquals(DocumentTemporalTracker.TrackingState.TRACKED_STRONG, out2.state)
        assertNotNull(out2.smoothedQuad)
    }

    @Test
    fun `test candidate switching protection prevents sudden single-frame takeover`() {
        val docQuad = createSampleQuad(0.1f)
        val intrudingFootQuad = createDivergentQuad()

        // Acquire document lock
        tracker.update(docQuad, 0.80f)
        val lockState = tracker.update(docQuad, 0.82f)
        assertTrue(lockState.isDetected)
        assertEquals(DocumentTemporalTracker.TrackingState.TRACKED_STRONG, lockState.state)

        // Frame 3: A foot or bedsheet pattern appears for a single frame
        val frame3 = tracker.update(intrudingFootQuad, 0.95f) // even with high single-frame score
        assertTrue("Tracked document should remain active during single-frame intrusion", frame3.isDetected)
        assertNotNull(frame3.smoothedQuad)
        // Verify smoothedQuad is close to original document, NOT the foot
        assertTrue(frame3.smoothedQuad!!.topLeft.x < 0.25f)

        // Frame 4: Intruder vanishes, document returns
        val frame4 = tracker.update(docQuad, 0.80f)
        assertTrue(frame4.isDetected)
        assertEquals(DocumentTemporalTracker.TrackingState.TRACKED_STRONG, frame4.state)
    }

    @Test
    fun `test temporary detection loss grace period prevents overlay flickering`() {
        val docQuad = createSampleQuad(0.15f)

        // Acquire lock
        tracker.update(docQuad, 0.80f)
        tracker.update(docQuad, 0.85f)
        val stateTracked = tracker.update(docQuad, 0.85f)
        assertTrue(stateTracked.isDetected)

        // Frame dropouts (e.g. fast hand movement or camera auto-exposure transition)
        val drop1 = tracker.update(null, 0f)
        assertTrue("Grace period should keep quad during brief frame dropout", drop1.isDetected)
        assertEquals(DocumentTemporalTracker.TrackingState.TEMPORARILY_LOST, drop1.state)

        val drop2 = tracker.update(null, 0f)
        assertTrue("Grace period continues", drop2.isDetected)

        // Document reappears: immediate seamless recovery
        val recovered = tracker.update(docQuad, 0.80f)
        assertTrue(recovered.isDetected)
        assertEquals(DocumentTemporalTracker.TrackingState.TRACKED_STRONG, recovered.state)
    }

    @Test
    fun `test adaptive corner smoothing follows document movement without sudden jumps`() {
        val docQuad1 = createSampleQuad(0.10f)
        tracker.update(docQuad1, 0.80f)
        tracker.update(docQuad1, 0.80f)

        // Small movement (hand tremor / micro movement)
        val docQuad2 = createSampleQuad(0.11f)
        val smoothOut = tracker.update(docQuad2, 0.80f)

        assertNotNull(smoothOut.smoothedQuad)
        // Corner coordinate should be interpolated between 0.10 and 0.11
        val tlX = smoothOut.smoothedQuad!!.topLeft.x
        assertTrue(tlX in 0.10f..0.11f)
    }

    @Test
    fun `test distance scaling handles zoom out and zoom in naturally`() {
        // Document at standard distance (inset 0.10)
        val docNear = createSampleQuad(0.10f)
        tracker.update(docNear, 0.80f)
        tracker.update(docNear, 0.80f)

        // Camera moves farther away: document shrinks (inset increases to 0.25)
        val docFar = createSampleQuad(0.25f)
        val farState = tracker.update(docFar, 0.75f)

        assertTrue("Distance change should maintain document tracking", farState.isDetected)
        assertNotNull(farState.smoothedQuad)
    }
}
