package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ScanConfig
import com.example.data.model.ScannedIp
import com.example.service.ScanProgressState
import com.example.ui.MainViewModel
import com.example.ui.theme.CfOrangeDark
import com.example.ui.theme.CfOrangePrimary
import com.example.ui.theme.CfOrangeSecondary
import com.example.ui.theme.DarkSlateCard
import com.example.ui.theme.MutedText
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.OffWhiteText
import com.example.ui.theme.PingGreenFast
import com.example.ui.theme.PingRedSlow
import com.example.ui.theme.PingYellowMedium

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    viewModel: MainViewModel,
    scanProgress: ScanProgressState,
    scanConfig: ScanConfig
) {
    val context = LocalContext.current
    var showSettings by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Hero Scan Banner
        HeroScanBanner(
            scanProgress = scanProgress,
            onStartScan = { viewModel.startScan() },
            onStopScan = { viewModel.stopScan() },
            onToggleSettings = { showSettings = !showSettings },
            showSettings = showSettings
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Expandable Configuration Card
        AnimatedVisibility(
            visible = showSettings,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            ScanConfigCard(
                scanConfig = scanConfig,
                onConfigChange = { viewModel.updateScanConfig(it) }
            )
        }

        if (showSettings) {
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Live Scanning Progress Indicator
        if (scanProgress.isScanning || scanProgress.scannedCount > 0) {
            ScanProgressBar(scanProgress = scanProgress)
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Quick Preset Filter Bar
        QuickFilterChips(
            scanConfig = scanConfig,
            scanProgress = scanProgress,
            onColoSelect = { colo ->
                val currentFilters = scanConfig.coloFilter.split(",")
                    .map { it.trim().uppercase() }
                    .filter { it.isNotBlank() }
                    .toMutableSet()
                
                if (currentFilters.contains(colo)) {
                    currentFilters.remove(colo)
                } else {
                    currentFilters.add(colo)
                }
                viewModel.updateScanConfig(scanConfig.copy(coloFilter = currentFilters.joinToString(",")))
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        val displayResults = remember(scanProgress.results, scanConfig.coloFilter) {
            val uniqueResults = scanProgress.results.distinctBy { it.ip }
            val filteredResults = if (scanConfig.coloFilter.isNotBlank()) {
                val filters = scanConfig.coloFilter.split(",").map { it.trim().uppercase() }
                if (filters.contains("ALL")) {
                    uniqueResults
                } else {
                    uniqueResults.filter { ip ->
                        filters.any { filter -> ip.dataCenter.equals(filter, ignoreCase = true) }
                    }
                }
            } else {
                uniqueResults
            }
            
            // Limit to max 10 IPs per datacenter
            filteredResults.groupBy { it.dataCenter.uppercase() }
                .flatMap { it.value.sortedBy { ip -> ip.latencyMs }.take(10) }
                .sortedBy { it.latencyMs }
        }

        // Results Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "扫描到的有效 IP (${displayResults.size})",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = OffWhiteText
                )
            )

            if (displayResults.isNotEmpty()) {
                IconButton(
                    onClick = {
                        val ips = displayResults.map { it.ip }
                        viewModel.copyIpsToClipboard(context, ips, "Scanned Cloudflare IPs")
                    },
                    modifier = Modifier.testTag("copy_all_ips_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "复制所有 IP",
                        tint = CfOrangeSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Results List
        if (displayResults.isEmpty() && !scanProgress.isScanning) {
            EmptyScannerState(onStartScan = { viewModel.startScan() })
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = displayResults,
                    key = { it.ip }
                ) { ip ->
                    ScannedIpCard(
                        scannedIp = ip,
                        onSetAsProxy = { viewModel.switchProxyTarget(ip) },
                        onSave = { viewModel.saveSingleIp(ip) }
                    )
                }
            }
        }
    }
}

@Composable
fun HeroScanBanner(
    scanProgress: ScanProgressState,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onToggleSettings: () -> Unit,
    showSettings: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(CfOrangePrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Scan Icon",
                            tint = CfOrangePrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Cloudflare IP 引擎",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = OffWhiteText
                            )
                        )
                        Text(
                            text = scanProgress.statusMessage,
                            style = MaterialTheme.typography.bodySmall.copy(color = MutedText),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onToggleSettings,
                    modifier = Modifier.testTag("toggle_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = if (showSettings) CfOrangePrimary else MutedText
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (scanProgress.isScanning) onStopScan() else onStartScan()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("start_stop_scan_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (scanProgress.isScanning) PingRedSlow else CfOrangePrimary
                )
            ) {
                if (scanProgress.isScanning) {
                    Icon(imageVector = Icons.Default.Stop, contentDescription = "Stop")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "停止扫描", fontWeight = FontWeight.Bold)
                } else {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Start")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "开始高速扫描", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ScanProgressBar(scanProgress: ScanProgressState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "进度：${(scanProgress.progressPercentage * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium.copy(color = OffWhiteText)
                )
                Text(
                    text = "${scanProgress.scannedCount} / ${scanProgress.totalCount} IPs",
                    style = MaterialTheme.typography.labelMedium.copy(color = CfOrangeSecondary)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { scanProgress.progressPercentage.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = CfOrangePrimary,
                trackColor = DarkSlateCard
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanConfigCard(
    scanConfig: ScanConfig,
    onConfigChange: (ScanConfig) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "扫描器选项",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = CfOrangeSecondary
                )
            )

            // IPv4 vs IPv6 Segmented Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("IP 版本", style = MaterialTheme.typography.bodyMedium.copy(color = OffWhiteText))
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = scanConfig.ipType == "4",
                        onClick = { onConfigChange(scanConfig.copy(ipType = "4")) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("IPv4")
                    }
                    SegmentedButton(
                        selected = scanConfig.ipType == "6",
                        onClick = { onConfigChange(scanConfig.copy(ipType = "6")) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("IPv6")
                    }
                }
            }

            // Concurrency Threads
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("并发线程", style = MaterialTheme.typography.bodyMedium.copy(color = OffWhiteText))
                    Text("${scanConfig.maxThreads}", style = MaterialTheme.typography.bodyMedium.copy(color = CfOrangePrimary, fontWeight = FontWeight.Bold))
                }
                Slider(
                    value = scanConfig.maxThreads.toFloat(),
                    onValueChange = { onConfigChange(scanConfig.copy(maxThreads = it.toInt())) },
                    valueRange = 10f..200f,
                    colors = SliderDefaults.colors(thumbColor = CfOrangePrimary, activeTrackColor = CfOrangePrimary)
                )
            }

            // Max Delay Threshold
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("最大延迟限制", style = MaterialTheme.typography.bodyMedium.copy(color = OffWhiteText))
                    Text("${scanConfig.delayMs} ms", style = MaterialTheme.typography.bodyMedium.copy(color = CfOrangePrimary, fontWeight = FontWeight.Bold))
                }
                Slider(
                    value = scanConfig.delayMs.toFloat(),
                    onValueChange = { onConfigChange(scanConfig.copy(delayMs = it.toInt())) },
                    valueRange = 100f..1000f,
                    colors = SliderDefaults.colors(thumbColor = CfOrangePrimary, activeTrackColor = CfOrangePrimary)
                )
            }

            // Target IP Count
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("目标 IP 数量", style = MaterialTheme.typography.bodyMedium.copy(color = OffWhiteText))
                    Text("${scanConfig.ipCount} IPs", style = MaterialTheme.typography.bodyMedium.copy(color = CfOrangePrimary, fontWeight = FontWeight.Bold))
                }
                Slider(
                    value = scanConfig.ipCount.toFloat(),
                    onValueChange = { onConfigChange(scanConfig.copy(ipCount = it.toInt())) },
                    valueRange = 10f..3000f,
                    colors = SliderDefaults.colors(thumbColor = CfOrangePrimary, activeTrackColor = CfOrangePrimary)
                )
            }

            // Custom Datacenter Filter
            OutlinedTextField(
                value = scanConfig.coloFilter,
                onValueChange = { onConfigChange(scanConfig.copy(coloFilter = it)) },
                label = { Text("数据中心过滤器 (如 HKG,SJC,LAX)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("colo_filter_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CfOrangePrimary,
                    unfocusedBorderColor = MutedText
                )
            )

            // Custom Target Domain
            OutlinedTextField(
                value = scanConfig.domain,
                onValueChange = { onConfigChange(scanConfig.copy(domain = it)) },
                label = { Text("Cloudflare 健康检查域名") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("domain_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CfOrangePrimary,
                    unfocusedBorderColor = MutedText
                )
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuickFilterChips(
    scanConfig: ScanConfig,
    scanProgress: ScanProgressState,
    onColoSelect: (String) -> Unit
) {
    var discoveredColos by remember { 
        mutableStateOf(
            scanConfig.coloFilter.split(",")
                .map { it.trim().uppercase() }
                .filter { it.isNotBlank() && it != "ALL" }
                .toSet()
        ) 
    }
    
    // Clear unselected discovered colos when a new scan starts
    LaunchedEffect(scanProgress.isScanning) {
        if (scanProgress.isScanning) {
            discoveredColos = scanConfig.coloFilter.split(",")
                .map { it.trim().uppercase() }
                .filter { it.isNotBlank() && it != "ALL" }
                .toSet()
        }
    }

    // Accumulate discovered colos over time
    LaunchedEffect(scanProgress.results) {
        if (scanProgress.results.isNotEmpty()) {
            val newColos = scanProgress.results.map { it.dataCenter.uppercase() }.toSet()
            discoveredColos = discoveredColos + newColos
        }
    }

    val dynamicColos = remember(discoveredColos) {
        discoveredColos.sorted()
    }
    
    val presets = remember(dynamicColos) {
        listOf("ALL") + dynamicColos
    }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(presets) { colo ->
            val currentFilters = scanConfig.coloFilter.split(",").map { it.trim().uppercase() }.filter { it.isNotBlank() }
            val isSelected = if (colo == "ALL") currentFilters.isEmpty() || currentFilters.contains("ALL") else currentFilters.contains(colo)
            
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onColoSelect(colo) },
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) CfOrangePrimary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = colo, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun ScannedIpCard(
    scannedIp: ScannedIp,
    onSetAsProxy: () -> Unit,
    onSave: () -> Unit
) {
    val latencyColor = when {
        scannedIp.latencyMs < 100 -> PingGreenFast
        scannedIp.latencyMs < 220 -> PingYellowMedium
        else -> PingRedSlow
    }

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        clipboardManager.setText(AnnotatedString(scannedIp.ip))
                        android.widget.Toast.makeText(context, "已复制 ${scannedIp.ip}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
            },
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
                Text(
                    text = scannedIp.ip,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = OffWhiteText
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CfOrangePrimary.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = scannedIp.dataCenter,
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = CfOrangePrimary,
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${scannedIp.city.ifEmpty { "Cloudflare 边缘节点" }} • ${scannedIp.region}",
                        style = MaterialTheme.typography.bodySmall.copy(color = MutedText)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Latency Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(latencyColor.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${scannedIp.latencyMs} ms",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = latencyColor,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Set as Proxy Target Button
                IconButton(
                    onClick = onSetAsProxy,
                    modifier = Modifier.testTag("set_proxy_target_button_${scannedIp.ip}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Router,
                        contentDescription = "Set as Proxy",
                        tint = NeonCyan
                    )
                }

                // Favorite Button
                IconButton(
                    onClick = onSave,
                    modifier = Modifier.testTag("save_ip_button_${scannedIp.ip}")
                ) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = "Save IP",
                        tint = CfOrangeSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyScannerState(onStartScan: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Bolt,
            contentDescription = "Empty Scanner",
            modifier = Modifier.size(64.dp),
            tint = MutedText
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "暂未扫描到 Cloudflare IP",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = OffWhiteText
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "点击“开始高速扫描”以测试全球数据中心 Cloudflare 边缘节点的延迟。",
            style = MaterialTheme.typography.bodySmall.copy(color = MutedText),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
