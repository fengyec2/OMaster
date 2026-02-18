package com.silas.omaster.data.local

import android.content.Context
import android.content.SharedPreferences

/**
 * 悬浮窗引导管理器
 * 用于记录用户是否已看过悬浮窗使用引导
 */
class FloatingWindowGuideManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    /**
     * 检查是否是首次使用悬浮窗功能
     */
    fun isFirstTimeUseFloatingWindow(): Boolean {
        return prefs.getBoolean(KEY_FIRST_TIME_FLOATING_WINDOW, true)
    }

    /**
     * 标记已显示过悬浮窗引导
     */
    fun markGuideShown() {
        prefs.edit().putBoolean(KEY_FIRST_TIME_FLOATING_WINDOW, false).apply()
    }

    /**
     * 重置引导状态（用于测试）
     */
    fun resetGuideStatus() {
        prefs.edit().putBoolean(KEY_FIRST_TIME_FLOATING_WINDOW, true).apply()
    }

    companion object {
        private const val PREFS_NAME = "omaster_guide_prefs"
        private const val KEY_FIRST_TIME_FLOATING_WINDOW = "first_time_floating_window"

        @Volatile
        private var INSTANCE: FloatingWindowGuideManager? = null

        fun getInstance(context: Context): FloatingWindowGuideManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FloatingWindowGuideManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
