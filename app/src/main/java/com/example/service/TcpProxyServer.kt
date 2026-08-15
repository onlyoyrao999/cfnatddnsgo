package com.example.service

import com.example.data.model.ProxyStatus
import com.example.data.model.ScannedIp
import com.example.data.network.CloudflareLocations
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class TcpProxyServer {

    private val _proxyStatus = MutableStateFlow(ProxyStatus())
    val proxyStatus: StateFlow<ProxyStatus> = _proxyStatus.asStateFlow()

    private var serverJob: Job? = null
    private var healthCheckJob: Job? = null
    private var serverSocket: ServerSocket? = null

    private val activeConnections = AtomicInteger(0)
    private val totalBytesTransferred = AtomicLong(0L)

    private val ipPool = mutableListOf<ScannedIp>()
    private var currentIndex = 0
    private val logs = ConcurrentBoundedList<String>(80)

    fun startServer(
        scope: CoroutineScope,
        localPort: Int = 1234,
        targetPort: Int = 443,
        initialIps: List<ScannedIp>,
        maxDelayMs: Int = 300,
        domain: String = "cloudflaremirrors.com/debian",
        useTls: Boolean = true
    ) {
        if (_proxyStatus.value.isRunning) return

        ipPool.clear()
        ipPool.addAll(initialIps)
        currentIndex = 0

        if (ipPool.isEmpty()) {
            addLog("Error: IP Pool is empty. Please run IP scanner first.")
            return
        }

        val primaryIp = ipPool[0]
        val loc = CloudflareLocations.getLocation(primaryIp.dataCenter)

        _proxyStatus.value = ProxyStatus(
            isRunning = true,
            localPort = localPort,
            localAddr = "127.0.0.1:$localPort",
            activeConnections = 0,
            activeTargetIp = primaryIp.ip,
            activeColo = primaryIp.dataCenter,
            activeCity = loc.city,
            activeLatencyMs = primaryIp.latencyMs,
            totalBytesTransferred = 0L,
            logMessages = logs.toList(),
            targetPoolSize = ipPool.size
        )

        addLog("Proxy listening on 127.0.0.1:$localPort -> Target pool: ${ipPool.size} IPs")
        addLog("Active Target: ${primaryIp.ip} [Colo: ${primaryIp.dataCenter}, Latency: ${primaryIp.latencyMs}ms]")

        serverJob = scope.launch(Dispatchers.IO) {
            try {
                val socket = ServerSocket()
                socket.reuseAddress = true
                socket.bind(InetSocketAddress("127.0.0.1", localPort))
                serverSocket = socket

                while (_proxyStatus.value.isRunning && !socket.isClosed) {
                    val clientSocket = socket.accept()
                    val clientAddr = clientSocket.remoteSocketAddress.toString()
                    val count = activeConnections.incrementAndGet()
                    updateStatus()

                    addLog("Client $clientAddr connected. Active connections: $count")

                    val currentIp = _proxyStatus.value.activeTargetIp
                    scope.launch(Dispatchers.IO) {
                        handleConnection(clientSocket, currentIp, targetPort, maxDelayMs)
                    }
                }
            } catch (e: Exception) {
                if (_proxyStatus.value.isRunning) {
                    addLog("Server socket error: ${e.message}")
                    _proxyStatus.value = _proxyStatus.value.copy(isRunning = false)
                }
            }
        }

        // Start background health checker & IP failover loop
        healthCheckJob = scope.launch(Dispatchers.IO) {
            while (_proxyStatus.value.isRunning) {
                delay(8000) // check every 8 seconds
                checkActiveIpHealth(targetPort, maxDelayMs, domain, useTls)
            }
        }
    }

    fun stopServer() {
        _proxyStatus.value = _proxyStatus.value.copy(isRunning = false)
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverJob?.cancel()
        healthCheckJob?.cancel()
        activeConnections.set(0)
        addLog("TCP Proxy Server Stopped.")
        updateStatus()
    }

    fun switchTargetIpManually(targetIp: ScannedIp) {
        val foundIndex = ipPool.indexOfFirst { it.ip == targetIp.ip }
        if (foundIndex != -1) {
            currentIndex = foundIndex
        } else {
            ipPool.add(0, targetIp)
            currentIndex = 0
        }
        val loc = CloudflareLocations.getLocation(targetIp.dataCenter)
        _proxyStatus.value = _proxyStatus.value.copy(
            activeTargetIp = targetIp.ip,
            activeColo = targetIp.dataCenter,
            activeCity = loc.city,
            activeLatencyMs = targetIp.latencyMs
        )
        addLog("Manually switched target to ${targetIp.ip} [${targetIp.dataCenter}]")
    }

    private suspend fun handleConnection(
        clientSocket: Socket,
        targetIp: String,
        targetPort: Int,
        maxDelayMs: Int
    ) = withContext(Dispatchers.IO) {
        var targetSocket: Socket? = null
        try {
            targetSocket = Socket()
            targetSocket.connect(InetSocketAddress(targetIp, targetPort), maxDelayMs.coerceAtLeast(1500))

            val clientIn = clientSocket.getInputStream()
            val clientOut = clientSocket.getOutputStream()
            val targetIn = targetSocket.getInputStream()
            val targetOut = targetSocket.getOutputStream()

            val job1 = launch(Dispatchers.IO) { copyStream(clientIn, targetOut) }
            val job2 = launch(Dispatchers.IO) { copyStream(targetIn, clientOut) }

            job1.join()
            job2.join()
        } catch (e: Exception) {
            addLog("Forwarding error ($targetIp): ${e.message}")
        } finally {
            try { clientSocket.close() } catch (_: Exception) {}
            try { targetSocket?.close() } catch (_: Exception) {}
            val remaining = activeConnections.decrementAndGet().coerceAtLeast(0)
            updateStatus()
            addLog("Connection closed. Active: $remaining")
        }
    }

    private fun copyStream(input: InputStream, output: OutputStream) {
        val buffer = ByteArray(8192)
        try {
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
                output.flush()
                totalBytesTransferred.addAndGet(bytesRead.toLong())
                updateStatus()
            }
        } catch (_: Exception) {}
    }

    private fun checkActiveIpHealth(
        targetPort: Int,
        maxDelayMs: Int,
        domain: String,
        useTls: Boolean
    ) {
        val currentIp = _proxyStatus.value.activeTargetIp
        if (currentIp.isEmpty()) return

        val startTime = System.currentTimeMillis()
        var healthy = false
        var latency = 0L
        var socket: Socket? = null

        try {
            socket = Socket()
            socket.connect(InetSocketAddress(currentIp, targetPort), maxDelayMs.coerceAtLeast(1000))
            latency = System.currentTimeMillis() - startTime
            healthy = latency <= maxDelayMs * 2
        } catch (e: Exception) {
            healthy = false
        } finally {
            try { socket?.close() } catch (_: Exception) {}
        }

        if (healthy) {
            _proxyStatus.value = _proxyStatus.value.copy(activeLatencyMs = latency)
        } else {
            addLog("Active IP $currentIp health check failed (> ${maxDelayMs}ms or timeout). Triggering failover...")
            switchToNextValidIP(targetPort, maxDelayMs)
        }
    }

    private fun switchToNextValidIP(targetPort: Int, maxDelayMs: Int) {
        if (ipPool.isEmpty()) return

        for (i in (currentIndex + 1) until ipPool.size) {
            val candidate = ipPool[i]
            if (testPing(candidate.ip, targetPort, maxDelayMs)) {
                currentIndex = i
                val loc = CloudflareLocations.getLocation(candidate.dataCenter)
                _proxyStatus.value = _proxyStatus.value.copy(
                    activeTargetIp = candidate.ip,
                    activeColo = candidate.dataCenter,
                    activeCity = loc.city,
                    activeLatencyMs = candidate.latencyMs
                )
                addLog("Failover success! Switched to new valid IP: ${candidate.ip} [${candidate.dataCenter}]")
                return
            }
        }

        addLog("All IPs in current pool checked. Looping back to primary IP.")
        if (ipPool.isNotEmpty()) {
            currentIndex = 0
            val candidate = ipPool[0]
            val loc = CloudflareLocations.getLocation(candidate.dataCenter)
            _proxyStatus.value = _proxyStatus.value.copy(
                activeTargetIp = candidate.ip,
                activeColo = candidate.dataCenter,
                activeCity = loc.city,
                activeLatencyMs = candidate.latencyMs
            )
        }
    }

    private fun testPing(ip: String, port: Int, timeoutMs: Int): Boolean {
        var socket: Socket? = null
        return try {
            socket = Socket()
            socket.connect(InetSocketAddress(ip, port), timeoutMs.coerceAtLeast(1000))
            true
        } catch (e: Exception) {
            false
        } finally {
            try { socket?.close() } catch (_: Exception) {}
        }
    }

    private fun addLog(msg: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        logs.add("[$timestamp] $msg")
        updateStatus()
    }

    private fun updateStatus() {
        _proxyStatus.value = _proxyStatus.value.copy(
            activeConnections = activeConnections.get(),
            totalBytesTransferred = totalBytesTransferred.get(),
            logMessages = logs.toList(),
            targetPoolSize = ipPool.size
        )
    }

    private class ConcurrentBoundedList<T>(private val maxSize: Int) {
        private val list = java.util.Collections.synchronizedList(ArrayList<T>())

        fun add(element: T) {
            list.add(element)
            while (list.size > maxSize) {
                list.removeAt(0)
            }
        }

        fun toList(): List<T> {
            return synchronized(list) { ArrayList(list) }
        }
    }
}
