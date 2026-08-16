import re

with open('./app/src/main/java/com/example/service/IpScannerEngine.kt', 'r') as f:
    content = f.read()

# Replace existingResults assignment
content = content.replace(
    'val existingResults = _progressState.value.results',
    'val existingResults = emptyList<ScannedIp>()'
)

with open('./app/src/main/java/com/example/service/IpScannerEngine.kt', 'w') as f:
    f.write(content)
