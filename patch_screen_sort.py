import re

with open('./app/src/main/java/com/example/ui/screens/ScannerScreen.kt', 'r') as f:
    content = f.read()

old_sort = """    val dynamicColos = remember(discoveredColos) {
        discoveredColos.sorted()
    }
       
    val presets = remember(dynamicColos) {
        listOf("ALL") + dynamicColos
    }"""

new_sort = """    val dynamicColos = remember(discoveredColos, scanConfig.coloFilter) {
        val currentFilters = scanConfig.coloFilter.split(",")
            .map { it.trim().uppercase() }
            .filter { it.isNotBlank() && it != "ALL" }
            .toSet()

        val defaultList = listOf("FRA", "HAM", "HKG", "LAX", "LHR", "SJC", "SIN", "NRT", "CDG")
        val allAvailable = (defaultList + discoveredColos).distinct()

        allAvailable.sortedWith { a, b ->
            val aSelected = currentFilters.contains(a)
            val bSelected = currentFilters.contains(b)
            if (aSelected && !bSelected) -1
            else if (!aSelected && bSelected) 1
            else a.compareTo(b)
        }
    }
       
    val presets = remember(dynamicColos) {
        listOf("ALL") + dynamicColos
    }"""

content = content.replace(old_sort, new_sort)

with open('./app/src/main/java/com/example/ui/screens/ScannerScreen.kt', 'w') as f:
    f.write(content)
