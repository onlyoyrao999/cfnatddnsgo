import re

with open('./app/src/main/java/com/example/service/IpScannerEngine.kt', 'r') as f:
    content = f.read()

# Replace the emptyList logic and remove the pre-populate and goal check
old_block = """        val existingResults = emptyList<ScannedIp>()
        val resultsQueue = ConcurrentLinkedQueue<ScannedIp>(existingResults)
        
        // Pre-populate coloCounts so we know how many we already have
        existingResults.forEach { ip ->
            val colo = ip.dataCenter.uppercase()
            coloCounts.getOrPut(colo) { AtomicInteger(0) }.incrementAndGet()
        }
        
        // If we already met the goal before scanning, don't scan
        if (!isAllRegions && filters.isNotEmpty()) {
            val allMet = filters.all { f ->
                (coloCounts[f.uppercase()]?.get() ?: 0) >= maxPerColo
            }
            if (allMet) {
                goalMet.set(true)
            }
        }"""

new_block = """        val existingResults = if (isAllRegions) {
            _progressState.value.results
        } else {
            _progressState.value.results.filter { ip ->
                filters.any { filter -> ip.dataCenter.equals(filter, ignoreCase = true) }
            }
        }
        val resultsQueue = ConcurrentLinkedQueue<ScannedIp>(existingResults)
        
        // Update the UI immediately to show only the filtered results
        _progressState.value = _progressState.value.copy(
            results = existingResults,
            validCount = existingResults.size
        )"""

content = content.replace(old_block, new_block)

with open('./app/src/main/java/com/example/service/IpScannerEngine.kt', 'w') as f:
    f.write(content)
