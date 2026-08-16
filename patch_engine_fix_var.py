import re

with open('./app/src/main/java/com/example/service/IpScannerEngine.kt', 'r') as f:
    content = f.read()

content = re.sub(
    r'@Volatile var goalMet = false',
    r'val goalMet = java.util.concurrent.atomic.AtomicBoolean(false)',
    content
)

content = re.sub(
    r'if \(isCancelled \|\| goalMet\) return@',
    r'if (isCancelled || goalMet.get()) return@',
    content
)

content = re.sub(
    r'goalMet = true',
    r'goalMet.set(true)',
    content
)

with open('./app/src/main/java/com/example/service/IpScannerEngine.kt', 'w') as f:
    f.write(content)
