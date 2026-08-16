fun main() {
    val currentFilters = mutableSetOf("FRA", "HKG")
    val colo = "ALL"
    if (currentFilters.contains(colo)) {
        currentFilters.remove(colo)
    } else {
        currentFilters.add(colo)
    }
    
    val currentFiltersStr = currentFilters.joinToString(",")
    println("Filter string: $currentFiltersStr")
    
    val parsedFilters = currentFiltersStr.split(",").map { it.trim().uppercase() }.filter { it.isNotBlank() }
    val isFraSelected = if ("FRA" == "ALL") parsedFilters.isEmpty() || parsedFilters.contains("ALL") else parsedFilters.contains("FRA")
    val isAllSelected = if ("ALL" == "ALL") parsedFilters.isEmpty() || parsedFilters.contains("ALL") else parsedFilters.contains("ALL")
    
    println("FRA selected: $isFraSelected")
    println("ALL selected: $isAllSelected")
}
