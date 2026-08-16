import re

with open('./app/src/main/java/com/example/service/IpScannerEngine.kt', 'r') as f:
    content = f.read()

# Pre-populate coloCounts
prepopulate = """        val resultsQueue = ConcurrentLinkedQueue<ScannedIp>(existingResults)
        
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
        }
"""
content = re.sub(
    r'val resultsQueue = ConcurrentLinkedQueue<ScannedIp>\(existingResults\)',
    prepopulate,
    content
)

with open('./app/src/main/java/com/example/service/IpScannerEngine.kt', 'w') as f:
    f.write(content)
