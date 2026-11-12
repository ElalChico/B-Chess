package com.chessx.data

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.BufferedSink
import okio.BufferedSource
import okio.Buffer
import okio.Okio
import okio.buffer
import okio.sink
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * UpdateManager - Manejo de actualizaciones in-app
 * 
 * Características:
 * - Verifica actualizaciones en GitHub Releases
 * - Descarga APK firmado con mismo keystore (datos persisten)
 * - Instala sin perder historial/perfil (mismo packageId + misma firma)
 * - Solo borra datos en: desinstalación manual, factory reset, o reinstall limpio
 * 
 * IMPORTANTE: El APK DEBE estar firmado con el MISMO keystore que la versión actual
 * para que Android preserve los datos al actualizar.
 */
class UpdateManager private constructor(context: Context) {

    private val ctx = context.applicationContext
    private val updatePrefs = ctx.getSharedPreferences("bchess_update_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    // GitHub repo para releases
    private val repoOwner = "ElalChico"
    private val repoName = "B-Chess"
    private val apiUrl = "https://api.github.com/repos/$repoOwner/$repoName/releases?per_page=20"
    private val apkPrefixes = listOf("B-Chess-", "Chess-X-")

    // Estados
    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState

    companion object {
        @Volatile private var INSTANCE: UpdateManager? = null
        fun getInstance(context: Context): UpdateManager = INSTANCE ?: synchronized(this) {
            INSTANCE ?: UpdateManager(context).also { INSTANCE = it }
        }
    }

    /**
     * Versión actual de la app
     */
    fun getCurrentVersion(): VersionInfo {
        try {
            val pkg = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
            return VersionInfo(
                versionCode = pkg.longVersionCode,
                versionName = pkg.versionName ?: "unknown"
            )
        } catch (e: Exception) {
            return VersionInfo(0, "unknown")
        }
    }

    fun getLatestKnownVersion(): VersionInfo {
        val current = getCurrentVersion()
        return VersionInfo(
            updatePrefs.getLong("latest_version_code", current.versionCode),
            updatePrefs.getString("latest_version_name", current.versionName) ?: current.versionName
        )
    }

    /**
     * Verifica si hay actualización disponible en GitHub
     */
    suspend fun checkForUpdate(): Result<UpdateCheckResult> {
        _updateState.value = UpdateState.Checking
        return suspendCoroutine { cont ->
            val request = Request.Builder()
                .url(apiUrl)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "B-Chess-Android")
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("UpdateManager", "checkForUpdate failed", e)
                    _updateState.value = UpdateState.Error(e.message ?: "Error de red")
                    cont.resume(Result.failure(e))
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        if (!response.isSuccessful) {
                            _updateState.value = UpdateState.Error("HTTP ${response.code}")
                            cont.resume(Result.failure(Exception("HTTP ${response.code}")))
                        } else {
                            val json = response.body?.string() ?: "{}"
                            response.close()
                            val result = parseReleaseJson(json)
                            if (result.isSuccess) {
                                ctx.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)
                                    .edit()
                                    .putLong("last_auto_check_ms", System.currentTimeMillis())
                                    .apply()
                            }
                            cont.resume(result)
                        }
                    } catch (e: Exception) {
                        Log.e("UpdateManager", "checkForUpdate parsing failed", e)
                        _updateState.value = UpdateState.Error(e.message ?: "Error parseando")
                        cont.resume(Result.failure(e))
                    }
                }
            })
        }
    }

    private fun parseReleaseJson(json: String): Result<UpdateCheckResult> {
        try {
            val releases = org.json.JSONArray(json)
            val current = getCurrentVersion()
            var latest: UpdateCheckResult? = null
            var latestVersion = current
            for (releaseIndex in 0 until releases.length()) {
                val release = releases.optJSONObject(releaseIndex) ?: continue
                val tagName = release.optString("tag_name")
                val remoteVersion = runCatching { parseVersionName(tagName) }.getOrNull() ?: continue
                if (remoteVersion.versionCode > latestVersion.versionCode) latestVersion = remoteVersion
                if (remoteVersion.versionCode <= current.versionCode) continue

                val assets = release.optJSONArray("assets") ?: continue
                var apkUrl: String? = null
                var apkSize = 0L
                for (assetIndex in 0 until assets.length()) {
                    val asset = assets.optJSONObject(assetIndex) ?: continue
                    if (apkPrefixes.any { prefix ->
                            asset.optString("name").equals("$prefix${remoteVersion.versionName}.apk", ignoreCase = true)
                        }
                    ) {
                        apkUrl = asset.optString("browser_download_url").takeIf { it.isNotBlank() }
                        apkSize = asset.optLong("size", 0L)
                        break
                    }
                }
                if (apkUrl == null) continue

                val candidate = UpdateCheckResult(
                    hasUpdate = true,
                    currentVersion = current,
                    remoteVersion = remoteVersion,
                    releaseName = release.optString("name").ifBlank { "Release $tagName" },
                    releaseNotes = release.optString("body", "Sin notas de versión"),
                    apkUrl = apkUrl,
                    apkSize = apkSize,
                    isPrerelease = release.optBoolean("prerelease", false)
                )
                if (latest == null || candidate.remoteVersion.versionCode > latest.remoteVersion.versionCode) {
                    latest = candidate
                }
            }

            updatePrefs.edit()
                .putLong("latest_version_code", latestVersion.versionCode)
                .putString("latest_version_name", latestVersion.versionName)
                .apply()

            val result = latest ?: UpdateCheckResult(
                hasUpdate = false,
                currentVersion = current,
                remoteVersion = latestVersion,
                releaseName = "",
                releaseNotes = "",
                apkUrl = null,
                apkSize = 0L,
                isPrerelease = false
            )
            _updateState.value = if (result.hasUpdate) UpdateState.UpdateAvailable(result) else UpdateState.UpToDate
            return Result.success(result)
        } catch (e: Exception) {
            Log.e("UpdateManager", "parseReleaseJson failed", e)
            _updateState.value = UpdateState.Error("Error parseando release")
            return Result.failure(e)
        }
    }

    private fun parseVersionName(tag: String): VersionInfo {
        val match = Regex("(\\d+)\\.(\\d+)\\.(\\d+)").find(tag)
            ?: throw IllegalArgumentException("Release tag must contain a semantic version, e.g. v1.0.1")
        val major = match.groupValues[1].toLong()
        val minor = match.groupValues[2].toLong()
        val patch = match.groupValues[3].toLong()
        require(minor < 1_000 && patch < 1_000) { "MINOR and PATCH must be below 1000" }
        val code = major * 1_000_000L + minor * 1_000L + patch
        return VersionInfo(code, "$major.$minor.$patch")
    }

    fun shouldCheckForUpdateAutomatically(): Boolean {
        val prefs = ctx.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)
        val lastCheckMs = prefs.getLong("last_auto_check_ms", 0L)
        val now = System.currentTimeMillis()
        return now - lastCheckMs >= TimeUnit.DAYS.toMillis(1)
    }

    /**
     * Descarga e instala el APK (método directo sin Play Store)
     * Requiere: APK firmado con MISMO keystore
     */
    suspend fun downloadAndInstall(
        apkUrl: String,
        onProgress: (Float) -> Unit = {}
    ): Result<Unit> {
        _updateState.value = UpdateState.Downloading(0f)
        
        val file = File(ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "b-chess-update.apk")
        if (file.exists()) file.delete()

        return suspendCoroutine { cont ->
            val request = Request.Builder().url(apkUrl).build()
            
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("UpdateManager", "downloadAndInstall failed", e)
                    _updateState.value = UpdateState.Error(e.message ?: "Error descargando")
                    cont.resume(Result.failure(e))
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        if (!response.isSuccessful) {
                            _updateState.value = UpdateState.Error("Descarga falló: HTTP ${response.code}")
                            response.close()
                            cont.resume(Result.failure(Exception("HTTP ${response.code}")))
                            return
                        }
                        
                        val total = response.body?.contentLength() ?: 0L
                        var downloaded = 0L
                        val source: BufferedSource = response.body?.source()
                            ?: throw IOException("Release download had an empty response body")
                        val sink: BufferedSink = FileOutputStream(file).sink().buffer()
                        
                        val buffer = Buffer()
                        while (true) {
                            val read = source.read(buffer, 8192)
                            if (read == -1L) break
                            downloaded += read
                            sink.write(buffer, read)
                            if (total > 0) {
                                val progress = downloaded.toFloat() / total.toFloat()
                                onProgress(progress)
                                _updateState.value = UpdateState.Downloading(progress)
                            }
                        }
                        sink.flush()
                        sink.close()
                        source.close()
                        response.close()
                        
                        _updateState.value = UpdateState.Downloaded(file)
                        val installResult = installApk(file)
                        cont.resume(installResult)
                    } catch (e: Exception) {
                        Log.e("UpdateManager", "downloadAndInstall failed", e)
                        _updateState.value = UpdateState.Error(e.message ?: "Error descargando")
                        cont.resume(Result.failure(e))
                    }
                }
            })
        }
    }

    /**
     * Instala APK usando Intent (Android 8+ usa FileProvider)
     * MISMO keystore = datos preservados automáticamente
     */
    private fun installApk(file: File): Result<Unit> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                !ctx.packageManager.canRequestPackageInstalls()
            ) {
                val settingsIntent = Intent(
                    android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${ctx.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(settingsIntent)
                _updateState.value = UpdateState.Error(
                    "Permite instalar aplicaciones de esta fuente y vuelve a tocar Actualizar"
                )
                return Result.failure(IllegalStateException(
                    "Permite instalar aplicaciones de esta fuente y vuelve a tocar Actualizar"
                ))
            }

            val uri: Uri
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                uri = FileProvider.getUriForFile(
                    ctx,
                    "${ctx.packageName}.fileprovider",
                    file
                )
            } else {
                uri = Uri.fromFile(file)
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }

            // Verificar que hay actividad para manejar el intent
            val pm = ctx.packageManager
            if (intent.resolveActivity(pm) != null) {
                ctx.startActivity(intent)
                _updateState.value = UpdateState.Installing
                Result.success(Unit)
            } else {
                _updateState.value = UpdateState.Error("No hay instalador de paquetes")
                Result.failure(Exception("No package installer"))
            }
        } catch (e: Exception) {
            Log.e("UpdateManager", "installApk failed", e)
            _updateState.value = UpdateState.Error(e.message ?: "Error instalando")
            Result.failure(e)
        }
    }

    /**
     * In-app updates via Play Core (requiere Play Store instalado)
     * Útil si la app está en Play Store (internal/closed testing)
     * NOTA: Requiere dependencias adicionales de Play Core
     */
    fun startPlayStoreUpdate(activity: Activity, requestCode: Int = 1001) {
        // Play Core in-app updates - disabled for now due to API complexity
        // Use direct APK download from GitHub Releases instead
        Log.d("UpdateManager", "Play Store update not implemented - use direct download")
    }
    
    /**
     * Abre GitHub Releases en navegador (fallback manual)
     */
    fun openReleasesPage(activity: Activity) {
        val uri = Uri.parse("https://github.com/$repoOwner/$repoName/releases")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        activity.startActivity(intent)
    }

    /**
     * Verifica si la app fue actualizada recientemente (para mostrar "novedades")
     */
    fun checkIfUpdated(): Boolean {
        val prefs = ctx.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)
        val lastVersionCode = prefs.getLong("last_version_code", 0)
        val current = getCurrentVersion().versionCode
        
        if (current > lastVersionCode) {
            prefs.edit().putLong("last_version_code", current).apply()
            return true
        }
        return false
    }

    fun clearUpdateState() {
        _updateState.value = UpdateState.Idle
    }
}

// ===== Data Classes =====

data class VersionInfo(
    val versionCode: Long,
    val versionName: String
)

sealed interface UpdateState {
    object Idle : UpdateState
    object Checking : UpdateState
    object UpToDate : UpdateState
    data class UpdateAvailable(val result: UpdateCheckResult) : UpdateState
    data class Downloading(val progress: Float) : UpdateState
    data class Downloaded(val file: File) : UpdateState
    object Installing : UpdateState
    object Installed : UpdateState
    data class Error(val message: String) : UpdateState
}

data class UpdateCheckResult(
    val hasUpdate: Boolean,
    val currentVersion: VersionInfo,
    val remoteVersion: VersionInfo,
    val releaseName: String,
    val releaseNotes: String,
    val apkUrl: String?,
    val apkSize: Long,
    val isPrerelease: Boolean
) {
    fun formattedSize(): String {
        return when {
            apkSize >= 1_000_000 -> "${(apkSize / 1_000_000.0).toString().take(4)} MB"
            apkSize >= 1_000 -> "${(apkSize / 1_000.0).toString().take(4)} KB"
            else -> "$apkSize bytes"
        }
    }
}