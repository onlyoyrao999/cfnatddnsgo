import re

with open("app/src/main/java/com/example/ui/MainViewModel.kt", "r") as f:
    content = f.read()

# Replace class definition
content = content.replace("class MainViewModel(\n    private val repository: IpRepository\n) : ViewModel() {",
"""import android.content.SharedPreferences\n\nclass MainViewModel(
    private val repository: IpRepository,
    private val prefs: SharedPreferences
) : ViewModel() {""")

# Add load/save functions and modify _scanConfig initialization
scan_config_init = """    private val _scanConfig = MutableStateFlow(ScanConfig())"""
scan_config_replacement = """    private fun loadScanConfig(): ScanConfig {
        return ScanConfig(
            ipType = prefs.getString("sc_ipType", "4") ?: "4",
            port = prefs.getInt("sc_port", 443),
            maxThreads = prefs.getInt("sc_maxThreads", 100),
            delayMs = prefs.getInt("sc_delayMs", 300),
            coloFilter = prefs.getString("sc_coloFilter", "") ?: "",
            domain = prefs.getString("sc_domain", "cloudflaremirrors.com/debian") ?: "cloudflaremirrors.com/debian",
            expectedCode = prefs.getInt("sc_expectedCode", 200),
            random = prefs.getBoolean("sc_random", true),
            ipCount = prefs.getInt("sc_ipCount", 1000),
            useTls = prefs.getBoolean("sc_useTls", true)
        )
    }

    private fun saveScanConfig(config: ScanConfig) {
        prefs.edit().apply {
            putString("sc_ipType", config.ipType)
            putInt("sc_port", config.port)
            putInt("sc_maxThreads", config.maxThreads)
            putInt("sc_delayMs", config.delayMs)
            putString("sc_coloFilter", config.coloFilter)
            putString("sc_domain", config.domain)
            putInt("sc_expectedCode", config.expectedCode)
            putBoolean("sc_random", config.random)
            putInt("sc_ipCount", config.ipCount)
            putBoolean("sc_useTls", config.useTls)
        }.apply()
    }

    private val _scanConfig = MutableStateFlow(loadScanConfig())"""

content = content.replace(scan_config_init, scan_config_replacement)

# Update _isAutoSyncEnabled
auto_sync_init = """    private val _isAutoSyncEnabled = MutableStateFlow(true)"""
auto_sync_replacement = """    private val _isAutoSyncEnabled = MutableStateFlow(prefs.getBoolean("sc_autoSync", true))"""
content = content.replace(auto_sync_init, auto_sync_replacement)

# Update setAutoSyncEnabled
set_auto_sync = """    fun setAutoSyncEnabled(enabled: Boolean) {
        _isAutoSyncEnabled.value = enabled
    }"""
set_auto_sync_replacement = """    fun setAutoSyncEnabled(enabled: Boolean) {
        _isAutoSyncEnabled.value = enabled
        prefs.edit().putBoolean("sc_autoSync", enabled).apply()
    }"""
content = content.replace(set_auto_sync, set_auto_sync_replacement)

# Update updateScanConfig
update_scan_config = """    fun updateScanConfig(config: ScanConfig) {
        _scanConfig.value = config
    }"""
update_scan_config_replacement = """    fun updateScanConfig(config: ScanConfig) {
        _scanConfig.value = config
        saveScanConfig(config)
    }"""
content = content.replace(update_scan_config, update_scan_config_replacement)

# Update MainViewModelFactory
factory = """class MainViewModelFactory(private val repository: IpRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}"""
factory_replacement = """class MainViewModelFactory(
    private val repository: IpRepository,
    private val prefs: SharedPreferences
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, prefs) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}"""
content = content.replace(factory, factory_replacement)

with open("app/src/main/java/com/example/ui/MainViewModel.kt", "w") as f:
    f.write(content)
