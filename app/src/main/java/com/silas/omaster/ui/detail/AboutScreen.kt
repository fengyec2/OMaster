package com.silas.omaster.ui.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.silas.omaster.ui.components.OMasterTopAppBar
import com.silas.omaster.ui.theme.DarkGray
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.util.UpdateChecker
import com.silas.omaster.util.VersionInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onScrollStateChanged: (Boolean) -> Unit,
    currentVersionCode: Int = VersionInfo.VERSION_CODE,
    currentVersionName: String = VersionInfo.VERSION_NAME
) {
    val scrollState = rememberScrollState()
    var previousScrollValue by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isScrollingUp by remember {
        derivedStateOf {
            val currentScroll = scrollState.value
            val isUp = currentScroll <= previousScrollValue
            previousScrollValue = currentScroll
            isUp
        }
    }

    // 更新检查状态
    var isChecking by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateChecker.UpdateInfo?>(null) }
    var checkError by remember { mutableStateOf<String?>(null) }
    var lastCheckTime by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(isScrollingUp) {
        onScrollStateChanged(isScrollingUp)
    }

    // 自动检查更新（进入页面时）
    LaunchedEffect(Unit) {
        delay(500) // 延迟500ms，让页面先渲染完成
        if (updateInfo == null && checkError == null) {
            isChecking = true
            checkError = null
            try {
                val result = UpdateChecker.checkUpdate(currentVersionCode)
                if (result != null) {
                    updateInfo = result
                    lastCheckTime = System.currentTimeMillis()
                } else {
                    checkError = "无法获取更新信息"
                }
            } catch (e: Exception) {
                checkError = e.message ?: "未知错误"
            } finally {
                isChecking = false
            }
        }
    }

    // 检查更新的函数
    val checkForUpdate = {
        scope.launch {
            isChecking = true
            checkError = null
            try {
                val result = UpdateChecker.checkUpdate(currentVersionCode)
                if (result != null) {
                    updateInfo = result
                    lastCheckTime = System.currentTimeMillis()
                } else {
                    checkError = "无法获取更新信息"
                }
            } catch (e: Exception) {
                checkError = e.message ?: "未知错误"
            } finally {
                isChecking = false
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        OMasterTopAppBar(
            title = "关于",
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 应用 Logo 和名称
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = HasselbladOrange)) {
                        append("O")
                    }
                    withStyle(style = SpanStyle(color = Color.White)) {
                        append("Master")
                    }
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "大师模式调色参数库 v$currentVersionName",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 检查更新卡片 - 优化版
            UpdateCard(
                currentVersionName = currentVersionName,
                isChecking = isChecking,
                updateInfo = updateInfo,
                checkError = checkError,
                lastCheckTime = lastCheckTime,
                onCheckClick = { checkForUpdate() },
                onDownloadClick = { 
                    // 【修改】使用 UpdateInfo 中的下载链接（国内链接优先）
                    updateInfo?.let { info ->
                        UpdateChecker.openDownloadPage(context, info.downloadUrl)
                    }
                },
                onRetryClick = {
                    checkError = null
                    checkForUpdate()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 功能介绍卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = DarkGray
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "功能介绍",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = HasselbladOrange
                    )
                    Text(
                        text = "OMaster 是一款专为OPPO/一加/Realme手机摄影爱好者设计的调色参数管理工具。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Start
                    )
                    Text(
                        text = "您可以在这里查看、收藏和复现大师模式的摄影调色参数，包括曝光、对比度、饱和度、色温等专业参数。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Start
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 制作信息卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = DarkGray
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "制作信息",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = HasselbladOrange
                    )
                    Text(
                        text = "开发者：Silas",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "素材提供：",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "@OPPO影像",
                            style = MaterialTheme.typography.bodyMedium,
                            color = HasselbladOrange,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier.clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://xhslink.com/m/8c2gJYGlCTR"))
                                context.startActivity(intent)
                            }
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "                    ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "@蘭州白鴿",
                            style = MaterialTheme.typography.bodyMedium,
                            color = HasselbladOrange,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier.clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://xhslink.com/m/4h5lx4Lg37n"))
                                context.startActivity(intent)
                            }
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "                    ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "@派瑞特凯",
                            style = MaterialTheme.typography.bodyMedium,
                            color = HasselbladOrange,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier.clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://xhslink.com/m/AkrgUI0kgg1"))
                                context.startActivity(intent)
                            }
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "                    ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "@ONESTEP™",
                            style = MaterialTheme.typography.bodyMedium,
                            color = HasselbladOrange,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier.clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://xhslink.com/m/4LZ8zRdNCSv"))
                                context.startActivity(intent)
                            }
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "                    ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "@盒子叔",
                            style = MaterialTheme.typography.bodyMedium,
                            color = HasselbladOrange,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier.clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://xhslink.com/m/4mje9mimNXJ"))
                                context.startActivity(intent)
                            }
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "                    ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "@Aurora",
                            style = MaterialTheme.typography.bodyMedium,
                            color = HasselbladOrange,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier.clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://xhslink.com/m/2Ebow4iyVOE"))
                                context.startActivity(intent)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "纯本地化运作，数据存储在本地",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )

                    Text(
                        text = "查看《用户协议和隐私政策》",
                        style = MaterialTheme.typography.bodySmall,
                        color = HasselbladOrange,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.umeng.com/page/policy"))
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun UpdateCard(
    currentVersionName: String,
    isChecking: Boolean,
    updateInfo: UpdateChecker.UpdateInfo?,
    checkError: String?,
    lastCheckTime: Long?,
    onCheckClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onRetryClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = DarkGray
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "版本更新",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = HasselbladOrange
                )

                // 刷新按钮
                if (!isChecking) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "刷新",
                        tint = HasselbladOrange,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { onCheckClick() }
                    )
                }
            }

            // 当前版本
            Text(
                text = "当前版本：v$currentVersionName",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f)
            )

            // 状态显示区域
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                when {
                    isChecking -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = HasselbladOrange,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "正在检查更新...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                    updateInfo != null -> {
                        if (updateInfo.isNewer) {
                            // 有新版本
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null,
                                        tint = HasselbladOrange,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "发现新版本 v${updateInfo.versionName}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = HasselbladOrange,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // 更新日志
                                if (updateInfo.releaseNotes.isNotBlank()) {
                                    Text(
                                        text = updateInfo.releaseNotes.take(150) +
                                                if (updateInfo.releaseNotes.length > 150) "..." else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.7f),
                                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.2
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                }

                                // 下载按钮（使用国内链接）
                                Button(
                                    onClick = onDownloadClick,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = HasselbladOrange
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("国内下载")
                                }
                            }
                        } else {
                            // 已是最新
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "当前已是最新版本",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                    checkError != null -> {
                        // 检查失败
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFFF5252),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "检查失败",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFFF5252)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = checkError,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "点击重试",
                                style = MaterialTheme.typography.bodySmall,
                                color = HasselbladOrange,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier.clickable { onRetryClick() }
                            )
                        }
                    }
                    else -> {
                        // 初始状态
                        Text(
                            text = "点击检查更新",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.clickable { onCheckClick() }
                        )
                    }
                }
            }

            // 最后检查时间
            AnimatedVisibility(
                visible = lastCheckTime != null && !isChecking,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                lastCheckTime?.let { time ->
                    val timeText = remember(time) {
                        val diff = System.currentTimeMillis() - time
                        when {
                            diff < 60000 -> "刚刚"
                            diff < 3600000 -> "${diff / 60000}分钟前"
                            diff < 86400000 -> "${diff / 3600000}小时前"
                            else -> "${diff / 86400000}天前"
                        }
                    }
                    Text(
                        text = "最后检查：$timeText",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}
