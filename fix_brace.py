import re

with open('./app/src/main/java/com/example/service/IpScannerEngine.kt', 'r') as f:
    content = f.read()

content = content.replace('statusMessage = "扫描完成！提取了 ${finalResults.size} 个最佳 IP。"\n        )\n    private fun testIpAddress(', 'statusMessage = "扫描完成！提取了 ${finalResults.size} 个最佳 IP。"\n        )\n    }\n\n    private fun testIpAddress(')

with open('./app/src/main/java/com/example/service/IpScannerEngine.kt', 'w') as f:
    f.write(content)
