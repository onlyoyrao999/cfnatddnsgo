import re

with open('./app/src/main/java/com/example/ui/screens/ScannerScreen.kt', 'r') as f:
    content = f.read()

imports = """import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
"""

content = content.replace("import androidx.compose.foundation.layout.Column", "import androidx.compose.foundation.layout.Column\n" + imports)

with open('./app/src/main/java/com/example/ui/screens/ScannerScreen.kt', 'w') as f:
    f.write(content)
