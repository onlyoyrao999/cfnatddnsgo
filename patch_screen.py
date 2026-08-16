import re

with open('./app/src/main/java/com/example/ui/screens/ScannerScreen.kt', 'r') as f:
    content = f.read()

# IP Count Slider replacement
ip_count_old = """
                    Text("目标 IP 数量", style = MaterialTheme.typography.bodyMedium.copy(color = OffWhiteText))
                    Text("${scanConfig.ipCount} IPs", style = MaterialTheme.typography.bodyMedium.copy(color = CfOrangePrimary, fontWeight = FontWeight.Bold))
                }
                Slider(
                    value = scanConfig.ipCount.toFloat(),
                    onValueChange = { onConfigChange(scanConfig.copy(ipCount = it.toInt())) },
                    valueRange = 10f..3000f,
                    colors = SliderDefaults.colors(thumbColor = CfOrangePrimary, activeTrackColor = CfOrangePrimary)
                )
            }
"""

ip_count_new = """
                    Text("目标 IP 数量", style = MaterialTheme.typography.bodyMedium.copy(color = OffWhiteText))
                    Text(if (scanConfig.ipCount >= 10000) "无限 (达标即停)" else "${scanConfig.ipCount} IPs", style = MaterialTheme.typography.bodyMedium.copy(color = CfOrangePrimary, fontWeight = FontWeight.Bold))
                }
                Slider(
                    value = scanConfig.ipCount.toFloat(),
                    onValueChange = { onConfigChange(scanConfig.copy(ipCount = it.toInt())) },
                    valueRange = 100f..10000f,
                    colors = SliderDefaults.colors(thumbColor = CfOrangePrimary, activeTrackColor = CfOrangePrimary)
                )
            }
            
            // maxPerColo Slider
            Spacer(modifier = Modifier.height(16.dp))
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("每个地区合格数", style = MaterialTheme.typography.bodyMedium.copy(color = OffWhiteText))
                    Text("${scanConfig.maxPerColo} 个", style = MaterialTheme.typography.bodyMedium.copy(color = CfOrangePrimary, fontWeight = FontWeight.Bold))
                }
                Slider(
                    value = scanConfig.maxPerColo.toFloat(),
                    onValueChange = { onConfigChange(scanConfig.copy(maxPerColo = it.toInt())) },
                    valueRange = 1f..20f,
                    steps = 18,
                    colors = SliderDefaults.colors(thumbColor = CfOrangePrimary, activeTrackColor = CfOrangePrimary)
                )
            }
"""

content = content.replace(ip_count_old.strip(), ip_count_new.strip())

with open('./app/src/main/java/com/example/ui/screens/ScannerScreen.kt', 'w') as f:
    f.write(content)
