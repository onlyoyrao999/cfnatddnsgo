import re

with open('./app/src/main/java/com/example/service/IpScannerEngine.kt', 'r') as f:
    content = f.read()

# We need to replace everything from `val actualGenerateCount` down to the end of `startScan`
start_marker = r'val actualGenerateCount'
end_marker = r'private fun testIpAddress'

match = re.search(f'({start_marker}.*?)(?=\n    {end_marker})', content, re.DOTALL)
if match:
    old_body = match.group(1)
    
    new_body = """val filters = if (config.coloFilter.isNotBlank()) {
            config.coloFilter.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() }
        } else emptyList()
        val isAllRegions = filters.isEmpty() || filters.contains("ALL")
        val maxPerColo = config.maxPerColo
        val coloCounts = ConcurrentHashMap<String, AtomicInteger>()

        val goalMet = java.util.concurrent.atomic.AtomicBoolean(false)
        val scannedCounter = AtomicInteger(0)
        val validCounter = AtomicInteger(0)
        val existingResults = _progressState.value.results
        val resultsQueue = ConcurrentLinkedQueue<ScannedIp>(existingResults)
        
        var batchCount = 0

        while (!isCancelled && !goalMet.get()) {
            val candidateIps = if (isIpv6) {
                IpGenerator.getRandomIPv6s(cidrs, count = config.ipCount)
            } else {
                IpGenerator.getRandomIPv4s(cidrs, count = config.ipCount)
            }

            if (candidateIps.isEmpty()) {
                if (batchCount == 0) {
                    _progressState.value = ScanProgressState(
                        isScanning = false,
                        statusMessage = "未生成 IP 地址。"
                    )
                }
                break
            }

            batchCount++
            val total = candidateIps.size
            scannedCounter.set(0)

            _progressState.value = _progressState.value.copy(
                isScanning = true,
                scannedCount = 0,
                totalCount = total,
                validCount = resultsQueue.size,
                progressPercentage = 0f,
                results = existingResults,
                statusMessage = if (isAllRegions) "正在使用 ${config.maxThreads} 个线程扫描 $total 个 IP..." else "正在扫描(第 $batchCount 批)..."
            )

            val semaphore = Semaphore(config.maxThreads.coerceIn(5, 200))

            coroutineScope {
                candidateIps.forEach { ipAddr ->
                    if (isCancelled || goalMet.get()) return@forEach

                    launch {
                        semaphore.withPermit {
                            if (isCancelled || goalMet.get()) return@withPermit

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

                                    if (coloCount.get() < maxPerColo) {
                                        coloCount.incrementAndGet()
                                        validCounter.incrementAndGet()
                                        resultsQueue.add(scannedIp)

                                        if (!isAllRegions && filters.isNotEmpty()) {
                                            val allMet = filters.all { f ->
                                                (coloCounts[f.uppercase()]?.get() ?: 0) >= maxPerColo
                                            }
                                            if (allMet) {
                                                goalMet.set(true)
                                            }
                                        }
                                    }
                                }
                            }

                            // Periodically update UI
                            if (currentScanned % 5 == 0 || currentScanned == total) {
                                val uniqueSorted = resultsQueue.distinctBy { it.ip }.sortedBy { it.latencyMs }
                                val validList = uniqueSorted.groupBy { it.dataCenter.uppercase() }
                                    .flatMap { it.value.take(maxPerColo) }
                                    .sortedBy { it.latencyMs }
                                val pct = currentScanned.toFloat() / total.toFloat()

                                _progressState.value = ScanProgressState(
                                    isScanning = !isCancelled && currentScanned < total,
                                    scannedCount = currentScanned,
                                    totalCount = total,
                                    validCount = validList.size,
                                    progressPercentage = pct,
                                    results = validList.take(config.ipCount * 2),
                                    statusMessage = if (currentScanned < total) "已扫描 $currentScanned / $total (${validList.size} 个有效)" else "完成批次扫描"
                                )
                            }
                        }
                    }
                }
            }

            if (isAllRegions) break // Only scan once if ALL is selected
        }

        val finalUniqueSorted = resultsQueue.distinctBy { it.ip }.sortedBy { it.latencyMs }
        val finalResults = finalUniqueSorted.groupBy { it.dataCenter.uppercase() }
            .flatMap { it.value.take(maxPerColo) }
            .sortedBy { it.latencyMs }
            
        _progressState.value = _progressState.value.copy(
            isScanning = false,
            scannedCount = finalResults.size,
            totalCount = finalResults.size,
            validCount = finalResults.size,
            progressPercentage = 1f,
            results = finalResults,
            statusMessage = "扫描完成！提取了 ${finalResults.size} 个最佳 IP。"
        )
"""
    content = content.replace(old_body, new_body)
    
    with open('./app/src/main/java/com/example/service/IpScannerEngine.kt', 'w') as f:
        f.write(content)
else:
    print("Failed to match")
