package com.silas.omaster.xposed

import android.content.Context
import android.util.Log
import com.tencent.mmkv.MMKV

/**
 * MMKV 参数读写器
 * 在 OMaster 进程中操作拷贝到临时目录的 MMKV 文件
 *
 * 工作流程:
 * 1. RootManager 将相机 MMKV 文件拷贝到临时目录
 * 2. 本类使用 MMKV 库打开临时目录中的文件
 * 3. 读取/写入参数
 * 4. RootManager 将修改后的文件写回相机目录
 */
class MmkvParamWriter(private val context: Context) {

    private var initialized = false

    /**
     * 初始化 MMKV 库
     * 注意：MMKV.initialize 只能调用一次
     */
    private fun ensureInitialized() {
        if (!initialized) {
            MMKV.initialize(context)
            initialized = true
        }
    }

    /**
     * 写入参数到指定 MMKV 文件
     *
     * @param tempDir 临时 MMKV 目录路径（拷贝自相机 data 目录）
     * @param fileName MMKV 文件名:
     *   - "com.oplus.camera_preferences_0" (索引 0 参数)
     *   - "mmkv" (索引 ≥1 参数)
     * @param params 要写入的键值对 (key: MMKV key, value: Int)
     * @return 是否成功
     */
    fun writeParams(
        tempDir: String,
        fileName: String,
        params: Map<String, Int>
    ): Boolean {
        return try {
            ensureInitialized()

            // 使用自定义 rootPath 打开指定目录下的 MMKV 文件
            val mmkv = MMKV.mmkvWithID(
                fileName,
                MMKV.SINGLE_PROCESS_MODE,
                null,    // 无加密
                tempDir  // 自定义根目录
            )

            // 写入所有参数
            params.forEach { (key, value) ->
                mmkv.encode(key, value)
                Log.d(TAG, "写入: $key = $value")
            }

            // 关闭 MMKV 实例，触发 munmap 确保 mmap 数据刷入物理文件
            // 必须在 cp 写回之前调用，否则 cp 可能读到未刷盘的旧数据
            mmkv.close()
            Log.d(TAG, "参数写入成功: $fileName, 共 ${params.size} 个参数")
            true
        } catch (e: Exception) {
            Log.e(TAG, "MMKV 写入失败: $fileName", e)
            false
        }
    }

    /**
     * 读取 MMKV 中指定 key 的当前值
     * 用于写入前预览当前相机设置
     *
     * @param tempDir 临时 MMKV 目录
     * @param fileName MMKV 文件名
     * @param keys 要读取的 key 列表
     * @param defaultValue 默认值
     * @return Map<String, Int> key 到当前值的映射
     */
    fun readParams(
        tempDir: String,
        fileName: String,
        keys: List<String>,
        defaultValue: Int = 0
    ): Map<String, Int> {
        return try {
            ensureInitialized()

            val mmkv = MMKV.mmkvWithID(
                fileName,
                MMKV.SINGLE_PROCESS_MODE,
                null,
                tempDir
            )

            keys.associateWith { key ->
                mmkv.decodeInt(key, defaultValue)
            }
        } catch (e: Exception) {
            Log.e(TAG, "MMKV 读取失败: $fileName", e)
            emptyMap()
        }
    }

    /**
     * 获取 MMKV 文件中所有 key（调试用）
     */
    fun getAllKeys(tempDir: String, fileName: String): Array<String> {
        return try {
            ensureInitialized()
            val mmkv = MMKV.mmkvWithID(
                fileName,
                MMKV.SINGLE_PROCESS_MODE,
                null,
                tempDir
            )

            mmkv.allKeys() ?: emptyArray()
        } catch (e: Exception) {
            Log.e(TAG, "获取 MMKV keys 失败", e)
            emptyArray()
        }
    }

    companion object {
        private const val TAG = "OMaster-MmkvWriter"
    }
}
