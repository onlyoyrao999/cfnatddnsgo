import re

with open('./app/src/main/java/com/example/service/IpScannerEngine.kt', 'r') as f:
    content = f.read()

content = re.sub(
    r'"已扫描 \$currentScanned / \$total \(\$\{validList\.size\} 个有效\)"',
    r'if (isAllRegions) "已扫描 $currentScanned / $total (${validList.size} 个有效)" else "第 $batchCount 批: 已扫描 $currentScanned / $total (${validList.size} 个有效)"',
    content
)

with open('./app/src/main/java/com/example/service/IpScannerEngine.kt', 'w') as f:
    f.write(content)
