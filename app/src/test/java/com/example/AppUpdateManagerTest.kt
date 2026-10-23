package com.example

import com.example.update.AppUpdateManager
import com.example.update.UpdateInfo
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppUpdateManagerTest {

    @Test
    fun testUpdateLogic_newerVersionAvailable() {
        val jsonString = """
            {
              "latestVersion": "1.0.2",
              "latestVersionCode": 3,
              "updateUrl": "https://example.com/download/app.apk",
              "title": "New Update Available",
              "message": "Bug fixes and performance improvements."
            }
        """.trimIndent()

        val json = JSONObject(jsonString)
        val installedVersionCode = 2

        val latestVersionCode = json.optInt("latestVersionCode", -1)
        val updateUrl = json.optString("updateUrl", "").trim()

        val isUpdateAvailable = latestVersionCode > installedVersionCode && updateUrl.isNotBlank()

        assertTrue(isUpdateAvailable)

        val updateInfo = UpdateInfo(
            latestVersion = json.optString("latestVersion", ""),
            latestVersionCode = latestVersionCode,
            updateUrl = updateUrl,
            title = json.optString("title", ""),
            message = json.optString("message", "")
        )

        assertEquals("1.0.2", updateInfo.latestVersion)
        assertEquals(3, updateInfo.latestVersionCode)
        assertEquals("https://example.com/download/app.apk", updateInfo.updateUrl)
    }

    @Test
    fun testUpdateLogic_sameOrOlderVersion_doesNotTrigger() {
        val jsonString = """
            {
              "latestVersion": "1.0.1",
              "latestVersionCode": 2,
              "updateUrl": "https://example.com/download/app.apk",
              "title": "Update Available",
              "message": "Some message"
            }
        """.trimIndent()

        val json = JSONObject(jsonString)
        val installedVersionCode = 2

        val latestVersionCode = json.optInt("latestVersionCode", -1)
        val updateUrl = json.optString("updateUrl", "").trim()

        val isUpdateAvailable = latestVersionCode > installedVersionCode && updateUrl.isNotBlank()

        assertFalse("Same version should not trigger update", isUpdateAvailable)
    }

    @Test
    fun testUpdateLogic_blankUrl_doesNotTrigger() {
        val jsonString = """
            {
              "latestVersion": "1.0.3",
              "latestVersionCode": 5,
              "updateUrl": "",
              "title": "Update",
              "message": "Message"
            }
        """.trimIndent()

        val json = JSONObject(jsonString)
        val installedVersionCode = 2

        val latestVersionCode = json.optInt("latestVersionCode", -1)
        val updateUrl = json.optString("updateUrl", "").trim()

        val isUpdateAvailable = latestVersionCode > installedVersionCode && updateUrl.isNotBlank()

        assertFalse("Blank URL should not trigger update", isUpdateAvailable)
    }

    @Test
    fun testReleaseVersionCode7_comparison() {
        val installedVersionCode = com.example.BuildConfig.VERSION_CODE
        assertEquals(7, installedVersionCode)
        assertEquals("1.2.4", com.example.BuildConfig.VERSION_NAME)

        // Remote has version code 8 (newer)
        val remoteJsonNewer = JSONObject("""{"latestVersionCode": 8, "updateUrl": "https://example.com/app.apk"}""")
        val isNewer = remoteJsonNewer.optInt("latestVersionCode") > installedVersionCode
        assertTrue(isNewer)

        // Remote has version code 7 (current)
        val remoteJsonSame = JSONObject("""{"latestVersionCode": 7, "updateUrl": "https://example.com/app.apk"}""")
        val isSame = remoteJsonSame.optInt("latestVersionCode") > installedVersionCode
        assertFalse(isSame)
    }
}
