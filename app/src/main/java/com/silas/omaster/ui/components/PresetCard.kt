package com.silas.omaster.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silas.omaster.R
import com.silas.omaster.model.MasterPreset
import com.silas.omaster.ui.theme.CardBorderHighlight
import com.silas.omaster.ui.theme.CardBorderLight
import com.silas.omaster.ui.theme.DarkGray
import com.silas.omaster.ui.theme.MediumGray
import com.silas.omaster.util.PresetI18n
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.silas.omaster.util.perform

@Composable
fun PresetCard(
    preset: MasterPreset,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    showFavoriteButton: Boolean = false,
    showDeleteButton: Boolean = false,
    modifier: Modifier = Modifier,
    imageHeight: Int = 200
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    // 动态边框颜色 - 悬停时更亮的微光效果
    val borderColor = if (isPressed) {
        CardBorderHighlight.copy(alpha = 0.6f)
    } else {
        CardBorderLight.copy(alpha = 0.08f)
    }
    val borderWidth = if (isPressed) 1.5.dp else 0.5.dp

    // 卡片背景渐变 - 玻璃态效果
    val cardBackgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MediumGray.copy(alpha = 0.4f),
            DarkGray.copy(alpha = 0.85f),
            DarkGray.copy(alpha = 0.95f)
        ),
        startY = 0f,
        endY = Float.POSITIVE_INFINITY
    )

    // 顶部高光渐变
    val topHighlightBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.08f),
            Color.White.copy(alpha = 0.02f),
            Color.Transparent
        ),
        startY = 0f,
        endY = 80f
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                // 悬浮阴影效果
                shadowElevation = if (isPressed) 16f else 8f
                shape = RoundedCornerShape(16.dp)
                clip = true
            }
            .border(
                width = borderWidth,
                brush = Brush.linearGradient(
                    colors = listOf(
                        borderColor.copy(alpha = if (isPressed) 0.8f else 0.3f),
                        borderColor.copy(alpha = if (isPressed) 0.4f else 0.1f),
                        borderColor.copy(alpha = if (isPressed) 0.6f else 0.2f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        // 背景层 - 玻璃态渐变
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBackgroundBrush)
                .drawBehind {
                    // 顶部高光效果
                    drawRect(
                        brush = topHighlightBrush,
                        size = size.copy(height = 80f)
                    )
                }
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(imageHeight.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                ) {
                    PresetImage(
                        preset = preset,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (showFavoriteButton) {
                        IconButton(
                            onClick = {
                                haptic.perform(HapticFeedbackType.ToggleOn)
                                onFavoriteClick()
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(36.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        color = if (preset.isFavorite)
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        else
                                            Color.Black.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(14.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (preset.isFavorite)
                                        Icons.Filled.Favorite
                                    else
                                        Icons.Outlined.FavoriteBorder,
                                    contentDescription = if (preset.isFavorite) stringResource(R.string.preset_favorited) else stringResource(R.string.preset_favorite),
                                    tint = if (preset.isFavorite) MaterialTheme.colorScheme.primary else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    if (showDeleteButton && preset.isCustom) {
                        IconButton(
                            onClick = {
                                haptic.perform(HapticFeedbackType.Confirm)
                                onDeleteClick()
                            },
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .size(36.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        color = Color.Black.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(14.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = stringResource(R.string.preset_delete),
                                    tint = Color.Red,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    if (preset.isNew) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.tertiary
                                        )
                                    ),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.preset_new),
                                fontSize = 10.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = PresetI18n.getLocalizedPresetName(preset.name),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun PresetCardPlaceholder(
    modifier: Modifier = Modifier
) {
    // 占位卡片背景渐变
    val placeholderBrush = Brush.verticalGradient(
        colors = listOf(
            MediumGray.copy(alpha = 0.3f),
            DarkGray.copy(alpha = 0.7f)
        )
    )

    // 顶部微光
    val shimmerBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.05f),
            Color.Transparent
        ),
        startY = 0f,
        endY = 60f
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp)
            .graphicsLayer {
                shadowElevation = 4f
                shape = RoundedCornerShape(16.dp)
                clip = true
            }
            .border(
                width = 0.5.dp,
                color = CardBorderLight.copy(alpha = 0.05f),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(placeholderBrush)
                .drawBehind {
                    drawRect(
                        brush = shimmerBrush,
                        size = size.copy(height = 60f)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.empty_no_data),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.4f)
            )
        }
    }
}
