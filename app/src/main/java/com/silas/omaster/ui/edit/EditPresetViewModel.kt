package com.silas.omaster.ui.edit

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.silas.omaster.data.repository.PresetRepository
import com.silas.omaster.model.MasterPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class EditPresetViewModel(
    private val context: Context,
    private val repository: PresetRepository
) : ViewModel() {

    private val _preset = MutableStateFlow<MasterPreset?>(null)
    val preset: StateFlow<MasterPreset?> = _preset

    fun loadPreset(presetId: String) {
        viewModelScope.launch {
            val loadedPreset = repository.getPresetById(presetId)
            _preset.value = loadedPreset
        }
    }

    fun updatePreset(
        name: String,
        newImageUri: Uri?,
        mode: String,
        filter: String,
        softLight: String,
        tone: Int,
        saturation: Int,
        warmCool: Int,
        cyanMagenta: Int,
        sharpness: Int,
        vignette: String,
        exposure: Float?,
        colorTemperature: Float?,
        colorHue: Float?
    ): Boolean {
        val currentPreset = _preset.value ?: return false

        if (name.isBlank()) {
            android.util.Log.e("EditPresetViewModel", "更新预设失败：预设名称为空")
            return false
        }

        return try {
            val coverPath = if (newImageUri != null) {
                currentPreset.coverPath?.let { oldPath ->
                    deleteImageFile(oldPath)
                }
                saveImageToInternalStorage(newImageUri)
            } else {
                currentPreset.coverPath
            }

            val updatedPreset = currentPreset.copy(
                name = name,
                coverPath = coverPath ?: currentPreset.coverPath,
                mode = mode,
                filter = filter,
                exposureCompensation = exposure?.let { String.format("%.1f", it) },
                colorTemperature = colorTemperature?.toInt(),
                colorHue = colorHue?.toInt(),
                softLight = softLight,
                tone = tone,
                saturation = saturation,
                warmCool = warmCool,
                cyanMagenta = cyanMagenta,
                sharpness = sharpness,
                vignette = vignette
            )

            repository.updateCustomPreset(updatedPreset)
            _preset.value = updatedPreset
            true
        } catch (e: Exception) {
            android.util.Log.e("EditPresetViewModel", "更新预设失败", e)
            false
        }
    }

    private fun saveImageToInternalStorage(uri: Uri): String {
        val fileName = "custom_${System.currentTimeMillis()}.jpg"
        val file = File(context.filesDir, "presets/$fileName")

        file.parentFile?.mkdirs()

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(file).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        } ?: throw IOException("无法打开图片文件: $uri")

        if (!file.exists() || file.length() == 0L) {
            file.delete()
            throw IOException("图片文件保存失败或为空")
        }

        android.util.Log.d("EditPresetViewModel", "图片保存成功: ${file.absolutePath}")
        return "presets/$fileName"
    }

    private fun deleteImageFile(relativePath: String) {
        val file = File(context.filesDir, relativePath)
        if (file.exists()) {
            val deleted = file.delete()
            android.util.Log.d("EditPresetViewModel", "删除旧图片: $relativePath, 成功: $deleted")
        }
    }
}

class EditPresetViewModelFactory(
    private val context: Context,
    private val repository: PresetRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditPresetViewModel::class.java)) {
            return EditPresetViewModel(context, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
