package com.example.service

import com.example.data.model.ScanConfig
import com.example.data.model.ScannedIp
import com.example.data.network.CloudflareCidrs
import com.example.data.network.CloudflareLocations
import com.example.data.network.IpGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

data class ScanProgressState(
    val isScanning: Boolean = false,
    val scannedCount: Int = 0,
    val totalCount: Int = 0,
    val validCount: Int = 0,
    val progressPercentage: Float = 0f,
    val results: List<ScannedIp> = emptyList(),
    val statusMessage: String = "就绪"
)

class IpScannerEngine {

    private val _progressState = MutableStateFlow(ScanProgressState())
    val progressState: StateFlow<ScanProgressState> = _progressState.asStateFlow()

    private val httpTraceClient = OkHttpClient.Builder()
        .connectTimeout(1, TimeUnit.SECONDS)
        .readTimeout(1, TimeUnit.SECONDS)
        .followRedirects(false)
        .build()

    @Volatile
    private var isCancelled = false

    fun stopScan() {
        isCancelled = true
        _progressState.value = _progressState.value.copy(
            isScanning = false,
            statusMessage = "扫描已停止"
        )
    }

    suspend fun startScan(config: ScanConfig) = withContext(Dispatchers.IO) {
        isCancelled = false
        _progressState.value = ScanProgressState(
            isScanning = true,
            statusMessage = "正在加载 Cloudflare 数据中心和子网..."
        )

        val locationsMap = CloudflareLocations.loadLocations()

        val isIpv6 = config.ipType == "6"
        val cidrs = CloudflareCidrs.fetchRemoteIps(isIpv6)

        _progressState.value = _progressState.value.copy(
            statusMessage = "正在生成目标 IP 地址..."
        )

        val candidateIps = if (isIpv6) {
            IpGenerator.getRandomIPv6s(cidrs, count = config.ipCount)
        } else {
            IpGenerator.getRandomIPv4s(cidrs, count = config.ipCount)
        }

        if (candidateIps.isEmpty()) {
            _progressState.value = ScanProgressState(
                isScanning = false,
                statusMessage = "未生成 IP 地址。"
            )
            return@withContext
        }

        val total = candidateIps.size
        val scannedCounter = AtomicInteger(0)
        val validCounter = AtomicInteger(0)
        val resultsQueue = ConcurrentLinkedQueue<ScannedIp>()

        _progressState.value = ScanProgressState(
            isScanning = true,
            scannedCount = 0,
            totalCount = total,
            validCount = 0,
            progressPercentage = 0f,
            results = emptyList(),
            statusMessage = "正在使用 ${config.maxThreads} 个线程扫描 $total 个 IP..."
        )

        val semaphore = Semaphore(config.maxThreads.coerceIn(5, 200))
        val filters = if (config.coloFilter.isNotBlank()) {
            config.coloFilter.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() }
        } else emptyList()

        val isAllRegions = filters.isEmpty() || filters.contains("ALL")
        val maxPerColo = 10
        val coloCounts = ConcurrentHashMap<String, AtomicInteger>()

        coroutineScope {
            candidateIps.forEach { ipAddr ->
                if (isCancelled) return@forEach

                launch {
                    semaphore.withPermit {
                        if (isCancelled) return@withPermit

                        val scannedIp = testIpAddress(
                            ip = ipAddr,
                            port = config.port,
                            timeoutMs = config.delayMs,
                            domain = config.domain,
                            expectedCode = config.expectedCode,
                            ipVersion = config.ipType
                        )

                        val currentScanned = scannedCounter.incrementAndGet()

                        if (scannedIp != null && scannedIp.isValid && !scannedIp.dataCenter.equals("CF", ignoreCase = true)) {
                            var matchesFilter = true
                            if (!isAllRegions && filters.isNotEmpty()) {
                                matchesFilter = filters.any { filter ->
                                    scannedIp.dataCenter.equals(filter, ignoreCase = true)
                                }
                            }

                            if (matchesFilter) {
                                val colo = scannedIp.dataCenter
                                val coloCount = coloCounts.getOrPut(colo) { AtomicInteger(0) }
                                
                                // Limit to maxPerColo if in ALL mode
                                if (!isAllRegions || coloCount.get() < maxPerColo) {
                                    coloCount.incrementAndGet()
                                    validCounter.incrementAndGet()
                                    resultsQueue.add(scannedIp)
                                }
                            }
                        }

                        // Periodically update UI (every 5 IPs or at final scan item)
                        if (currentScanned % 5 == 0 || currentScanned == total) {
                            val validList = resultsQueue.distinctBy { it.ip }.sortedBy { it.latencyMs }
                            val pct = currentScanned.toFloat() / total.toFloat()

                            _progressState.value = ScanProgressState(
                                isScanning = !isCancelled && currentScanned < total,
                                scannedCount = currentScanned,
                                totalCount = total,
                                validCount = validList.size,
                                progressPercentage = pct,
                                results = validList.take(config.ipCount * 2),
                                statusMessage = if (currentScanned < total) "已扫描 $currentScanned / $total (${validList.size} 个有效)" else "扫描完成！找到 ${validList.size} 个有效 IP"
                            )
                        }
                    }
                }
            }
        }

        val finalResults = resultsQueue.distinctBy { it.ip }.sortedBy { it.latencyMs }.take(config.ipCount)
        _progressState.value = ScanProgressState(
            isScanning = false,
            scannedCount = total,
            totalCount = total,
            validCount = finalResults.size,
            progressPercentage = 1f,
            results = finalResults,
            statusMessage = "扫描完成！提取了 ${finalResults.size} 个最佳 IP。"
        )
    }

    private fun testIpAddress(
        ip: String,
        port: Int,
        timeoutMs: Int,
        domain: String,
        expectedCode: Int,
        ipVersion: String
    ): ScannedIp? {
        val startTime = System.currentTimeMillis()
        var socket: Socket? = null
        var tcpLatency: Long = 0
        try {
            socket = Socket()
            socket.connect(InetSocketAddress(ip, port), timeoutMs)
            tcpLatency = System.currentTimeMillis() - startTime
        } catch (e: Exception) {
            return null
        } finally {
            try { socket?.close() } catch (_: Exception) {}
        }

        if (tcpLatency > timeoutMs) {
            return null
        }

        // Detect Colo code via HTTP trace or response headers
        val coloCode = detectColoCode(ip, port, domain) ?: "CF"
        val loc = CloudflareLocations.getLocation(coloCode)

        return ScannedIp(
            ip = ip,
            dataCenter = coloCode.uppercase(),
            region = loc.region.ifEmpty { "全球边缘节点" },
            city = loc.city.ifEmpty { coloCode },
            latencyMs = tcpLatency,
            isValid = true,
            ipVersion = ipVersion,
            testedAt = System.currentTimeMillis()
        )
    }

    private fun detectColoCode(ip: String, port: Int, domain: String): String? {
        val targetHost = domain.substringBefore("/")
        val protocol = if (port == 443) "https" else "http"
        val traceUrl = "$protocol://$targetHost/cdn-cgi/trace"

        // Use custom DNS to force the domain to resolve to our specific IP.
        // This solves the SNI issue for HTTPS perfectly without disabling TLS.
        val customClient = httpTraceClient.newBuilder()
            .dns(object : okhttp3.Dns {
                override fun lookup(hostname: String): List<java.net.InetAddress> {
                    if (hostname == targetHost) {
                        return listOf(java.net.InetAddress.getByName(ip))
                    }
                    return okhttp3.Dns.SYSTEM.lookup(hostname)
                }
            })
            .build()

        return try {
            val request = Request.Builder()
                .url(traceUrl)
                .header("User-Agent", "CloudflareScanner/1.0")
                .build()

            customClient.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (body.contains("colo=")) {
                    body.lines().forEach { line ->
                        if (line.startsWith("colo=")) {
                            return line.substringAfter("colo=").trim()
                        }
                    }
                }
                val cfRay = response.header("CF-RAY")
                if (!cfRay.isNullOrEmpty() && cfRay.contains("-")) {
                    return cfRay.substringAfterLast("-").trim()
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
}
