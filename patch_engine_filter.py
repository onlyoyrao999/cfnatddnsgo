import re

with open('./app/src/main/java/com/example/service/IpScannerEngine.kt', 'r') as f:
    content = f.read()

# Modify existingResults assignment
old_existing = r'val existingResults = _progressState\.value\.results'
new_existing = """val existingResults = if (isAllRegions) {
            _progressState.value.results
        } else {
            _progressState.value.results.filter { ip ->
                filters.any { filter -> ip.dataCenter.equals(filter, ignoreCase = true) }
            }
        }"""

content = re.sub(old_existing, new_existing, content)

with open('./app/src/main/java/com/example/service/IpScannerEngine.kt', 'w') as f:
    f.write(content)
