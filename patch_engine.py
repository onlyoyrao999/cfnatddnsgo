import re

with open('./app/src/main/java/com/example/service/IpScannerEngine.kt', 'r') as f:
    content = f.read()

# Add setInitialResults
initial_method = """
    fun setInitialResults(initialResults: List<ScannedIp>) {
        if (!_progressState.value.isScanning && _progressState.value.results.isEmpty()) {
            _progressState.value = _progressState.value.copy(
                results = initialResults,
                statusMessage = "就绪"
            )
        }
    }
"""
if "fun setInitialResults" not in content:
    content = content.replace("fun stopScan() {", initial_method + "\n    fun stopScan() {")

# Update startScan to NOT clear results and to seed resultsQueue
content = re.sub(
    r'val resultsQueue = ConcurrentLinkedQueue<ScannedIp>\(\)',
    r'val existingResults = _progressState.value.results\n        val resultsQueue = ConcurrentLinkedQueue<ScannedIp>(existingResults)',
    content
)

content = re.sub(
    r'isScanning = true,\s*scannedCount = 0,\s*totalCount = total,\s*validCount = 0,\s*progressPercentage = 0f,\s*results = emptyList\(\),',
    r'isScanning = true,\n            scannedCount = 0,\n            totalCount = total,\n            validCount = existingResults.size,\n            progressPercentage = 0f,\n            results = existingResults,',
    content
)

with open('./app/src/main/java/com/example/service/IpScannerEngine.kt', 'w') as f:
    f.write(content)
