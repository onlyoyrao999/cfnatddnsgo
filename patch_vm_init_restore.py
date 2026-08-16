import re

with open('./app/src/main/java/com/example/ui/MainViewModel.kt', 'r') as f:
    content = f.read()

init_block = """
    init {
        viewModelScope.launch {
            repository.savedIps.collect { savedEntities ->
                if (!scannerEngine.progressState.value.isScanning && scannerEngine.progressState.value.results.isEmpty()) {
                    val initialIps = savedEntities.map { entity ->
                        ScannedIp(
                            ip = entity.ip,
                            dataCenter = entity.dataCenter,
                            region = entity.region,
                            city = entity.city,
                            latencyMs = entity.latencyMs,
                            testedAt = entity.testedAt,
                            ipVersion = entity.ipVersion
                        )
                    }
                    scannerEngine.setInitialResults(initialIps)
                }
            }
        }
    }
"""

# Insert right after `val isAutoSyncEnabled: StateFlow<Boolean> = _isAutoSyncEnabled.asStateFlow()`
target = 'val isAutoSyncEnabled: StateFlow<Boolean> = _isAutoSyncEnabled.asStateFlow()'
content = content.replace(target, target + '\n' + init_block)

with open('./app/src/main/java/com/example/ui/MainViewModel.kt', 'w') as f:
    f.write(content)
