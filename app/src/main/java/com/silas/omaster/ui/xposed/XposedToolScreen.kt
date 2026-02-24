package com.silas.omaster.ui.xposed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.silas.omaster.R
import com.silas.omaster.data.repository.PresetRepository
import com.silas.omaster.ui.theme.DarkGray
import com.silas.omaster.ui.theme.NearBlack
import com.silas.omaster.ui.theme.PureBlack
import com.silas.omaster.xposed.FilterMapManager
import com.silas.omaster.xposed.RootManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Xposed 工具页面（诊断与参考）
 * 功能: 环境检测 + 滤镜索引对照表
 * 实际写入操作在 DetailScreen → WriteFilterDialog 中完成
 */
@Composable
fun XposedToolScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { PresetRepository.getInstance(context) }
    val viewModel: XposedToolViewModel = viewModel(
        factory = XposedToolViewModelFactory(context, repository)
    )

    val rootStatus by viewModel.rootStatus.collectAsState()
    val filterMap by viewModel.filterMap.collectAsState()
    val isFilterMapAvailable by viewModel.isFilterMapAvailable.collectAsState()
    val lastCaptureTime by viewModel.lastCaptureTime.collectAsState()
    val killCameraResult by viewModel.killCameraResult.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // 顶部导航栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = Color.White
                )
            }
            Text(
                text = stringResource(R.string.xposed_tool_title),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ===== 环境状态卡片 =====
            item {
                EnvironmentStatusCard(
                    rootStatus = rootStatus,
                    isFilterMapAvailable = isFilterMapAvailable,
                    filterCount = filterMap.size,
                    lastCaptureTime = lastCaptureTime,
                    onRefresh = { viewModel.checkEnvironment() }
                )
            }

            // ===== 重启相机 =====
            item {
                KillCameraCard(
                    rootAvailable = rootStatus == RootManager.RootStatus.Available,
                    killResult = killCameraResult,
                    onKill = { viewModel.killCamera() },
                    onClearResult = { viewModel.clearKillResult() }
                )
            }

            // ===== 使用说明 =====
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NearBlack),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.xposed_usage_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.xposed_usage_steps),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // ===== 滤镜索引对照表标题 =====
            item {
                AnimatedVisibility(
                    visible = filterMap.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = stringResource(R.string.xposed_filter_table_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
            }

            // 滤镜表格
            if (filterMap.isNotEmpty()) {
                item { FilterTableHeader() }
                items(filterMap) { entry ->
                    FilterTableRow(entry = entry)
                }
            }

            // 底部间距
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

// ===== 子组件 =====

@Composable
private fun EnvironmentStatusCard(
    rootStatus: RootManager.RootStatus,
    isFilterMapAvailable: Boolean,
    filterCount: Int,
    lastCaptureTime: Long,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NearBlack),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.xposed_env_status),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onRefresh, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh), tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Root 状态
            StatusRow(
                label = "Root",
                status = when (rootStatus) {
                    RootManager.RootStatus.Available -> StatusType.OK
                    RootManager.RootStatus.Unknown -> StatusType.LOADING
                    else -> StatusType.ERROR
                },
                detail = when (rootStatus) {
                    RootManager.RootStatus.Available -> stringResource(R.string.xposed_root_granted)
                    RootManager.RootStatus.Denied -> stringResource(R.string.xposed_root_denied)
                    RootManager.RootStatus.Unavailable -> stringResource(R.string.xposed_root_unavailable)
                    RootManager.RootStatus.Unknown -> stringResource(R.string.checking)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Xposed 模块状态
            StatusRow(
                label = "Xposed",
                status = if (isFilterMapAvailable) StatusType.OK else StatusType.ERROR,
                detail = if (isFilterMapAvailable) {
                    stringResource(R.string.xposed_module_active, filterCount)
                } else {
                    stringResource(R.string.xposed_module_inactive)
                }
            )

            // 操作引导提示
            if (!isFilterMapAvailable) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.xposed_hint_open_camera),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFFB300),
                    modifier = Modifier.padding(start = 28.dp)
                )
            }

            // 最后捕获时间
            if (lastCaptureTime > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(Date(lastCaptureTime))
                Text(
                    text = stringResource(R.string.xposed_last_capture, dateStr),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 28.dp)
                )
            }
        }
    }
}

private enum class StatusType { OK, ERROR, LOADING }

@Composable
private fun StatusRow(label: String, status: StatusType, detail: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        when (status) {
            StatusType.OK -> Icon(
                Icons.Default.Check, contentDescription = null,
                tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp)
            )
            StatusType.ERROR -> Icon(
                Icons.Default.Close, contentDescription = null,
                tint = Color(0xFFE53935), modifier = Modifier.size(20.dp)
            )
            StatusType.LOADING -> CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = Color.Gray
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "$label: ", color = Color.White, fontWeight = FontWeight.Medium)
        Text(text = detail, color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
    }
}

// ===== 滤镜对照表组件 =====

@Composable
private fun FilterTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkGray, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text("#", color = Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp), fontSize = 13.sp)
        Text(stringResource(R.string.xposed_col_name), color = Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), fontSize = 13.sp)
        Text(stringResource(R.string.xposed_col_lut), color = Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f), fontSize = 13.sp)
    }
}

@Composable
private fun FilterTableRow(entry: FilterMapManager.FilterEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${entry.index}",
            color = MaterialTheme.colorScheme.primary,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(36.dp),
            fontSize = 13.sp
        )
        Text(
            text = entry.name,
            color = Color.White,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = entry.lutFile,
            color = Color.Gray,
            modifier = Modifier.weight(1.5f),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ===== 重启相机卡片 =====

@Composable
private fun KillCameraCard(
    rootAvailable: Boolean,
    killResult: Boolean?,
    onKill: () -> Unit,
    onClearResult: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NearBlack),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = rootAvailable) {
                    onClearResult()
                    onKill()
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.xposed_kill_camera),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (rootAvailable) Color.White else Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.xposed_kill_camera_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            // 结果反馈
            killResult?.let { success ->
                Icon(
                    imageVector = if (success) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    tint = if (success) Color(0xFF4CAF50) else Color(0xFFE53935),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
