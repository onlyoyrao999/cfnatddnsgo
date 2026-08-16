import re

with open('./app/src/main/java/com/example/ui/screens/ScannerScreen.kt', 'r') as f:
    content = f.read()

# Add verticalScroll to ScanConfigCard Column
content = re.sub(
    r'Column\(\n\s*modifier = Modifier\.padding\(16\.dp\),\n\s*verticalArrangement = Arrangement\.spacedBy\(12\.dp\)\n\s*\)',
    r'Column(\n            modifier = Modifier\n                .verticalScroll(rememberScrollState())\n                .padding(16.dp),\n            verticalArrangement = Arrangement.spacedBy(12.dp)\n        )',
    content
)

with open('./app/src/main/java/com/example/ui/screens/ScannerScreen.kt', 'w') as f:
    f.write(content)
