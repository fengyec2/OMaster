package com.silas.omaster.data.local

import android.content.Context
import android.content.SharedPreferences

/**
 * 【新预设标记管理器 - 简化版】
 * 
 * 【使用方式】
 * 1. 在 presets.json 中手动标记新增预设为 "isNew": true
 * 2. 发版前将旧版本的 "isNew" 改为 false（或删除该字段）
 * 
 * 【工作原理】
 * - 完全依赖 JSON 中的 isNew 字段
 * - 不需要版本号判断
 * - 手动控制哪些预设显示 NEW 标签
 */
class NewPresetManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "omaster_new_presets"

        @Volatile
        private var INSTANCE: NewPresetManager? = null

        fun getInstance(context: Context): NewPresetManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NewPresetManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
