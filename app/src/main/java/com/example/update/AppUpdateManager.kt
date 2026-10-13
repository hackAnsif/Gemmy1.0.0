package com.example.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class AppUpdateInfo(
    val versionTag: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val downloadUrl: String?,
    val apkName: String,
    val isUpdateAvailable: Boolean,
    val currentVersion: String = BuildConfig.VERSION_NAME
)

object AppUpdateManager {

    private const val TAG = "AppUpdateManager"
    const val DEFAULT_REPO = "google-gemmy/gemmy" // Default placeholder repo, customizable in Settings

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Checks GitHub API for the latest published release in the specified repo (owner/repo format).
     */
    suspend fun checkForUpdate(
        context: Context,
        repoOwnerAndName: String
    ): AppUpdateInfo? = withContext(Dispatchers.IO) {
        val repo = repoOwnerAndName.trim().ifBlank { DEFAULT_REPO }
        if (!repo.contains("/")) {
            Log.w(TAG, "Invalid GitHub repository format: $repo. Expected owner/repo")
            return@withContext null
        }

        val url = "https://api.github.com/repos/$repo/releases/latest"
        try {
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "Gemmy-Android-Updater")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "GitHub Releases API returned HTTP ${response.code}")
                    return@withContext null
                }

                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)

                val tagName = json.optString("tag_name", "").trim()
                val title = json.optString("name", tagName).trim()
                val notes = json.optString("body", "Bug fixes and improvements.").trim()

                // Find APK in assets
                var apkUrl: String? = null
                var apkName = "update.apk"
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        val downloadUrl = asset.optString("browser_download_url", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            // Prefer release apk if multiple
                            if (name.contains("release", ignoreCase = true) || apkUrl == null) {
                                apkUrl = downloadUrl
                                apkName = name
                            }
                        }
                    }
                }

                val currentVersion = BuildConfig.VERSION_NAME
                val isNewer = isRemoteVersionNewer(tagName, currentVersion)

                Log.d(TAG, "Latest release on GitHub: $tagName (isNewer=$isNewer, current=$currentVersion, apkUrl=$apkUrl)")

                AppUpdateInfo(
                    versionTag = tagName,
                    releaseTitle = title.ifBlank { "Gemmy $tagName" },
                    releaseNotes = notes,
                    downloadUrl = apkUrl,
                    apkName = apkName,
                    isUpdateAvailable = isNewer && apkUrl != null,
                    currentVersion = currentVersion
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking GitHub release updates: ${e.message}", e)
            null
        }
    }

    /**
     * Compares version tags (e.g. v1.1 vs 1.0 or v1.0.1 vs 1.0.0)
     */
    fun isRemoteVersionNewer(remoteTag: String, currentVersion: String): Boolean {
        val cleanRemote = remoteTag.removePrefix("v").removePrefix("V").trim()
        val cleanCurrent = currentVersion.removePrefix("v").removePrefix("V").trim()

        if (cleanRemote.isBlank()) return false
        if (cleanRemote == cleanCurrent) return false

        val remoteParts = cleanRemote.split(".", "-", "_")
        val currentParts = cleanCurrent.split(".", "-", "_")

        val maxLength = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLength) {
            val r = remoteParts.getOrNull(i)?.toIntOrNull() ?: 0
            val c = currentParts.getOrNull(i)?.toIntOrNull() ?: 0
            if (r > c) return true
            if (r < c) return false
        }

        // If numeric parts are equal, check raw lexicographical difference
        return cleanRemote > cleanCurrent
    }

    /**
     * Downloads APK from GitHub release and triggers the Android Package Installer.
     */
    suspend fun downloadAndInstall(
        context: Context,
        downloadUrl: String,
        apkName: String,
        onProgress: (Float) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val targetDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val apkFile = File(targetDir, apkName.ifBlank { "update.apk" })
            if (apkFile.exists()) {
                apkFile.delete()
            }

            val request = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "Gemmy-Android-Updater")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        onError("Failed to download APK (HTTP ${response.code})")
                    }
                    return@withContext
                }

                val body = response.body ?: run {
                    withContext(Dispatchers.Main) { onError("Empty download response body") }
                    return@withContext
                }

                val contentLength = body.contentLength()
                val inputStream = body.byteStream()
                val outputStream = FileOutputStream(apkFile)

                val buffer = ByteArray(8 * 1024)
                var bytesRead: Int
                var totalBytesRead = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    if (contentLength > 0) {
                        val progress = (totalBytesRead.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f)
                        withContext(Dispatchers.Main) {
                            onProgress(progress)
                        }
                    }
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                withContext(Dispatchers.Main) {
                    onProgress(1.0f)
                    installApk(context, apkFile)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading and installing APK: ${e.message}", e)
            withContext(Dispatchers.Main) {
                onError("Download error: ${e.message ?: "Unknown error"}")
            }
        }
    }

    /**
     * Prompts the native Android package installer to install the downloaded APK.
     */
    fun installApk(context: Context, apkFile: File) {
        try {
            if (!apkFile.exists() || apkFile.length() == 0L) {
                Log.e(TAG, "Cannot install: APK file does not exist or is empty")
                return
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            // On Android 8.0+, check if install permission is granted
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    // Open permission request screen
                    val manageIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(manageIntent)
                }
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package installer: ${e.message}", e)
        }
    }
}
