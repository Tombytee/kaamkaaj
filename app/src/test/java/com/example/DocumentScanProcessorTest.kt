package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.example.ui.scanner.DocumentScanProcessor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DocumentScanProcessorTest {

    @Test
    fun `test detectDocumentCorners for straight document on dark desk`() = runBlocking {
        // Create a 800x600 dark background bitmap with a bright white 400x300 document in the center
        val width = 800
        val height = 600
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Dark desk background
        val bgPaint = Paint().apply { color = Color.rgb(30, 30, 35) }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // White paper document in rectangle [150, 100, 650, 500]
        val docPaint = Paint().apply { color = Color.rgb(245, 245, 245) }
        canvas.drawRect(150f, 100f, 650f, 500f, docPaint)

        val corners = DocumentScanProcessor.detectDocumentCorners(bitmap)

        // Verify detected corners are close to actual rect (150,100), (650,100), (650,500), (150,500)
        assertTrue("Top-Left X should be near 150", abs(corners.topLeft.x - 150f) < 40f)
        assertTrue("Top-Left Y should be near 100", abs(corners.topLeft.y - 100f) < 40f)
        assertTrue("Top-Right X should be near 650", abs(corners.topRight.x - 650f) < 40f)
        assertTrue("Top-Right Y should be near 100", abs(corners.topRight.y - 100f) < 40f)
        assertTrue("Bottom-Right X should be near 650", abs(corners.bottomRight.x - 650f) < 40f)
        assertTrue("Bottom-Right Y should be near 500", abs(corners.bottomRight.y - 500f) < 40f)
        assertTrue("Bottom-Left X should be near 150", abs(corners.bottomLeft.x - 150f) < 40f)
        assertTrue("Bottom-Left Y should be near 500", abs(corners.bottomLeft.y - 500f) < 40f)

        // Test crop and straighten
        val cropped = DocumentScanProcessor.cropAndStraighten(bitmap, corners)
        assertNotNull(cropped)
        assertTrue("Cropped width should be around 500", cropped.width in 400..600)
        assertTrue("Cropped height should be around 400", cropped.height in 300..500)
    }

    @Test
    fun `test detectDocumentCorners for skewed perspective document`() = runBlocking {
        // Create an angled/skewed trapezoid document (perspective effect)
        val width = 800
        val height = 600
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint().apply { color = Color.rgb(25, 25, 30) }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Rasterize perspective trapezoid document [TL(220,120), TR(580,140), BR(660,480), BL(140,460)]
        // Using horizontal scanlines to ensure complete pixel rasterization across all JVM/Robolectric test runners
        val docPaint = Paint().apply { color = Color.rgb(240, 240, 245) }
        val docColor = Color.rgb(240, 240, 245)

        val pixels = IntArray(width * height) { Color.rgb(25, 25, 30) }

        val tlX = 220f; val tlY = 120f
        val trX = 580f; val trY = 140f
        val brX = 660f; val brY = 480f
        val blX = 140f; val blY = 460f

        for (y in 120 until 480) {
            val progressY = (y - 120f) / (480f - 120f)
            val leftX = tlX + progressY * (blX - tlX)
            val rightX = trX + progressY * (brX - trX)
            for (x in leftX.toInt() until rightX.toInt()) {
                if (x in 0 until width && y in 0 until height) {
                    pixels[y * width + x] = docColor
                }
            }
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)

        val corners = DocumentScanProcessor.detectDocumentCorners(bitmap)

        // Verify detected corners match the skewed trapezoid vertices
        assertTrue("Top-Left was ${corners.topLeft}, expected near (220, 120)", abs(corners.topLeft.x - 220f) < 55f && abs(corners.topLeft.y - 120f) < 55f)
        assertTrue("Top-Right was ${corners.topRight}, expected near (580, 140)", abs(corners.topRight.x - 580f) < 55f && abs(corners.topRight.y - 140f) < 55f)
        assertTrue("Bottom-Right was ${corners.bottomRight}, expected near (660, 480)", abs(corners.bottomRight.x - 660f) < 55f && abs(corners.bottomRight.y - 480f) < 55f)
        assertTrue("Bottom-Left was ${corners.bottomLeft}, expected near (140, 460)", abs(corners.bottomLeft.x - 140f) < 55f && abs(corners.bottomLeft.y - 460f) < 55f)
    }
}
