import re

with open('./app/src/main/java/com/example/service/IpScannerEngine.kt', 'r') as f:
    content = f.read()

# Add Context import if not present
if "import android.content.Context" not in content:
    content = content.replace("import com.example.data.model.ScanConfig", "import android.content.Context\nimport com.example.data.model.ScanConfig")

# Update class constructor
content = re.sub(r'class IpScannerEngine \{', r'class IpScannerEngine(private val context: Context) {', content)

with open('./app/src/main/java/com/example/service/IpScannerEngine.kt', 'w') as f:
    f.write(content)
