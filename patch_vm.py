import re

with open('./app/src/main/java/com/example/ui/MainViewModel.kt', 'r') as f:
    content = f.read()

# Update constructor to take Context
content = re.sub(r'class MainViewModel\(\s+private val repository: IpRepository,\s+private val prefs: SharedPreferences\s+\) : ViewModel\(\) \{', r'class MainViewModel(\n    private val repository: IpRepository,\n    private val prefs: SharedPreferences,\n    private val context: Context\n) : ViewModel() {', content)

# Update scanner engine instantiation
content = content.replace("private val scannerEngine = IpScannerEngine()", "private val scannerEngine = IpScannerEngine(context)")

# Update the factory
content = re.sub(r'class MainViewModelFactory\(\s+private val repository: IpRepository,\s+private val prefs: SharedPreferences\s+\) : ViewModelProvider\.Factory \{', r'class MainViewModelFactory(\n    private val repository: IpRepository,\n    private val prefs: SharedPreferences,\n    private val context: Context\n) : ViewModelProvider.Factory {', content)

content = re.sub(r'return MainViewModel\(repository, prefs\) as T', r'return MainViewModel(repository, prefs, context) as T', content)

with open('./app/src/main/java/com/example/ui/MainViewModel.kt', 'w') as f:
    f.write(content)
