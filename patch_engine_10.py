import re

with open('./app/src/main/java/com/example/service/IpScannerEngine.kt', 'r') as f:
    content = f.read()

# Modify the queue addition limit
content = re.sub(
    r'if \(!isAllRegions \|\| coloCount\.get\(\) < maxPerColo\) \{',
    r'if (coloCount.get() < maxPerColo) {',
    content
)

# Modify periodic UI updates to keep max 10 per region
ui_update_logic = """
                            val uniqueSorted = resultsQueue.distinctBy { it.ip }.sortedBy { it.latencyMs }
                            val validList = uniqueSorted.groupBy { it.dataCenter.uppercase() }
                                .flatMap { it.value.take(maxPerColo) }
                                .sortedBy { it.latencyMs }
"""
content = re.sub(
    r'val validList = resultsQueue\.distinctBy \{ it\.ip \}\.sortedBy \{ it\.latencyMs \}',
    ui_update_logic.strip(),
    content
)

# Modify final results to keep max 10 per region
final_results_logic = """
        val finalUniqueSorted = resultsQueue.distinctBy { it.ip }.sortedBy { it.latencyMs }
        val finalResults = finalUniqueSorted.groupBy { it.dataCenter.uppercase() }
            .flatMap { it.value.take(maxPerColo) }
            .sortedBy { it.latencyMs }
"""
content = re.sub(
    r'val finalResults = resultsQueue\.distinctBy \{ it\.ip \}\.sortedBy \{ it\.latencyMs \}\.take\(config\.ipCount\)',
    final_results_logic.strip(),
    content
)

with open('./app/src/main/java/com/example/service/IpScannerEngine.kt', 'w') as f:
    f.write(content)
