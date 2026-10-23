package com.example.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val latestVersion: String,
    val latestVersionCode: Int,
    val updateUrl: String,
    val title: String,
    val message: String
)

sealed interface CheckUpdateResult {
    data class UpdateAvailable(val info: UpdateInfo) : CheckUpdateResult
    data class UpToDate(val currentVersion: String, val latestVersion: String) : CheckUpdateResult
    data class CheckFailed(val message: String = "Unable to connect to the update server. Please check your internet connection.") : CheckUpdateResult
}

sealed interface DownloadState {
    object Idle : DownloadState
    data class Downloading(
        val progress: Float, // 0.0f to 1.0f or -1f if total size unknown
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : DownloadState
    data class Completed(val apkFile: File) : DownloadState
    data class Failed(val message: String) : DownloadState
}

sealed interface ApkValidationResult {
    data class Valid(
        val apkFile: File,
        val apkPackageName: String,
        val apkVersionCode: Int,
        val apkVersionName: String
    ) : ApkValidationResult

    data class Corrupted(
        val message: String = "Downloaded package was incomplete or corrupted. Please retry."
    ) : ApkValidationResult

    data class Mismatch(
        val message: String,
        val apkFile: File
    ) : ApkValidationResult
}

object AppUpdateManager {
    const val UPDATE_JSON_URL = "https://raw.githubusercontent.com/aiwebnix-oss/kamkaaj-update/refs/heads/main/update.json"

    // Guard to ensure we check only once per app session for auto-check
    private var hasCheckedThisSession = false

    // Guard to ensure we show the offline update reminder only once per app session
    var hasShownOfflineReminderThisSession = false

    /**
     * Checks whether the device currently has an active internet connection.
     */
    fun isNetworkAvailable(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val activeNetwork = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            false
        }
    }

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private var downloadJob: Job? = null
    private var activeDownloadId: Long = -1L

    /**
     * Asynchronously checks if a newer version of the app is available.
     * Returns UpdateInfo if an update is available, or null otherwise.
     * Completely silent and error-free on network errors, timeouts, invalid JSON, or offline usage.
     */
    suspend fun checkForUpdate(
        installedVersionCode: Int = BuildConfig.VERSION_CODE,
        forceCheck: Boolean = false
    ): UpdateInfo? = withContext(Dispatchers.IO) {
        if (hasCheckedThisSession && !forceCheck) {
            return@withContext null
        }
        hasCheckedThisSession = true

        try {
            val result = checkUpdateDetailed(installedVersionCode)
            if (result is CheckUpdateResult.UpdateAvailable) {
                return@withContext result.info
            }
        } catch (_: Exception) {
            // Silently swallow all errors
        }
        return@withContext null
    }

    /**
     * Performs a detailed update check returning [CheckUpdateResult]
     * for explicit user-triggered checks in Settings.
     */
    suspend fun checkUpdateDetailed(
        installedVersionCode: Int = BuildConfig.VERSION_CODE
    ): CheckUpdateResult = withContext(Dispatchers.IO) {
        try {
            val url = URL(UPDATE_JSON_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "KamKaj-Android")
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                connection.disconnect()
                return@withContext CheckUpdateResult.CheckFailed("Update server responded with status ${connection.responseCode}.")
            }

            val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            if (jsonText.isBlank()) {
                return@withContext CheckUpdateResult.CheckFailed("Empty response from update server.")
            }

            val json = JSONObject(jsonText)

            val latestVersionCode = json.optInt("latestVersionCode", -1)
            val latestVersion = json.optString("latestVersion", "").trim()
            val updateUrl = json.optString("updateUrl", "").trim()
            val title = json.optString("title", "New KaamKaaj Update Available").trim()
            val message = json.optString("message", "A new version of KaamKaaj is available.").trim()

            val displayVersion = if (latestVersion.isNotBlank()) latestVersion else "v$latestVersionCode"

            // Compare version code strictly: latestVersionCode > installedVersionCode
            if (latestVersionCode > installedVersionCode && updateUrl.isNotBlank() && (updateUrl.startsWith("http://") || updateUrl.startsWith("https://"))) {
                return@withContext CheckUpdateResult.UpdateAvailable(
                    UpdateInfo(
                        latestVersion = displayVersion,
                        latestVersionCode = latestVersionCode,
                        updateUrl = updateUrl,
                        title = if (title.isNotBlank()) title else "New KaamKaaj Update Available",
                        message = if (message.isNotBlank()) message else "A new version of KaamKaaj is available."
                    )
                )
            } else {
                return@withContext CheckUpdateResult.UpToDate(
                    currentVersion = "v${BuildConfig.VERSION_NAME}",
                    latestVersion = displayVersion
                )
            }
        } catch (e: Exception) {
            return@withContext CheckUpdateResult.CheckFailed("Unable to reach update server. Please check your internet connection.")
        }
    }

    /**
     * Obtains the target file location for downloading the update APK.
     */
    fun getApkFile(context: Context, versionCode: Int): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: File(context.filesDir, "Download").apply { mkdirs() }
        return File(dir, "KaamKaaj_v$versionCode.apk")
    }

    /**
     * Validates that the downloaded APK file is readable, uncorrupted, matches application package name,
     * and contains a version code higher than the currently installed build.
     */
    fun validateApk(context: Context, apkFile: File, updateInfo: UpdateInfo): ApkValidationResult {
        if (!apkFile.exists() || !apkFile.canRead() || apkFile.length() <= 0L) {
            return ApkValidationResult.Corrupted("Downloaded file is missing or unreadable.")
        }

        val pm = context.packageManager
        val info = try {
            pm.getPackageArchiveInfo(apkFile.absolutePath, 0)
        } catch (_: Exception) {
            null
        }

        if (info == null || info.packageName.isNullOrBlank()) {
            return ApkValidationResult.Corrupted("Downloaded package was incomplete or corrupted. Please retry.")
        }

        val expectedPackageName = context.packageName
        if (info.packageName != expectedPackageName) {
            return ApkValidationResult.Mismatch(
                message = "Package mismatch: Downloaded package '${info.packageName}' does not match expected '$expectedPackageName'.",
                apkFile = apkFile
            )
        }

        @Suppress("DEPRECATION")
        val apkVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode.toInt()
        } else {
            info.versionCode
        }
        val apkVersionName = info.versionName ?: "v$apkVersionCode"
        val installedVersionCode = BuildConfig.VERSION_CODE

        if (apkVersionCode <= installedVersionCode) {
            return ApkValidationResult.Mismatch(
                message = "Downloaded APK version ($apkVersionName, code $apkVersionCode) is not newer than installed version (v${BuildConfig.VERSION_NAME}, code $installedVersionCode).",
                apkFile = apkFile
            )
        }

        return ApkValidationResult.Valid(
            apkFile = apkFile,
            apkPackageName = info.packageName,
            apkVersionCode = apkVersionCode,
            apkVersionName = apkVersionName
        )
    }

    /**
     * Legacy boolean validator for backward compatibility.
     */
    fun isValidApk(context: Context, apkFile: File, expectedVersionCode: Int): Boolean {
        val result = validateApk(context, apkFile, UpdateInfo("", expectedVersionCode, "", "", ""))
        return result is ApkValidationResult.Valid
    }

    /**
     * Enqueues an Android-native APK download using [DownloadManager] and polls for completion.
     */
    fun startDownload(context: Context, updateInfo: UpdateInfo, scope: CoroutineScope) {
        val apkFile = getApkFile(context, updateInfo.latestVersionCode)

        // Check if a valid, complete APK is already present on disk
        val initialValidation = validateApk(context, apkFile, updateInfo)
        if (initialValidation is ApkValidationResult.Valid) {
            _downloadState.value = DownloadState.Completed(initialValidation.apkFile)
            installApk(context, initialValidation.apkFile)
            return
        }

        // Clean up previous job and partial file
        cancelDownload(context)
        if (apkFile.exists()) {
            apkFile.delete()
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            ?: run {
                _downloadState.value = DownloadState.Failed("DownloadManager is unavailable on this device.")
                return
            }

        val request = DownloadManager.Request(Uri.parse(updateInfo.updateUrl)).apply {
            setTitle("KaamKaaj Update ${updateInfo.latestVersion}")
            setDescription("Downloading latest release package...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setMimeType("application/vnd.android.package-archive")
            setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "KaamKaaj_v${updateInfo.latestVersionCode}.apk")
        }

        val downloadId = try {
            downloadManager.enqueue(request)
        } catch (e: Exception) {
            _downloadState.value = DownloadState.Failed("Could not start download: ${e.localizedMessage}")
            return
        }

        activeDownloadId = downloadId
        _downloadState.value = DownloadState.Downloading(progress = 0f, bytesDownloaded = 0L, totalBytes = -1L)

        downloadJob = scope.launch(Dispatchers.IO) {
            var isRunning = true
            while (isRunning && isActive) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)
                if (cursor != null && cursor.moveToFirst()) {
                    val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val bytesIdx = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val totalIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)

                    val status = if (statusIdx != -1) cursor.getInt(statusIdx) else -1
                    val bytesDownloaded = if (bytesIdx != -1) cursor.getLong(bytesIdx) else 0L
                    val totalBytes = if (totalIdx != -1) cursor.getLong(totalIdx) else -1L

                    val progress = if (totalBytes > 0) {
                        (bytesDownloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                    } else {
                        -1f
                    }

                    when (status) {
                        DownloadManager.STATUS_RUNNING,
                        DownloadManager.STATUS_PAUSED,
                        DownloadManager.STATUS_PENDING -> {
                            _downloadState.value = DownloadState.Downloading(
                                progress = progress,
                                bytesDownloaded = bytesDownloaded,
                                totalBytes = totalBytes
                            )
                        }
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            isRunning = false
                            cursor.close()

                            when (val result = validateApk(context, apkFile, updateInfo)) {
                                is ApkValidationResult.Valid -> {
                                    _downloadState.value = DownloadState.Completed(result.apkFile)
                                    withContext(Dispatchers.Main) {
                                        installApk(context, result.apkFile)
                                    }
                                }
                                is ApkValidationResult.Corrupted -> {
                                    if (apkFile.exists()) apkFile.delete()
                                    _downloadState.value = DownloadState.Failed(result.message)
                                }
                                is ApkValidationResult.Mismatch -> {
                                    if (apkFile.exists()) apkFile.delete()
                                    _downloadState.value = DownloadState.Failed(result.message)
                                }
                            }
                            return@launch
                        }
                        DownloadManager.STATUS_FAILED -> {
                            isRunning = false
                            cursor.close()
                            if (apkFile.exists()) apkFile.delete()
                            _downloadState.value = DownloadState.Failed("Download failed. Please check your internet connection and try again.")
                            return@launch
                        }
                    }
                    cursor.close()
                } else {
                    isRunning = false
                    if (apkFile.exists()) apkFile.delete()
                    _downloadState.value = DownloadState.Failed("Download session was interrupted.")
                    return@launch
                }
                delay(350)
            }
        }
    }

    /**
     * Cancels any active download and cleans up state.
     */
    fun cancelDownload(context: Context) {
        downloadJob?.cancel()
        downloadJob = null
        if (activeDownloadId != -1L) {
            try {
                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                downloadManager?.remove(activeDownloadId)
            } catch (_: Exception) {}
            activeDownloadId = -1L
        }
        _downloadState.value = DownloadState.Idle
    }

    /**
     * Resets download state to Idle without cancelling any completed file.
     */
    fun resetDownloadState() {
        downloadJob?.cancel()
        downloadJob = null
        activeDownloadId = -1L
        _downloadState.value = DownloadState.Idle
    }

    /**
     * Launches the Android Package Installer using FileProvider content URI.
     */
    fun installApk(context: Context, apkFile: File) {
        if (!apkFile.exists()) return

        // Check Android 8.0+ (API 26+) Unknown App Install Permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                try {
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                } catch (_: Exception) {}
                return
            }
        }

        val apkUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
        } else {
            Uri.fromFile(apkFile)
        }

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            context.startActivity(installIntent)
        } catch (e: Exception) {
            _downloadState.value = DownloadState.Failed("Unable to launch Package Installer: ${e.localizedMessage}")
        }
    }

    /**
     * Legacy open URL function retained for backward compatibility.
     */
    fun openUpdateUrl(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}
