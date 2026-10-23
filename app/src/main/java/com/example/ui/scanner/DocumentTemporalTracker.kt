package com.example.ui.scanner

import android.graphics.PointF
import kotlin.math.*

/**
 * Temporal tracking, smoothing, hysteresis, and candidate switching protection
 * for the live document scanner pipeline.
 *
 * Prevents random background objects (bedsheet patterns, feet, hands, furniture)
 * from stealing the active document lock, while smoothly tracking genuine document movement
 * and distance changes.
 */
class DocumentTemporalTracker {

    enum class TrackingState {
        NO_DETECTION,       // No document currently detected
        ACQUIRING,          // Candidate seen for 1-2 frames, building confidence
        TRACKED_STRONG,     // High confidence document lock
        TRACKED_UNCERTAIN,  // Usable lock, but minor interference or brief frame dropout
        TEMPORARILY_LOST    // Document lost for a few frames; grace period active for recovery
    }

    data class TrackerOutput(
        val state: TrackingState,
        val smoothedQuad: DocumentScanProcessor.NormalizedQuad?,
        val confidence: Float,
        val isDetected: Boolean,
        val isStable: Boolean
    )

    private var currentState: TrackingState = TrackingState.NO_DETECTION
    private var trackedQuad: DocumentScanProcessor.NormalizedQuad? = null
    private var trackedConfidence: Float = 0f

    // History & Persistence Counters
    private var consecutiveValidFrames: Int = 0
    private var framesSinceLastDetection: Int = 0
    private var lastDetectionTimestamp: Long = 0L

    // Candidate Switching Protection
    private var competingCandidate: DocumentScanProcessor.NormalizedQuad? = null
    private var competingCandidateFrames: Int = 0
    private var competingCandidateConfidence: Float = 0f

    // Constants for tuning
    companion object {
        private const val MAX_LOST_GRACE_FRAMES = 10        // ~350-400ms at 25-30 FPS
        private const val FRAMES_TO_ACQUIRE = 2             // 2 consecutive agreeing frames to start tracking
        private const val FRAMES_TO_SWITCH = 4              // 4 consecutive frames required for a competing candidate to replace tracked doc
        private const val MAX_CORNER_MATCH_DIST = 0.22f     // Max normalized distance between corners to consider "same document"
        private const val MAX_CENTER_MATCH_DIST = 0.18f     // Max normalized distance between quad centers
    }

    /**
     * Resets all internal tracking history (called when scanner restarts or camera unbinds).
     */
    @Synchronized
    fun reset() {
        currentState = TrackingState.NO_DETECTION
        trackedQuad = null
        trackedConfidence = 0f
        consecutiveValidFrames = 0
        framesSinceLastDetection = 0
        lastDetectionTimestamp = 0L
        competingCandidate = null
        competingCandidateFrames = 0
        competingCandidateConfidence = 0f
    }

    /**
     * Updates tracker state with a raw single-frame candidate from the computer-vision detector.
     * Applies hysteresis, candidate switching protection, adaptive exponential corner smoothing,
     * distance scaling invariance, and grace-period recovery.
     */
    @Synchronized
    fun update(rawCandidate: DocumentScanProcessor.NormalizedQuad?, rawConfidence: Float, timestamp: Long = System.currentTimeMillis()): TrackerOutput {
        if (rawCandidate != null && rawConfidence >= 0.28f) {
            if (trackedQuad == null) {
                // Not currently tracking any document
                handleNewCandidateAcquisition(rawCandidate, rawConfidence, timestamp)
            } else {
                // Currently tracking an existing document
                handleExistingTrackedUpdate(rawCandidate, rawConfidence, timestamp)
            }
        } else {
            // No valid candidate in this frame (motion blur, lighting, brief obstruction)
            handleFrameDropout(timestamp)
        }

        val isDetected = (currentState == TrackingState.TRACKED_STRONG ||
                currentState == TrackingState.TRACKED_UNCERTAIN ||
                currentState == TrackingState.TEMPORARILY_LOST) && trackedQuad != null

        val isStable = (currentState == TrackingState.TRACKED_STRONG && trackedConfidence > 0.60f)

        return TrackerOutput(
            state = currentState,
            smoothedQuad = if (isDetected) trackedQuad else null,
            confidence = trackedConfidence,
            isDetected = isDetected,
            isStable = isStable
        )
    }

    private fun handleNewCandidateAcquisition(rawCandidate: DocumentScanProcessor.NormalizedQuad, rawConfidence: Float, timestamp: Long) {
        if (competingCandidate == null) {
            competingCandidate = rawCandidate
            competingCandidateFrames = 1
            competingCandidateConfidence = rawConfidence
            currentState = TrackingState.ACQUIRING
        } else {
            val dist = computeQuadDistance(competingCandidate!!, rawCandidate)
            if (dist < MAX_CORNER_MATCH_DIST) {
                competingCandidateFrames++
                competingCandidateConfidence = (competingCandidateConfidence * 0.5f) + (rawConfidence * 0.5f)
                // Smooth competing candidate slightly
                competingCandidate = interpolateQuads(competingCandidate!!, rawCandidate, 0.45f)

                if (competingCandidateFrames >= FRAMES_TO_ACQUIRE) {
                    trackedQuad = competingCandidate
                    trackedConfidence = competingCandidateConfidence
                    consecutiveValidFrames = competingCandidateFrames
                    framesSinceLastDetection = 0
                    lastDetectionTimestamp = timestamp
                    currentState = if (trackedConfidence >= 0.55f) TrackingState.TRACKED_STRONG else TrackingState.TRACKED_UNCERTAIN
                    competingCandidate = null
                    competingCandidateFrames = 0
                }
            } else {
                // Candidate jumped, reset acquisition to latest
                competingCandidate = rawCandidate
                competingCandidateFrames = 1
                competingCandidateConfidence = rawConfidence
                currentState = TrackingState.ACQUIRING
            }
        }
    }

    private fun handleExistingTrackedUpdate(rawCandidate: DocumentScanProcessor.NormalizedQuad, rawConfidence: Float, timestamp: Long) {
        val currentTracked = trackedQuad ?: return
        val cornerDist = computeQuadDistance(currentTracked, rawCandidate)
        val centerDist = computeCenterDistance(currentTracked, rawCandidate)
        val areaRatio = computeAreaRatio(currentTracked, rawCandidate)

        val isConsistentWithTracked = (cornerDist < MAX_CORNER_MATCH_DIST || (centerDist < MAX_CENTER_MATCH_DIST && areaRatio in 0.40f..2.50f))

        if (isConsistentWithTracked) {
            // MATCH: The raw candidate corresponds to our active document!
            // 1. Reset competing candidate counters
            competingCandidate = null
            competingCandidateFrames = 0

            // 2. Continuous velocity-damped quadratic temporal corner smoothing
            // Quadratic velocity damping curve: completely calms stationary micro-jitter when holding still,
            // while smoothly tracking dynamic camera translations with zero perceived lag.
            val t = ((cornerDist - 0.005f) / 0.08f).coerceIn(0f, 1f)
            val adaptiveAlpha = 0.14f + 0.60f * (t * t)

            trackedQuad = interpolateQuads(currentTracked, rawCandidate, adaptiveAlpha)
            trackedConfidence = (trackedConfidence * 0.70f) + (rawConfidence * 0.30f)
            consecutiveValidFrames++
            framesSinceLastDetection = 0
            lastDetectionTimestamp = timestamp

            currentState = if (trackedConfidence >= 0.50f) {
                TrackingState.TRACKED_STRONG
            } else {
                TrackingState.TRACKED_UNCERTAIN
            }
        } else {
            // DIVERGENT CANDIDATE: Could be a foot, hand, bedsheet pattern, or the user legitimately moved to another paper
            // CANDIDATE SWITCHING PROTECTION:
            // Do NOT immediately switch tracking to this new object!
            // Require the competing candidate to persist for FRAMES_TO_SWITCH (4) consecutive frames.

            if (competingCandidate == null) {
                competingCandidate = rawCandidate
                competingCandidateFrames = 1
                competingCandidateConfidence = rawConfidence
            } else {
                val compDist = computeQuadDistance(competingCandidate!!, rawCandidate)
                if (compDist < MAX_CORNER_MATCH_DIST) {
                    competingCandidateFrames++
                    competingCandidateConfidence = (competingCandidateConfidence * 0.5f) + (rawConfidence * 0.5f)
                    competingCandidate = interpolateQuads(competingCandidate!!, rawCandidate, 0.4f)

                    // Only switch if competing candidate persisted across multiple frames and has significant confidence
                    if (competingCandidateFrames >= FRAMES_TO_SWITCH && competingCandidateConfidence > 0.45f) {
                        trackedQuad = competingCandidate
                        trackedConfidence = competingCandidateConfidence
                        consecutiveValidFrames = competingCandidateFrames
                        framesSinceLastDetection = 0
                        lastDetectionTimestamp = timestamp
                        currentState = TrackingState.TRACKED_STRONG
                        competingCandidate = null
                        competingCandidateFrames = 0
                        return
                    }
                } else {
                    // Competing candidate was erratic, reset it
                    competingCandidate = rawCandidate
                    competingCandidateFrames = 1
                    competingCandidateConfidence = rawConfidence
                }
            }

            // In the meantime, protect the currently tracked document during this frame
            framesSinceLastDetection++
            if (framesSinceLastDetection <= MAX_LOST_GRACE_FRAMES) {
                currentState = TrackingState.TRACKED_UNCERTAIN
                trackedConfidence *= 0.92f
            } else {
                // Grace period expired with no consistent detection for the original document
                currentState = TrackingState.TEMPORARILY_LOST
                trackedConfidence *= 0.80f
                if (framesSinceLastDetection > MAX_LOST_GRACE_FRAMES + 6) {
                    reset()
                }
            }
        }
    }

    private fun handleFrameDropout(timestamp: Long) {
        competingCandidate = null
        competingCandidateFrames = 0

        if (trackedQuad != null) {
            framesSinceLastDetection++
            consecutiveValidFrames = 0

            if (framesSinceLastDetection <= MAX_LOST_GRACE_FRAMES) {
                // GRACE PERIOD: Hold the last known valid quadrilateral so the overlay doesn't flicker/vanish
                currentState = TrackingState.TEMPORARILY_LOST
                trackedConfidence *= 0.90f
            } else {
                // Truly lost: clear tracking
                reset()
            }
        } else {
            reset()
        }
    }

    /**
     * Computes the mean Euclidean distance across the 4 normalized corner points.
     */
    private fun computeQuadDistance(q1: DocumentScanProcessor.NormalizedQuad, q2: DocumentScanProcessor.NormalizedQuad): Float {
        val d1 = hypot(q1.topLeft.x - q2.topLeft.x, q1.topLeft.y - q2.topLeft.y)
        val d2 = hypot(q1.topRight.x - q2.topRight.x, q1.topRight.y - q2.topRight.y)
        val d3 = hypot(q1.bottomRight.x - q2.bottomRight.x, q1.bottomRight.y - q2.bottomRight.y)
        val d4 = hypot(q1.bottomLeft.x - q2.bottomLeft.x, q1.bottomLeft.y - q2.bottomLeft.y)
        return (d1 + d2 + d3 + d4) / 4f
    }

    private fun computeCenterDistance(q1: DocumentScanProcessor.NormalizedQuad, q2: DocumentScanProcessor.NormalizedQuad): Float {
        val c1x = (q1.topLeft.x + q1.topRight.x + q1.bottomRight.x + q1.bottomLeft.x) / 4f
        val c1y = (q1.topLeft.y + q1.topRight.y + q1.bottomRight.y + q1.bottomLeft.y) / 4f

        val c2x = (q2.topLeft.x + q2.topRight.x + q2.bottomRight.x + q2.bottomLeft.x) / 4f
        val c2y = (q2.topLeft.y + q2.topRight.y + q2.bottomRight.y + q2.bottomLeft.y) / 4f

        return hypot(c1x - c2x, c1y - c2y)
    }

    private fun computeAreaRatio(q1: DocumentScanProcessor.NormalizedQuad, q2: DocumentScanProcessor.NormalizedQuad): Float {
        val a1 = computeQuadArea(q1).coerceAtLeast(0.001f)
        val a2 = computeQuadArea(q2).coerceAtLeast(0.001f)
        return a2 / a1
    }

    private fun computeQuadArea(q: DocumentScanProcessor.NormalizedQuad): Float {
        val pts = arrayOf(q.topLeft, q.topRight, q.bottomRight, q.bottomLeft)
        return 0.5f * abs(
            (pts[0].x * pts[1].y - pts[1].x * pts[0].y) +
                    (pts[1].x * pts[2].y - pts[2].x * pts[1].y) +
                    (pts[2].x * pts[3].y - pts[3].x * pts[2].y) +
                    (pts[3].x * pts[0].y - pts[0].x * pts[3].y)
        )
    }

    private fun interpolateQuads(
        current: DocumentScanProcessor.NormalizedQuad,
        target: DocumentScanProcessor.NormalizedQuad,
        alpha: Float
    ): DocumentScanProcessor.NormalizedQuad {
        val clampedAlpha = alpha.coerceIn(0f, 1f)
        return DocumentScanProcessor.NormalizedQuad(
            topLeft = PointF(
                current.topLeft.x + (target.topLeft.x - current.topLeft.x) * clampedAlpha,
                current.topLeft.y + (target.topLeft.y - current.topLeft.y) * clampedAlpha
            ),
            topRight = PointF(
                current.topRight.x + (target.topRight.x - current.topRight.x) * clampedAlpha,
                current.topRight.y + (target.topRight.y - current.topRight.y) * clampedAlpha
            ),
            bottomRight = PointF(
                current.bottomRight.x + (target.bottomRight.x - current.bottomRight.x) * clampedAlpha,
                current.bottomRight.y + (target.bottomRight.y - current.bottomRight.y) * clampedAlpha
            ),
            bottomLeft = PointF(
                current.bottomLeft.x + (target.bottomLeft.x - current.bottomLeft.x) * clampedAlpha,
                current.bottomLeft.y + (target.bottomLeft.y - current.bottomLeft.y) * clampedAlpha
            )
        )
    }
}
