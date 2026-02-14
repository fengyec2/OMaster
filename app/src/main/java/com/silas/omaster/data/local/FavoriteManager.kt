package com.silas.omaster.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 【核心数据管理类 - 软件更新时数据保留的关键】
 * 收藏管理器 - 使用 SharedPreferences 存储收藏的预设 ID
 * 
 * 【重要】此文件管理的数据在以下场景会保留：
 * 1. App 版本更新（1.1.0 -> 1.1.1）
 * 2. App 覆盖安装
 * 3. 系统更新
 * 
 * 【警告】以下操作会导致数据丢失：
 * 1. 用户清除 App 数据
 * 2. 用户卸载 App 后重新安装
 * 3. 修改 PREFS_NAME 或 KEY_FAVORITES 常量值
 * 
 * 【数据关联】
 * 收藏数据通过 presetId 与自定义预设关联
 * 如果自定义预设被删除，对应的收藏记录会自动清理
 */
class FavoriteManager(context: Context) {

    /**
     * 【关键常量 - 不可修改】
     * SharedPreferences 文件名
     * 修改此值会导致现有用户收藏数据丢失
     * 位置：/data/data/com.silas.omaster/shared_prefs/omaster_prefs.xml
     */
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _favoritesFlow: MutableStateFlow<Set<String>>
    val favoritesFlow: StateFlow<Set<String>>

    init {
        // 【数据加载】必须使用 HashSet 创建副本
        // 因为 getStringSet 返回的是原始引用，可能被系统修改
        val initialFavorites = HashSet(prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet())
        android.util.Log.d("FavoriteManager", "Init loaded ${initialFavorites.size} favorites: $initialFavorites")
        _favoritesFlow = MutableStateFlow(initialFavorites)
        favoritesFlow = _favoritesFlow.asStateFlow()
    }

    /**
     * 获取所有收藏的预设 ID
     * 返回的是 Set<String>，每个元素是预设的 UUID
     */
    fun getFavorites(): Set<String> {
        return _favoritesFlow.value
    }

    /**
     * 检查预设是否已收藏
     * @param presetId 预设的唯一标识符
     */
    fun isFavorite(presetId: String): Boolean {
        return presetId in getFavorites()
    }

    /**
     * 【用户数据写入操作】
     * 切换收藏状态（收藏/取消收藏）
     * 数据会立即同步写入 SharedPreferences
     */
    fun toggleFavorite(presetId: String): Boolean {
        val favorites = getFavorites().toMutableSet()
        val isNowFavorite = if (presetId in favorites) {
            favorites.remove(presetId)
            false
        } else {
            favorites.add(presetId)
            true
        }
        android.util.Log.d("FavoriteManager", "Toggle $presetId -> $isNowFavorite, saving ${favorites.size} favorites")
        saveFavorites(favorites)
        return isNowFavorite
    }

    /**
     * 【用户数据写入操作】
     * 添加收藏
     */
    fun addFavorite(presetId: String) {
        val favorites = getFavorites().toMutableSet()
        favorites.add(presetId)
        saveFavorites(favorites)
    }

    /**
     * 【用户数据写入操作】
     * 移除收藏
     * 通常在删除预设时调用，清理关联的收藏记录
     */
    fun removeFavorite(presetId: String) {
        val favorites = getFavorites().toMutableSet()
        favorites.remove(presetId)
        saveFavorites(favorites)
    }

    /**
     * 【危险操作】
     * 清空所有收藏
     * 此操作会永久删除所有收藏数据，谨慎使用
     */
    fun clearFavorites() {
        prefs.edit().remove(KEY_FAVORITES).apply()
        _favoritesFlow.value = emptySet()
    }

    /**
     * 【数据保存 - 核心方法】
     * 保存收藏数据到 SharedPreferences
     * 
     * 【关键逻辑】
     * 1. 创建新的 HashSet 保存，避免引用问题
     * 2. 使用 commit() 同步保存，确保数据立即写入
     * 3. 保存后验证数据是否正确写入
     * 
     * 【存储格式】
     * - 键名：favorite_presets
     * - 值：Set<String>（预设 ID 集合）
     * - 示例：["550e8400-e29b-41d4-a716-446655440000", "6ba7b810-9dad-11d1-80b4-00c04fd430c8"]
     */
    private fun saveFavorites(favorites: Set<String>) {
        // 创建新的 HashSet 保存，避免引用问题
        val newSet = HashSet(favorites)
        android.util.Log.d("FavoriteManager", "Saving to SharedPreferences: $newSet")
        // 使用 commit() 同步保存，确保数据立即写入（不是 apply() 的异步）
        val success = prefs.edit().putStringSet(KEY_FAVORITES, newSet).commit()
        android.util.Log.d("FavoriteManager", "Commit result: $success")
        _favoritesFlow.value = newSet
        
        // 验证保存结果
        val saved = prefs.getStringSet(KEY_FAVORITES, emptySet())
        android.util.Log.d("FavoriteManager", "Verified saved to SharedPreferences: $saved")
    }

    companion object {
        /**
         * 【关键常量 - 绝对不可修改】
         * SharedPreferences 文件名
         * 修改此值会导致所有用户的收藏数据丢失
         */
        private const val PREFS_NAME = "omaster_prefs"
        
        /**
         * 【关键常量 - 绝对不可修改】
         * SharedPreferences 中存储收藏数据的键名
         * 修改此值会导致无法读取现有收藏数据
         */
        private const val KEY_FAVORITES = "favorite_presets"

        @Volatile
        private var INSTANCE: FavoriteManager? = null

        /**
         * 单例获取方法
         * 使用 applicationContext 避免内存泄漏
         */
        fun getInstance(context: Context): FavoriteManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FavoriteManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
