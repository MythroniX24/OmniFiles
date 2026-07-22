package com.omnilabs.omfiles.preview

import android.app.Application
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.omnilabs.omfiles.archive.ArchiveEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

sealed class PreviewUiState {
    data object Loading : PreviewUiState()
    data class Ready(
        val path: String,
        val previewType: PreviewType,
        val mimeType: String,
        val extension: String,
        val fileName: String
    ) : PreviewUiState()
    data class Error(val message: String) : PreviewUiState()
}

sealed class ArchivePreviewState {
    data object Loading : ArchivePreviewState()
    data class Entries(val entries: List<String>, val count: Int, val totalSize: Long) : ArchivePreviewState()
    data class Error(val message: String) : ArchivePreviewState()
}

data class ApkInfo(
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val targetSdk: Int,
    val minSdk: Int,
    val permissions: List<String>,
    val installStatus: String,
    val size: Long
)

@HiltViewModel
class PreviewViewModel @Inject constructor(
    application: Application,
    private val previewRegistry: PreviewRegistry,
    private val archiveEngine: ArchiveEngine
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<PreviewUiState>(PreviewUiState.Loading)
    val uiState: StateFlow<PreviewUiState> = _uiState.asStateFlow()

    private val _archiveState = MutableStateFlow<ArchivePreviewState>(ArchivePreviewState.Loading)
    val archiveState: StateFlow<ArchivePreviewState> = _archiveState.asStateFlow()

    private val _apkInfo = MutableStateFlow<ApkInfo?>(null)
    val apkInfo: StateFlow<ApkInfo?> = _apkInfo.asStateFlow()

    private val _textContent = MutableStateFlow<String?>(null)
    val textContent: StateFlow<String?> = _textContent.asStateFlow()

    private val _hexHeader = MutableStateFlow<String>("")
    val hexHeader: StateFlow<String> = _hexHeader.asStateFlow()

    fun load(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(path)
                if (!file.exists()) {
                    _uiState.value = PreviewUiState.Error("File not found")
                    return@launch
                }
                if (file.length() > 100_000_000 && isPreviewHeavy(file)) {
                    _uiState.value = PreviewUiState.Error("Large file: use external app to open")
                    return@launch
                }

                val fileInfo = com.omnilabs.omfiles.domain.model.FileInfo.fromFile(file)
                    ?: run {
                        _uiState.value = PreviewUiState.Error("Cannot read file")
                        return@launch
                    }
                val entry = previewRegistry.resolve(fileInfo)

                _uiState.value = PreviewUiState.Ready(
                    path = path,
                    previewType = entry.type,
                    mimeType = entry.mimeType,
                    extension = entry.actualExtension,
                    fileName = fileInfo.name
                )

                when (entry.type) {
                    PreviewType.ARCHIVE -> loadArchiveEntries(path)
                    PreviewType.APK -> loadApkInfo(path)
                    PreviewType.TEXT, PreviewType.CODE -> loadTextContent(path)
                    else -> loadHexHeader(path)
                }
            } catch (e: Exception) {
                _uiState.value = PreviewUiState.Error("Failed to load preview: ${e.message}")
            }
        }
    }

    private fun isPreviewHeavy(file: File): Boolean {
        val ext = file.extension.lowercase()
        return ext in setOf("mp4", "mkv", "avi", "mov", "webm", "mp3", "flac", "wav")
    }

    private suspend fun loadArchiveEntries(path: String) {
        _archiveState.value = ArchivePreviewState.Loading
        val result = archiveEngine.getArchiveEntries(path)
        _archiveState.value = when (result) {
            is ArchiveEngine.ArchiveResult.Success -> {
                val lines = result.message.split("\n").filter { it.isNotBlank() }
                ArchivePreviewState.Entries(lines, lines.size, File(path).length())
            }
            is ArchiveEngine.ArchiveResult.Error -> ArchivePreviewState.Error(result.message)
        }
    }

    private suspend fun loadApkInfo(path: String) {
        withContext(Dispatchers.IO) {
            try {
                val pm = getApplication<Application>().packageManager
                val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageArchiveInfo(path, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageArchiveInfo(path, 0)
                }

                val file = File(path)
                packageInfo?.applicationInfo?.let { appInfo ->
                    appInfo.sourceDir = path
                    appInfo.publicSourceDir = path
                }

                val permissions = packageInfo?.requestedPermissions?.toList() ?: emptyList()
                val packageName = packageInfo?.packageName ?: ""
                val versionName = packageInfo?.versionName ?: ""
                val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo?.longVersionCode ?: 0L
                } else {
                    @Suppress("DEPRECATION")
                    (packageInfo?.versionCode ?: 0).toLong()
                }
                val targetSdk = packageInfo?.applicationInfo?.targetSdkVersion ?: 0

                val installed = try {
                    pm.getPackageInfo(packageName, 0)
                    "Installed"
                } catch (_: Exception) {
                    "Not installed"
                }

                _apkInfo.value = ApkInfo(
                    packageName = packageName,
                    versionName = versionName,
                    versionCode = versionCode,
                    targetSdk = targetSdk,
                    minSdk = 0,
                    permissions = permissions,
                    installStatus = installed,
                    size = file.length()
                )
            } catch (e: Exception) {
                _apkInfo.value = null
            }
        }
    }

    private suspend fun loadTextContent(path: String) {
        withContext(Dispatchers.IO) {
            try {
                val file = File(path)
                val maxBytes = 1_000_000
                val bytesToRead = file.length().coerceAtMost(maxBytes.toLong()).toInt()
                val bytes = file.inputStream().use { it.readNBytes(bytesToRead) }
                val charset = java.nio.charset.Charset.defaultCharset()
                val content = String(bytes, charset)
                    .take(500_000)
                _textContent.value = content
            } catch (e: Exception) {
                _textContent.value = "Failed to read text: ${e.message}"
            }
        }
    }

    private suspend fun loadHexHeader(path: String) {
        withContext(Dispatchers.IO) {
            try {
                val bytes = File(path).inputStream().use { it.readNBytes(64) }
                _hexHeader.value = bytes.toHexString()
            } catch (e: Exception) {
                _hexHeader.value = ""
            }
        }
    }

    private fun ByteArray.toHexString(): String {
        val sb = StringBuilder()
        for (i in indices) {
            if (i > 0 && i % 16 == 0) sb.append("\n")
            sb.append(String.format("%02X ", this[i]))
        }
        return sb.toString().trim()
    }
}
