package com.silas.omaster.ui.xposed

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.silas.omaster.data.repository.PresetRepository
import com.silas.omaster.xposed.FilterMapManager
import com.silas.omaster.xposed.RootManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Xposed 工具页 ViewModel
 * 职责: 环境检测 + 滤镜映射数据加载（诊断与参考）
 * 写入逻辑已移至 WriteFilterDialog（从 DetailScreen 调用）
 */
class XposedToolViewModel(
    context: Context,
    repository: PresetRepository
) : ViewModel() {

    private val rootManager = RootManager.getInstance()
    private val filterMapManager = FilterMapManager.getInstance(context)

    // ===== 环境状态 =====
    val rootStatus: StateFlow<RootManager.RootStatus> = rootManager.rootStatus
    val filterMap: StateFlow<List<FilterMapManager.FilterEntry>> = filterMapManager.filterMap
    val isFilterMapAvailable: StateFlow<Boolean> = filterMapManager.isAvailable
    val lastCaptureTime: StateFlow<Long> = filterMapManager.lastCaptureTime

    init {
        checkEnvironment()
    }

    /**
     * 检查运行环境（Root + Xposed 模块状态）
     */
    fun checkEnvironment() {
        viewModelScope.launch {
            rootManager.checkRoot()
            filterMapManager.loadFilterMap()
        }
    }

    // ===== 重启相机 =====
    private val _killCameraResult = MutableStateFlow<Boolean?>(null)
    val killCameraResult: StateFlow<Boolean?> = _killCameraResult

    /**
     * 强制杀死相机进程
     */
    fun killCamera() {
        viewModelScope.launch {
            _killCameraResult.value = rootManager.killCameraApp()
        }
    }

    fun clearKillResult() {
        _killCameraResult.value = null
    }
}

/**
 * ViewModel 工厂
 */
class XposedToolViewModelFactory(
    private val context: Context,
    private val repository: PresetRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return XposedToolViewModel(context, repository) as T
    }
}
