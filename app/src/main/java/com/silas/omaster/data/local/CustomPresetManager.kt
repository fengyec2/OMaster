package com.silas.omaster.data.local

import android.content.Context
import android.content.SharedPreferences
import com.silas.omaster.model.MasterPreset
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.util.UUID

/**
 * 【核心数据管理类 - 软件更新时数据保留的关键】
 * 自定义预设管理器 - 使用 SharedPreferences 存储用户创建的预设
 * 
 * 【重要】此文件管理的数据在以下场景会保留：
 * 1. App 版本更新（1.1.0 -> 1.1.1）
 * 2. App 覆盖安装
 * 3. 系统更新
 * 
 * 【警告】以下操作会导致数据丢失：
 * 1. 用户清除 App 数据
 * 2. 用户卸载 App 后重新安装
 * 3. 修改 PREFS_NAME 或 KEY_CUSTOM_PRESETS 常量值
 */
class CustomPresetManager(context: Context) {

    /**
     * 【关键常量 - 不可修改】
     * SharedPreferences 文件名
     * 修改此值会导致现有用户数据无法读取（视为新用户）
     * 位置：/data/data/com.silas.omaster/shared_prefs/omaster_custom_presets.xml
     */
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    private val gson = Gson()

    private val _customPresetsFlow = MutableStateFlow<List<MasterPreset>>(emptyList())
    val customPresetsFlow: StateFlow<List<MasterPreset>> = _customPresetsFlow

    init {
        // 启动时从 SharedPreferences 加载用户数据
        loadCustomPresets()
    }

    /**
     * 获取所有自定义预设（内存中的缓存）
     */
    fun getCustomPresets(): List<MasterPreset> {
        return _customPresetsFlow.value
    }

    /**
     * 【用户数据写入操作】
     * 添加新的自定义预设
     * 数据会立即写入 SharedPreferences 持久化保存
     */
    fun addCustomPreset(preset: MasterPreset) {
        val presets = getCustomPresets().toMutableList()
        val newPreset = preset.copy(
            id = preset.id ?: UUID.randomUUID().toString(),
            isCustom = true
        )
        presets.add(0, newPreset) // 添加到开头（最新的在前面）
        saveCustomPresets(presets)
    }

    /**
     * 【用户数据写入操作】
     * 更新已有自定义预设
     * 通过 preset.id 匹配，保留原有 ID 确保数据连续性
     */
    fun updateCustomPreset(preset: MasterPreset) {
        val presets = getCustomPresets().toMutableList()
        val index = presets.indexOfFirst { it.id == preset.id }
        if (index != -1) {
            presets[index] = preset.copy(isCustom = true)
            saveCustomPresets(presets)
        }
    }

    /**
     * 【用户数据删除操作】
     * 删除自定义预设及其关联图片文件
     * 注意：此操作会永久删除数据，无法恢复
     */
    fun deleteCustomPreset(context: Context, presetId: String) {
        // 先获取要删除的预设，以便删除其图片文件
        val presetToDelete = getCustomPresets().find { it.id == presetId }
        
        // 从列表中移除
        val presets = getCustomPresets().filter { it.id != presetId }
        saveCustomPresets(presets)
        
        // 删除关联的图片文件（内部存储中的文件）
        presetToDelete?.let { preset ->
            deletePresetImages(context, preset)
        }
    }
    
    /**
     * 【内部存储文件管理】
     * 删除预设关联的图片文件
     * 位置：/data/data/com.silas.omaster/files/presets/
     * 这些文件在 App 更新时会保留，但卸载时会删除
     */
    private fun deletePresetImages(context: Context, preset: MasterPreset) {
        // 删除封面图片
        preset.coverPath?.let { coverPath ->
            val coverFile = File(context.filesDir, coverPath)
            if (coverFile.exists()) {
                val deleted = coverFile.delete()
                android.util.Log.d("CustomPresetManager", "Deleted cover image: $coverPath, success: $deleted")
            }
        }
        
        // 删除画廊图片
        preset.galleryImages?.forEach { galleryPath ->
            val galleryFile = File(context.filesDir, galleryPath)
            if (galleryFile.exists()) {
                val deleted = galleryFile.delete()
                android.util.Log.d("CustomPresetManager", "Deleted gallery image: $galleryPath, success: $deleted")
            }
        }
    }

    /**
     * 根据 ID 获取预设
     */
    fun getPresetById(presetId: String): MasterPreset? {
        return getCustomPresets().find { it.id == presetId }
    }

    /**
     * 【数据加载 - 核心方法】
     * 从 SharedPreferences 加载用户自定义预设
     * 
     * 【关键逻辑】
     * 1. 读取 KEY_CUSTOM_PRESETS 键对应的 JSON 字符串
     * 2. 使用 Gson 解析为 MasterPreset 对象列表
     * 3. 如果解析失败，返回空列表（不会崩溃）
     * 
     * 【兼容性考虑】
     * - 如果用户从旧版本升级，JSON 结构可能缺少新字段
     * - Gson 会自动将缺失字段设为 null 或默认值
     * - 新增字段需要有默认值（在 MasterPreset 中定义）
     */
    private fun loadCustomPresets() {
        val json = prefs.getString(KEY_CUSTOM_PRESETS, null)
        val presets = if (json != null) {
            try {
                val type = object : TypeToken<List<MasterPreset>>() {}.type
                gson.fromJson<List<MasterPreset>>(json, type) ?: emptyList()
            } catch (e: Exception) {
                // 【错误处理】如果 JSON 损坏，记录错误并返回空列表
                // 这会导致用户数据丢失，但 App 不会崩溃
                android.util.Log.e("CustomPresetManager", "加载自定义预设失败", e)
                emptyList()
            }
        } else {
            // 新用户或首次使用，返回空列表
            emptyList()
        }
        _customPresetsFlow.value = presets
    }

    /**
     * 【数据保存 - 核心方法】
     * 将自定义预设列表保存到 SharedPreferences
     * 
     * 【存储格式】
     * - 键名：custom_presets
     * - 值：JSON 数组字符串
     * - 示例：[{"id":"xxx","name":"预设1",...}, {...}]
     * 
     * 【性能注意】
     * - 使用 apply() 异步保存（不会阻塞 UI）
     * - 数据量大时可能会有短暂延迟
     */
    private fun saveCustomPresets(presets: List<MasterPreset>) {
        val json = gson.toJson(presets)
        prefs.edit().putString(KEY_CUSTOM_PRESETS, json).apply()
        _customPresetsFlow.value = presets
    }

    companion object {
        /**
         * 【关键常量 - 绝对不可修改】
         * SharedPreferences 文件名
         * 修改此值会导致：
         * 1. 所有现有用户的自定义预设消失
         * 2. 收藏数据与预设失联
         * 3. 用户数据无法恢复
         */
        private const val PREFS_NAME = "omaster_custom_presets"
        
        /**
         * 【关键常量 - 绝对不可修改】
         * SharedPreferences 中存储预设数据的键名
         * 修改此值会导致无法读取现有数据
         */
        private const val KEY_CUSTOM_PRESETS = "custom_presets"

        @Volatile
        private var INSTANCE: CustomPresetManager? = null

        /**
         * 单例获取方法
         * 使用 applicationContext 避免内存泄漏
         */
        fun getInstance(context: Context): CustomPresetManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CustomPresetManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
