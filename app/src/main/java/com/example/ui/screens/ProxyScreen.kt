package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProxyStatus
import com.example.ui.MainViewModel
import com.example.ui.theme.CfOrangePrimary
import com.example.ui.theme.CfOrangeSecondary
import com.example.ui.theme.DarkSlateCard
import com.example.ui.theme.MutedText
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.OffWhiteText
import com.example.ui.theme.PingGreenFast
import com.example.ui.theme.PingRedSlow

@Composable
fun ProxyScreen(
    viewModel: MainViewModel,
    proxyStatus: ProxyStatus
) {
    var localPortText by remember { mutableStateOf(proxyStatus.localPort.toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Power Switch Circle
        ProxyPowerButton(
            isRunning = proxyStatus.isRunning,
            onToggle = {
                val port = localPortText.toIntOrNull() ?: 1234
                viewModel.toggleProxy(port)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Status Banner Cards Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProxyStatCard(
                modifier = Modifier.weight(1f),
                title = "本地监听",
                value = if (proxyStatus.isRunning) "127.0.0.1:${proxyStatus.localPort}" else "离线",
                icon = Icons.Default.Router,
                tint = NeonCyan
            )

            ProxyStatCard(
                modifier = Modifier.weight(1f),
                title = "当前连接数",
                value = "${proxyStatus.activeConnections}",
                icon = Icons.Default.SwapHoriz,
                tint = CfOrangePrimary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Active Target IP Details Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "当前 Cloudflare 端点",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = CfOrangeSecondary
                        )
                    )

                    if (proxyStatus.activeColo.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CfOrangePrimary.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "数据中心: ${proxyStatus.activeColo}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = CfOrangePrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (proxyStatus.activeTargetIp.isNotEmpty()) proxyStatus.activeTargetIp else "无 (请扫描或选择一个 IP)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = OffWhiteText
                    )
                )

                if (proxyStatus.activeCity.isNotEmpty()) {
                    Text(
                        text = "位置：${proxyStatus.activeCity}",
                        style = MaterialTheme.typography.bodySmall.copy(color = MutedText)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "延迟：${if (proxyStatus.activeLatencyMs > 0) "${proxyStatus.activeLatencyMs} ms" else "检查中..."}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = if (proxyStatus.activeLatencyMs in 1..150) PingGreenFast else CfOrangeSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    val formattedBytes = formatBytes(proxyStatus.totalBytesTransferred)
                    Text(
                        text = "流量：$formattedBytes",
                        style = MaterialTheme.typography.labelMedium.copy(color = OffWhiteText)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Console Log Terminal Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "代理控制台日志",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = OffWhiteText
                )
            )

            Text(
                text = "IP池：${proxyStatus.targetPoolSize} 个",
                style = MaterialTheme.typography.labelSmall.copy(color = MutedText)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Terminal Log Console Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
        ) {
            if (proxyStatus.logMessages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "控制台就绪。启动代理查看实时流量日志。",
                        style = MaterialTheme.typography.bodySmall.copy(color = MutedText)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    reverseLayout = true
                ) {
                    items(proxyStatus.logMessages.reversed()) { log ->
                        Text(
                            text = log,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = if (log.contains("Error") || log.contains("failed")) PingRedSlow else PingGreenFast
                            ),
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProxyPowerButton(
    isRunning: Boolean,
    onToggle: () -> Unit
) {
    val buttonBg by animateColorAsState(
        targetValue = if (isRunning) PingGreenFast else DarkSlateCard,
        label = "PowerBg"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(buttonBg)
                .border(4.dp, if (isRunning) CfOrangePrimary else MutedText, CircleShape)
                .testTag("proxy_power_button"),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onToggle,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = "Power Proxy",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isRunning) "代理运行中 & 负载均衡" else "点击启动代理转发",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = if (isRunning) PingGreenFast else MutedText
            )
        )
    }
}

@Composable
fun ProxyStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(color = MutedText)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = OffWhiteText
                    )
                )
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
    val pre = "KMGTPE"[exp - 1]
    return String.format("%.1f %sB", bytes / Math.pow(1024.0, exp.toDouble()), pre)
}
