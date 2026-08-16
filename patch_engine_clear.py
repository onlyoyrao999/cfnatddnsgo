import re

with open('./app/src/main/java/com/example/service/IpScannerEngine.kt', 'r') as f:
    content = f.read()

# Clear results when scan starts
content = re.sub(
    r'isCancelled = false\n        _progressState\.value = _progressState\.value\.copy\(\n            isScanning = true,\n            statusMessage = "正在加载 Cloudflare 数据中心和子网\.\.\."\n        \)',
    'isCancelled = false\n        _progressState.value = _progressState.value.copy(\n            isScanning = true,\n            statusMessage = "正在加载 Cloudflare 数据中心和子网...",\n            results = emptyList(),\n            scannedCount = 0,\n            validCount = 0,\n            progressPercentage = 0f\n        )',
    content
)

# Also fix the existingResults logic
content = re.sub(
    r'val existingResults = if \(isAllRegions\) \{.*?\} else \{.*?\}\n',
    'val existingResults = emptyList<ScannedIp>()\n',
    content,
    flags=re.DOTALL
)

with open('./app/src/main/java/com/example/service/IpScannerEngine.kt', 'w') as f:
    f.write(content)
