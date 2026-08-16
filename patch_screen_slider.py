import re

with open('./app/src/main/java/com/example/ui/screens/ScannerScreen.kt', 'r') as f:
    content = f.read()

# Replace the slider code
old_ip_count = r'Text\(if \(scanConfig\.ipCount >= 10000\) "无限 \(达标即停\)" else "\$\{scanConfig\.ipCount\} IPs", style = MaterialTheme\.typography\.bodyMedium\.copy\(color = CfOrangePrimary, fontWeight = FontWeight\.Bold\)\)\n\s*\}\n\s*Slider\(\n\s*value = scanConfig\.ipCount\.toFloat\(\),\n\s*onValueChange = \{ onConfigChange\(scanConfig\.copy\(ipCount = it\.toInt\(\)\)\) \},\n\s*valueRange = 100f\.\.10000f,\n\s*colors = SliderDefaults\.colors\(thumbColor = CfOrangePrimary, activeTrackColor = CfOrangePrimary\)\n\s*\)'

new_ip_count = """Text("${scanConfig.ipCount} IPs", style = MaterialTheme.typography.bodyMedium.copy(color = CfOrangePrimary, fontWeight = FontWeight.Bold))
                }
                Slider(
                    value = scanConfig.ipCount.toFloat(),
                    onValueChange = { onConfigChange(scanConfig.copy(ipCount = it.toInt())) },
                    valueRange = 100f..3000f,
                    colors = SliderDefaults.colors(thumbColor = CfOrangePrimary, activeTrackColor = CfOrangePrimary)
                )"""

content = re.sub(old_ip_count, new_ip_count, content)

with open('./app/src/main/java/com/example/ui/screens/ScannerScreen.kt', 'w') as f:
    f.write(content)
