package com.silas.omaster.util

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * 全局震感开关
 * 日后可在设置中修改
 */
object HapticSettings {
    var enabled: Boolean = true
}

/**
 * 执行震感反馈
 */
fun HapticFeedback.perform(type: HapticFeedbackType) {
    if (HapticSettings.enabled) {
        performHapticFeedback(type)
    }
}

/**
 * 带震感反馈的点击
 */
fun Modifier.hapticClickable(
    type: HapticFeedbackType = HapticFeedbackType.TextHandleMove,
    enabled: Boolean = true,
    onClick: () -> Unit
) = composed {
    val haptic = LocalHapticFeedback.current
    clickable(enabled = enabled) {
        haptic.perform(type)
        onClick()
    }
}
