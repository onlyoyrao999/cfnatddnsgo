package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ScannedIp
import com.example.data.model.ScannedIpEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.CfOrangePrimary
import com.example.ui.theme.CfOrangeSecondary
import com.example.ui.theme.MutedText
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.OffWhiteText
import com.example.ui.theme.PingGreenFast
import com.example.ui.theme.PingRedSlow
import com.example.ui.theme.PingYellowMedium

@Composable
fun SavedIpScreen(
    viewModel: MainViewModel,
    savedIps: List<ScannedIpEntity>
) {
    val context = LocalContext.current
    var filterFavoritesOnly by remember { mutableStateOf(false) }

    val displayedIps = if (filterFavoritesOnly) {
        savedIps.filter { it.isFavorite }
    } else savedIps

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Action Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = "Database",
                    tint = CfOrangePrimary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "已保存的 IP 仓库",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = OffWhiteText
                        )
                    )
                    Text(
                        text = "${displayedIps.size} 个 IP 已存入本地数据库",
                        style = MaterialTheme.typography.bodySmall.copy(color = MutedText)
                    )
                }
            }

            if (savedIps.isNotEmpty()) {
                IconButton(
                    onClick = { viewModel.clearSavedIps() },
                    modifier = Modifier.testTag("clear_all_saved_ips")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "清除全部",
                        tint = PingRedSlow
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Export Actions Bar
        if (savedIps.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val ipList = displayedIps.map { it.ip }
                        viewModel.copyIpsToClipboard(context, ipList, "Cloudflare Raw IPs")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("export_raw_ips"),
                    colors = ButtonDefaults.buttonColors(containerColor = CfOrangePrimary)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("复制 IP")
                }

                OutlinedButton(
                    onClick = {
                        filterFavoritesOnly = !filterFavoritesOnly
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("toggle_favorite_filter")
                ) {
                    Icon(
                        imageVector = if (filterFavoritesOnly) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "过滤",
                        tint = CfOrangeSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (filterFavoritesOnly) "仅收藏" else "显示全部")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Saved List
        if (displayedIps.isEmpty()) {
            EmptySavedState()
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(displayedIps.distinctBy { it.ip }, key = { it.ip }) { entity ->
                    SavedIpItemCard(
                        entity = entity,
                        onToggleFavorite = { viewModel.toggleFavorite(entity.ip, !entity.isFavorite) },
                        onDelete = { viewModel.deleteSavedIp(entity.ip) },
                        onSetAsProxy = {
                            viewModel.switchProxyTarget(
                                ScannedIp(
                                    ip = entity.ip,
                                    dataCenter = entity.dataCenter,
                                    region = entity.region,
                                    city = entity.city,
                                    latencyMs = entity.latencyMs,
                                    ipVersion = entity.ipVersion
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SavedIpItemCard(
    entity: ScannedIpEntity,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onSetAsProxy: () -> Unit
) {
    val latencyColor = when {
        entity.latencyMs < 100 -> PingGreenFast
        entity.latencyMs < 220 -> PingYellowMedium
        else -> PingRedSlow
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entity.ip,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = OffWhiteText
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CfOrangePrimary.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = entity.dataCenter,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = CfOrangePrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${entity.city.ifEmpty { "Cloudflare 边缘节点" }} • ${entity.region}",
                    style = MaterialTheme.typography.bodySmall.copy(color = MutedText)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(latencyColor.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${entity.latencyMs} ms",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = latencyColor,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                IconButton(
                    onClick = onSetAsProxy,
                    modifier = Modifier.testTag("saved_set_proxy_${entity.ip}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Router,
                        contentDescription = "设为代理",
                        tint = NeonCyan
                    )
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.testTag("saved_favorite_${entity.ip}")
                ) {
                    Icon(
                        imageVector = if (entity.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "收藏",
                        tint = CfOrangePrimary
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("saved_delete_${entity.ip}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MutedText
                    )
                }
            }
        }
    }
}

@Composable
fun EmptySavedState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Storage,
            contentDescription = "Empty DB",
            modifier = Modifier.size(64.dp),
            tint = MutedText
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "仓库中无已保存的 IP",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = OffWhiteText
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "运行 Cloudflare IP 扫描器发现高速边缘节点并自动保存于此。",
            style = MaterialTheme.typography.bodySmall.copy(color = MutedText),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
