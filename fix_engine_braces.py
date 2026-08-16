with open('./app/src/main/java/com/example/service/IpScannerEngine.kt', 'r') as f:
    content = f.read()

# Let's completely replace the entire class body below init to be sure.
# No, let's just locate the "statusMessage = "扫描完成！提取了 ${finalResults.size} 个最佳 IP。" )" part
import re
match = re.search(r'statusMessage = "扫描完成！提取了 \$\{finalResults\.size\} 个最佳 IP。"\n\s*\)', content)
if match:
    idx = match.end()
    # Insert two braces after this
    content = content[:idx] + "\n    }\n" + content[idx:]
    with open('./app/src/main/java/com/example/service/IpScannerEngine.kt', 'w') as f:
        f.write(content)
else:
    print("Match failed")
