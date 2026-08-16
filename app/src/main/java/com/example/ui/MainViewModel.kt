package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.db.IpRepository
import com.example.data.model.CfDnsRuleEntity
import com.example.data.model.ProxyStatus
import com.example.data.model.ScanConfig
import com.example.data.model.ScanHistoryEntity
import com.example.data.model.ScannedIp
import com.example.data.model.ScannedIpEntity
import com.example.service.CloudflareDnsSyncService
import com.example.service.IpScannerEngine
import com.example.service.ScanProgressState
import com.example.service.SyncResult
import com.example.service.TcpProxyServer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import android.content.SharedPreferences

class MainViewModel(
    private val repository: IpRepository,
    private val prefs: SharedPreferences,
    private val context: Context
) : ViewModel() {

    private val scannerEngine = IpScannerEngine(context)
    private val proxyServer = TcpProxyServer()
    private val dnsSyncService = CloudflareDnsSyncService()

    val scanProgress: StateFlow<ScanProgressState> = scannerEngine.progressState
    val proxyStatus: StateFlow<ProxyStatus> = proxyServer.proxyStatus

    val savedIps: StateFlow<List<ScannedIpEntity>> = repository.savedIps.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val favoriteIps: StateFlow<List<ScannedIpEntity>> = repository.favoriteIps.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val scanHistory: StateFlow<List<ScanHistoryEntity>> = repository.scanHistory.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val dnsRules: StateFlow<List<CfDnsRuleEntity>> = repository.dnsRules.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private fun loadScanConfig(): ScanConfig {
        return ScanConfig(
            ipType = prefs.getString("sc_ipType", "4") ?: "4",
            port = prefs.getInt("sc_port", 443),
            maxThreads = prefs.getInt("sc_maxThreads", 100),
            delayMs = prefs.getInt("sc_delayMs", 350),
            coloFilter = prefs.getString("sc_coloFilter", "") ?: "",
            domain = prefs.getString("sc_domain", "cloudflaremirrors.com/debian") ?: "cloudflaremirrors.com/debian",
            expectedCode = prefs.getInt("sc_expectedCode", 200),
            random = prefs.getBoolean("sc_random", true),
            ipCount = prefs.getInt("sc_ipCount", 2000),
            useTls = prefs.getBoolean("sc_useTls", true)
        )
    }

    private fun saveScanConfig(config: ScanConfig) {
        prefs.edit().apply {
            putString("sc_ipType", config.ipType)
            putInt("sc_port", config.port)
            putInt("sc_maxThreads", config.maxThreads)
            putInt("sc_delayMs", config.delayMs)
            putString("sc_coloFilter", config.coloFilter)
            putString("sc_domain", config.domain)
            putInt("sc_expectedCode", config.expectedCode)
            putBoolean("sc_random", config.random)
            putInt("sc_ipCount", config.ipCount)
            putBoolean("sc_useTls", config.useTls)
        }.apply()
    }

    private val _scanConfig = MutableStateFlow(loadScanConfig())
    val scanConfig: StateFlow<ScanConfig> = _scanConfig.asStateFlow()

    private val _isAutoSyncEnabled = MutableStateFlow(prefs.getBoolean("sc_autoSync", true))
    val isAutoSyncEnabled: StateFlow<Boolean> = _isAutoSyncEnabled.asStateFlow()

    fun setAutoSyncEnabled(enabled: Boolean) {
        _isAutoSyncEnabled.value = enabled
        prefs.edit().putBoolean("sc_autoSync", enabled).apply()
    }

    fun updateScanConfig(config: ScanConfig) {
        _scanConfig.value = config
        saveScanConfig(config)
    }

    fun startScan() {
        viewModelScope.launch {
            val config = _scanConfig.value
            scannerEngine.startScan(config)

            // Save history when scan completes
            val state = scannerEngine.progressState.value
            if (state.results.isNotEmpty()) {
                val best = state.results.first()
                repository.addHistory(
                    ScanHistoryEntity(
                        ipType = config.ipType,
                        totalScanned = state.scannedCount,
                        validFound = state.validCount,
                        bestIp = best.ip,
                        bestLatencyMs = best.latencyMs,
                        bestColo = best.dataCenter
                    )
                )

                // Save top scanned IPs into database automatically
                val entities = state.results.map { ip ->
                    ScannedIpEntity(
                        ip = ip.ip,
                        dataCenter = ip.dataCenter,
                        region = ip.region,
                        city = ip.city,
                        latencyMs = ip.latencyMs,
                        testedAt = ip.testedAt,
                        ipVersion = ip.ipVersion,
                        isFavorite = false,
                        port = config.port
                    )
                }
                repository.saveIps(entities)

                // Auto-sync enabled Cloudflare DNS rules
                if (_isAutoSyncEnabled.value) {
                    autoSyncEnabledDnsRules(state.results)
                }
            }
        }
    }

    fun stopScan() {
        scannerEngine.stopScan()
    }

    // DNS Rule Actions
    fun saveDnsRule(rule: CfDnsRuleEntity) {
        viewModelScope.launch {
            repository.saveDnsRule(rule)
        }
    }

    fun updateDnsRule(rule: CfDnsRuleEntity) {
        viewModelScope.launch {
            repository.updateDnsRule(rule)
        }
    }

    fun deleteDnsRule(id: Long) {
        viewModelScope.launch {
            repository.deleteDnsRule(id)
        }
    }

    fun triggerSyncRule(rule: CfDnsRuleEntity) {
        viewModelScope.launch {
            repository.updateDnsRuleSyncResult(rule.id, "同步中...", "", System.currentTimeMillis())

            val currentResults = scanProgress.value.results
            val targetIps = findBestMatchingIps(rule, currentResults, rule.maxIpCount)

            if (targetIps.isEmpty()) {
                repository.updateDnsRuleSyncResult(
                    rule.id,
                    "错误: 未找到匹配过滤器 '${rule.coloFilter.ifBlank { "ALL" }}' 的 IP",
                    "",
                    System.currentTimeMillis()
                )
                return@launch
            }

            val ipSummaryStr = targetIps.joinToString(", ")
            when (val result = dnsSyncService.syncDnsRecords(rule, targetIps)) {
                is SyncResult.Success -> {
                    repository.updateDnsRuleSyncResult(
                        rule.id,
                        result.message,
                        ipSummaryStr,
                        System.currentTimeMillis()
                    )
                }
                is SyncResult.Error -> {
                    repository.updateDnsRuleSyncResult(
                        rule.id,
                        result.message,
                        ipSummaryStr,
                        System.currentTimeMillis()
                    )
                }
            }
        }
    }

    fun triggerSyncAllRules() {
        viewModelScope.launch {
            val rules = repository.getEnabledDnsRules()
            for (rule in rules) {
                triggerSyncRule(rule)
            }
        }
    }

    private suspend fun autoSyncEnabledDnsRules(scannedIps: List<ScannedIp>) {
        val enabledRules = repository.getEnabledDnsRules()
        for (rule in enabledRules) {
            val targetIps = findBestMatchingIps(rule, scannedIps, rule.maxIpCount)
            if (targetIps.isNotEmpty()) {
                val ipSummaryStr = targetIps.joinToString(", ")
                when (val result = dnsSyncService.syncDnsRecords(rule, targetIps)) {
                    is SyncResult.Success -> {
                        repository.updateDnsRuleSyncResult(
                            rule.id,
                            result.message,
                            ipSummaryStr,
                            System.currentTimeMillis()
                        )
                    }
                    is SyncResult.Error -> {
                        repository.updateDnsRuleSyncResult(
                            rule.id,
                            result.message,
                            ipSummaryStr,
                            System.currentTimeMillis()
                        )
                    }
                }
            } else {
                repository.updateDnsRuleSyncResult(
                    rule.id,
                    "错误: 自动同步时未找到匹配过滤器 '${rule.coloFilter.ifBlank { "ALL" }}' 的 IP",
                    "",
                    System.currentTimeMillis()
                )
            }
        }
    }

    private fun findBestMatchingIps(rule: CfDnsRuleEntity, currentScanResults: List<ScannedIp>, limit: Int): List<String> {
        val filter = rule.coloFilter.trim()
        val resultList = mutableListOf<String>()
        val maxLimit = limit.coerceAtLeast(1)

        // 1. Try matching in current scan results FIRST
        if (currentScanResults.isNotEmpty()) {
            val scanMatches = if (filter.isBlank() || filter.equals("ALL", ignoreCase = true)) {
                currentScanResults
            } else {
                currentScanResults.filter { ip ->
                    ip.dataCenter.contains(filter, ignoreCase = true) ||
                            ip.city.contains(filter, ignoreCase = true) ||
                            ip.region.contains(filter, ignoreCase = true)
                }
            }
            for (match in scanMatches) {
                if (!resultList.contains(match.ip)) {
                    resultList.add(match.ip)
                    if (resultList.size >= maxLimit) break
                }
            }
            // If we have current scan results, we strictly use them.
            // We do NOT fallback to old saved IPs if the current scan didn't find enough.
            return resultList
        }

        // 2. Fallback to saved IPs in Room DB ONLY if no scan has been performed yet
        val saved = savedIps.value
        if (saved.isNotEmpty()) {
            val savedMatches = if (filter.isBlank() || filter.equals("ALL", ignoreCase = true)) {
                saved
            } else {
                saved.filter { ip ->
                    ip.dataCenter.contains(filter, ignoreCase = true) ||
                            ip.city.contains(filter, ignoreCase = true) ||
                            ip.region.contains(filter, ignoreCase = true)
                }
            }
            for (match in savedMatches) {
                if (!resultList.contains(match.ip)) {
                    resultList.add(match.ip)
                    if (resultList.size >= maxLimit) return resultList
                }
            }
        }

        return resultList
    }

    fun toggleProxy(localPort: Int = 1234) {
        val current = proxyStatus.value
        if (current.isRunning) {
            proxyServer.stopServer()
        } else {
            val config = _scanConfig.value
            val currentResults = scanProgress.value.results
            val targetPool = if (currentResults.isNotEmpty()) {
                currentResults
            } else {
                // Use saved IPs from Room DB
                savedIps.value.map { entity ->
                    ScannedIp(
                        ip = entity.ip,
                        dataCenter = entity.dataCenter,
                        region = entity.region,
                        city = entity.city,
                        latencyMs = entity.latencyMs,
                        ipVersion = entity.ipVersion,
                        isFavorite = entity.isFavorite
                    )
                }
            }

            if (targetPool.isEmpty()) {
                return
            }

            proxyServer.startServer(
                scope = viewModelScope,
                localPort = localPort,
                targetPort = config.port,
                initialIps = targetPool,
                maxDelayMs = config.delayMs,
                domain = config.domain,
                useTls = config.useTls
            )
        }
    }

    fun switchProxyTarget(ip: ScannedIp) {
        proxyServer.switchTargetIpManually(ip)
    }

    fun toggleFavorite(ip: String, isFavorite: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(ip, isFavorite)
        }
    }

    fun saveSingleIp(ip: ScannedIp) {
        viewModelScope.launch {
            val entity = ScannedIpEntity(
                ip = ip.ip,
                dataCenter = ip.dataCenter,
                region = ip.region,
                city = ip.city,
                latencyMs = ip.latencyMs,
                testedAt = System.currentTimeMillis(),
                ipVersion = ip.ipVersion,
                isFavorite = true
            )
            repository.saveIp(entity)
        }
    }

    fun deleteSavedIp(ip: String) {
        viewModelScope.launch {
            repository.deleteIp(ip)
        }
    }

    fun clearSavedIps() {
        viewModelScope.launch {
            repository.clearSavedIps()
        }
    }

    fun copyIpsToClipboard(context: Context, ips: List<String>, label: String = "Cloudflare IPs") {
        if (ips.isEmpty()) return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = ips.joinToString("\n")
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "已复制 ${ips.size} 个 IP 到剪贴板！", Toast.LENGTH_SHORT).show()
    }

    override fun onCleared() {
        super.onCleared()
        proxyServer.stopServer()
    }
}

class MainViewModelFactory(
    private val repository: IpRepository,
    private val prefs: SharedPreferences,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, prefs, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
