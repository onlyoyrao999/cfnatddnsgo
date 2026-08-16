import re

with open('./app/src/main/java/com/example/ui/MainViewModel.kt', 'r') as f:
    content = f.read()

content = re.sub(
    r'val useTls = prefs\.getBoolean\("sc_useTls", true\)',
    r'useTls = prefs.getBoolean("sc_useTls", true),\n            maxPerColo = prefs.getInt("sc_maxPerColo", 10)',
    content
)

content = re.sub(
    r'putBoolean\("sc_useTls", config\.useTls\)',
    r'putBoolean("sc_useTls", config.useTls)\n            putInt("sc_maxPerColo", config.maxPerColo)',
    content
)

with open('./app/src/main/java/com/example/ui/MainViewModel.kt', 'w') as f:
    f.write(content)
