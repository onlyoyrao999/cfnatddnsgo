import re

with open('./app/src/main/java/com/example/service/IpScannerEngine.kt', 'r') as f:
    content = f.read()

# Update generation count
generation_new = """
        val actualGenerateCount = if (config.ipCount >= 10000) 100000 else config.ipCount
        val candidateIps = if (isIpv6) {
            IpGenerator.getRandomIPv6s(cidrs, count = actualGenerateCount)
        } else {
            IpGenerator.getRandomIPv4s(cidrs, count = actualGenerateCount)
        }
"""
content = re.sub(
    r'val candidateIps = if \(isIpv6\) \{\n            IpGenerator\.getRandomIPv6s\(cidrs, count = config\.ipCount\)\n        \} else \{\n            IpGenerator\.getRandomIPv4s\(cidrs, count = config\.ipCount\)\n        \}',
    generation_new.strip(),
    content
)

# Update maxPerColo
content = re.sub(
    r'val maxPerColo = 10',
    r'val maxPerColo = config.maxPerColo',
    content
)

# Insert goalMet logic
loop_head = """
        @Volatile var goalMet = false
        coroutineScope {
            candidateIps.forEach { ipAddr ->
                if (isCancelled || goalMet) return@forEach
"""
content = re.sub(
    r'coroutineScope \{\n            candidateIps\.forEach \{ ipAddr ->\n                if \(isCancelled\) return@forEach',
    loop_head.strip(),
    content
)

semaphore_head = """
                    semaphore.withPermit {
                        if (isCancelled || goalMet) return@withPermit
"""
content = re.sub(
    r'semaphore\.withPermit \{\n                        if \(isCancelled\) return@withPermit',
    semaphore_head.strip(),
    content
)

check_logic = """
                                // Limit to maxPerColo if in ALL mode (or any mode now)
                                if (coloCount.get() < maxPerColo) {
                                    coloCount.incrementAndGet()
                                    validCounter.incrementAndGet()
                                    resultsQueue.add(scannedIp)
                                    
                                    // Check if goal is met for auto-stop in Infinite mode
                                    if (config.ipCount >= 10000 && !isAllRegions && filters.isNotEmpty()) {
                                        val allMet = filters.all { f ->
                                            (coloCounts[f.uppercase()]?.get() ?: 0) >= maxPerColo
                                        }
                                        if (allMet) {
                                            goalMet = true
                                        }
                                    }
                                }
"""
content = re.sub(
    r'if \(coloCount\.get\(\) < maxPerColo\) \{\n                                    coloCount\.incrementAndGet\(\)\n                                    validCounter\.incrementAndGet\(\)\n                                    resultsQueue\.add\(scannedIp\)\n                                \}',
    check_logic.strip(),
    content
)

with open('./app/src/main/java/com/example/service/IpScannerEngine.kt', 'w') as f:
    f.write(content)
