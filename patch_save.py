import re

with open('./app/src/main/java/com/example/ui/MainViewModel.kt', 'r') as f:
    content = f.read()

replacement = """
                val currentSaved = repository.savedIps.value
                val favoriteMap = currentSaved.associate { it.ip to it.isFavorite }

                // Save top scanned IPs into database automatically
                val entities = state.results.map { ip ->
                    ScannedIpEntity(
                        ip = ip.ip,
                        dataCenter = ip.dataCenter,
                        region = ip.region,
                        city = ip.city,
                        latencyMs = ip.latencyMs,
                        testedAt = ip.testedAt,
                        ipVersion = ip.ipVersion,
                        isFavorite = favoriteMap[ip.ip] ?: false,
                        port = config.port
                    )
                }
"""

content = re.sub(
    r'// Save top scanned IPs into database automatically\s*val entities = state\.results\.map \{ ip ->\s*ScannedIpEntity\(\s*ip = ip\.ip,\s*dataCenter = ip\.dataCenter,\s*region = ip\.region,\s*city = ip\.city,\s*latencyMs = ip\.latencyMs,\s*testedAt = ip\.testedAt,\s*ipVersion = ip\.ipVersion,\s*isFavorite = false,\s*port = config\.port\s*\)\s*\}',
    replacement.strip(),
    content
)

with open('./app/src/main/java/com/example/ui/MainViewModel.kt', 'w') as f:
    f.write(content)
