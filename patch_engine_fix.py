import re

with open('./app/src/main/java/com/example/service/IpScannerEngine.kt', 'r') as f:
    content = f.read()

content = content.replace('''        val existingResults = emptyList<ScannedIp>()
            }
        }
                val resultsQueue = ConcurrentLinkedQueue<ScannedIp>(existingResults)''', '''        val existingResults = emptyList<ScannedIp>()
        val resultsQueue = ConcurrentLinkedQueue<ScannedIp>(existingResults)''')

with open('./app/src/main/java/com/example/service/IpScannerEngine.kt', 'w') as f:
    f.write(content)
