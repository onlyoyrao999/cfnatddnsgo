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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mail
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CfDnsRuleEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.CfOrangePrimary
import com.example.ui.theme.DarkSlateCard
import com.example.ui.theme.DarkSlateSurface
import com.example.ui.theme.MutedText
import com.example.ui.theme.OffWhiteText
import com.example.ui.theme.PingGreenFast
import com.example.ui.theme.PingRedSlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DnsSyncScreen(
    viewModel: MainViewModel,
    dnsRules: List<CfDnsRuleEntity>
) {
    val isAutoSyncEnabled by viewModel.isAutoSyncEnabled.collectAsState()
    var showRuleDialog by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<CfDnsRuleEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingRule = null
                    showRuleDialog = true
                },
                containerColor = CfOrangePrimary,
                contentColor = DarkSlateSurface,
                modifier = Modifier.testTag("add_dns_rule_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Cloudflare Domain Sync Rule")
            }
        },
        containerColor = DarkSlateSurface
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Header Info Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSlateCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = CfOrangePrimary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Cloudflare DNS 同步",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = OffWhiteText
                                )
                            )
                        }

                        if (dnsRules.isNotEmpty()) {
                            Button(
                                onClick = { viewModel.triggerSyncAllRules() },
                                colors = ButtonDefaults.buttonColors(containerColor = CfOrangePrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Sync All",
                                    tint = DarkSlateSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("全部同步", color = DarkSlateSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "设置目标域名，根据节点/机场过滤器（如 HKG、SJC、LAX）自动或手动同步扫描到的 Cloudflare IP。",
                        style = MaterialTheme.typography.bodySmall.copy(color = MutedText)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "扫描后自动同步",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = OffWhiteText
                            )
                        )
                        androidx.compose.material3.Switch(
                            checked = isAutoSyncEnabled,
                            onCheckedChange = { viewModel.setAutoSyncEnabled(it) },
                            colors = androidx.compose.material3.SwitchDefaults.colors(
                                checkedThumbColor = CfOrangePrimary,
                                checkedTrackColor = CfOrangePrimary.copy(alpha = 0.5f),
                                uncheckedThumbColor = MutedText,
                                uncheckedTrackColor = DarkSlateSurface
                            )
                        )
                    }
                }
            }

            if (dnsRules.isEmpty()) {
                // Empty State
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSlateCard),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = null,
                            tint = MutedText,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "无 Cloudflare 域名规则",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = OffWhiteText
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "点击 + 按钮添加域名规则。将根据机场/数据中心过滤器自动同步 IP 到您的 Cloudflare DNS 记录。",
                            style = MaterialTheme.typography.bodySmall.copy(color = MutedText),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = {
                                editingRule = null
                                showRuleDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CfOrangePrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = DarkSlateSurface)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("添加域名规则", color = DarkSlateSurface, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // List of Domain Rules
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(dnsRules, key = { it.id }) { rule ->
                        DnsRuleCard(
                            rule = rule,
                            onSync = { viewModel.triggerSyncRule(rule) },
                            onToggleEnabled = { enabled ->
                                viewModel.updateDnsRule(rule.copy(isEnabled = enabled))
                            },
                            onEdit = {
                                editingRule = rule
                                showRuleDialog = true
                            },
                            onDelete = {
                                viewModel.deleteDnsRule(rule.id)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showRuleDialog) {
        DnsRuleDialog(
            existingRule = editingRule,
            onDismiss = { showRuleDialog = false },
            onSave = { rule ->
                if (rule.id == 0L) {
                    viewModel.saveDnsRule(rule)
                } else {
                    viewModel.updateDnsRule(rule)
                }
                showRuleDialog = false
            }
        )
    }
}

@Composable
fun DnsRuleCard(
    rule: CfDnsRuleEntity,
    onSync: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSlateCard),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Rule Name & Enable Switch
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = rule.ruleName.ifBlank { "域名规则" },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = OffWhiteText
                        )
                    )
                    Text(
                        text = rule.cfRecordName.ifBlank { "no-domain.com" },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = CfOrangePrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                Switch(
                    checked = rule.isEnabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = DarkSlateSurface,
                        checkedTrackColor = CfOrangePrimary,
                        uncheckedThumbColor = MutedText,
                        uncheckedTrackColor = DarkSlateSurface
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Badges Row: Colo Filter
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Colo Filter Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CfOrangePrimary.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = CfOrangePrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (rule.coloFilter.isBlank()) "过滤器: 全部" else "机场/数据中心: ${rule.coloFilter}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = CfOrangePrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                // Sync IP Count Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CfOrangePrimary.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "同步数量: ${rule.maxIpCount} 个 IP",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = CfOrangePrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // Zone ID Badge
                if (rule.cfZoneId.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSlateSurface)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "区域 ID: ${rule.cfZoneId.take(6)}...",
                            style = MaterialTheme.typography.labelSmall.copy(color = MutedText)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Last Sync Details
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                val isSuccess = rule.lastSyncStatus.contains("Synced") || rule.lastSyncStatus.contains("up-to-date")
                val isError = rule.lastSyncStatus.contains("Error") || rule.lastSyncStatus.contains("failed")

                val statusColor = when {
                    isSuccess -> PingGreenFast
                    isError -> PingRedSlow
                    else -> MutedText
                }

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isSuccess) "同步成功" else rule.lastSyncStatus,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = statusColor,
                            fontWeight = FontWeight.Medium
                        )
                    )

                    if (rule.lastSyncTime > 0) {
                        val formattedTime = remember(rule.lastSyncTime) {
                            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(rule.lastSyncTime))
                        }
                        Text(
                            text = "上次同步: $formattedTime",
                            style = MaterialTheme.typography.labelSmall.copy(color = MutedText)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Rule",
                        tint = PingRedSlow
                    )
                }

                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Rule",
                        tint = MutedText
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onSync,
                    colors = ButtonDefaults.buttonColors(containerColor = CfOrangePrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = DarkSlateSurface,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("立即同步", color = DarkSlateSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun DnsRuleDialog(
    existingRule: CfDnsRuleEntity?,
    onDismiss: () -> Unit,
    onSave: (CfDnsRuleEntity) -> Unit
) {
    var ruleName by remember { mutableStateOf(existingRule?.ruleName ?: "") }
    var coloFilter by remember { mutableStateOf(existingRule?.coloFilter ?: "") }
    var cfEmail by remember { mutableStateOf(existingRule?.cfEmail ?: "") }
    var cfApiKey by remember { mutableStateOf(existingRule?.cfApiKey ?: "") }
    var cfZoneId by remember { mutableStateOf(existingRule?.cfZoneId ?: "") }
    var cfRecordName by remember { mutableStateOf(existingRule?.cfRecordName ?: "") }
    var maxIpCountText by remember { mutableStateOf((existingRule?.maxIpCount ?: 1).toString()) }
    var isEnabled by remember { mutableStateOf(existingRule?.isEnabled ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSlateCard,
        title = {
            Text(
                text = if (existingRule == null) "添加 Cloudflare 域名同步" else "编辑域名同步规则",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = OffWhiteText
                )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = ruleName,
                    onValueChange = { ruleName = it },
                    label = { Text("机场 / 规则名称 (如 HK Airport Node)") },
                    singleLine = true,
                    colors = textFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("rule_name_input")
                )

                OutlinedTextField(
                    value = coloFilter,
                    onValueChange = { coloFilter = it },
                    label = { Text("机场 / 数据中心过滤 (如 HKG, SJC, LAX)") },
                    placeholder = { Text("留空表示任意最佳 IP") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = CfOrangePrimary) },
                    colors = textFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("colo_filter_input")
                )

                OutlinedTextField(
                    value = cfRecordName,
                    onValueChange = { cfRecordName = it },
                    label = { Text("目标域名记录 (如 hk.example.com)") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Public, contentDescription = null, tint = CfOrangePrimary) },
                    colors = textFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("record_name_input")
                )

                OutlinedTextField(
                    value = maxIpCountText,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.all { it.isDigit() }) {
                            maxIpCountText = input
                        }
                    },
                    label = { Text("IP 同步数量 (默认: 1)") },
                    placeholder = { Text("1") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = { Icon(Icons.Default.Numbers, contentDescription = null, tint = CfOrangePrimary) },
                    colors = textFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("max_ip_count_input")
                )

                OutlinedTextField(
                    value = cfZoneId,
                    onValueChange = { cfZoneId = it },
                    label = { Text("Cloudflare Zone ID") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Dns, contentDescription = null, tint = CfOrangePrimary) },
                    colors = textFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("zone_id_input")
                )

                OutlinedTextField(
                    value = cfEmail,
                    onValueChange = { cfEmail = it },
                    label = { Text("Cloudflare 邮箱 (使用 Token 时可选)") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Mail, contentDescription = null, tint = CfOrangePrimary) },
                    colors = textFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("cf_email_input")
                )

                OutlinedTextField(
                    value = cfApiKey,
                    onValueChange = { cfApiKey = it },
                    label = { Text("Cloudflare Global API Key 或 Token") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = CfOrangePrimary) },
                    colors = textFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("api_key_input")
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Text("启用自动同步", style = MaterialTheme.typography.bodyMedium.copy(color = OffWhiteText))
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DarkSlateSurface,
                            checkedTrackColor = CfOrangePrimary
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedCount = maxIpCountText.toIntOrNull()?.coerceIn(1, 50) ?: 1
                    val rule = CfDnsRuleEntity(
                        id = existingRule?.id ?: 0L,
                        ruleName = ruleName,
                        coloFilter = coloFilter,
                        cfEmail = cfEmail,
                        cfApiKey = cfApiKey,
                        cfZoneId = cfZoneId,
                        cfRecordName = cfRecordName,
                        maxIpCount = parsedCount,
                        isEnabled = isEnabled,
                        lastSyncStatus = existingRule?.lastSyncStatus ?: "Not synced",
                        lastSyncedIp = existingRule?.lastSyncedIp ?: "",
                        lastSyncTime = existingRule?.lastSyncTime ?: 0L
                    )
                    onSave(rule)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CfOrangePrimary)
            ) {
                Text("保存规则", color = DarkSlateSurface, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("取消", color = OffWhiteText)
            }
        }
    )
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CfOrangePrimary,
    unfocusedBorderColor = MutedText,
    focusedLabelColor = CfOrangePrimary,
    unfocusedLabelColor = MutedText,
    focusedTextColor = OffWhiteText,
    unfocusedTextColor = OffWhiteText
)
 
