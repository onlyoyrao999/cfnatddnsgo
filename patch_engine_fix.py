import re

with open('./app/src/main/java/com/example/service/IpScannerEngine.kt', 'r') as f:
    content = f.read()

# Fix the first _progressState.value assignment
content = re.sub(
    r'_progressState.value = ScanProgressState\(\s*isScanning = true,\s*statusMessage = "正在加载 Cloudflare 数据中心和子网\.\.\."\s*\)',
    r'_progressState.value = _progressState.value.copy(\n            isScanning = true,\n            statusMessage = "正在加载 Cloudflare 数据中心和子网..."\n        )',
    content
)

# And the second _progressState.value assignment (正在生成目标 IP 地址)
content = re.sub(
    r'_progressState.value = _progressState.value.copy\(\s*statusMessage = "正在生成目标 IP 地址\.\.\."\s*\)',
    r'_progressState.value = _progressState.value.copy(\n            statusMessage = "正在生成目标 IP 地址..."\n        )',
    content
)

# We also need to fix: _progressState.value = ScanProgressState( at line 105 to be a copy or just pass existingResults properly. Actually it's calling `ScanProgressState(...)` directly.
content = re.sub(
    r'_progressState\.value = ScanProgressState\(\n            isScanning = true,\n            scannedCount = 0,\n            totalCount = total,\n            validCount = existingResults\.size,\n            progressPercentage = 0f,\n            results = existingResults,\n            statusMessage = (.*?)\n        \)',
    r'_progressState.value = _progressState.value.copy(\n            isScanning = true,\n            scannedCount = 0,\n            totalCount = total,\n            validCount = existingResults.size,\n            progressPercentage = 0f,\n            results = existingResults,\n            statusMessage = \1\n        )',
    content
)

# Same for final result update
content = re.sub(
    r'_progressState\.value = ScanProgressState\(\n            isScanning = false,\n            scannedCount = total,\n            totalCount = total,\n            validCount = finalResults\.size,\n            progressPercentage = 1f,\n            results = finalResults,\n            statusMessage = (.*?)\n        \)',
    r'_progressState.value = _progressState.value.copy(\n            isScanning = false,\n            scannedCount = total,\n            totalCount = total,\n            validCount = finalResults.size,\n            progressPercentage = 1f,\n            results = finalResults,\n            statusMessage = \1\n        )',
    content
)

with open('./app/src/main/java/com/example/service/IpScannerEngine.kt', 'w') as f:
    f.write(content)
