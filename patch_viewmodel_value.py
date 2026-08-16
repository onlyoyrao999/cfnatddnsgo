import re

with open('./app/src/main/java/com/example/ui/MainViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace("val currentSaved = repository.savedIps.value", "val currentSaved = savedIps.value")

with open('./app/src/main/java/com/example/ui/MainViewModel.kt', 'w') as f:
    f.write(content)
