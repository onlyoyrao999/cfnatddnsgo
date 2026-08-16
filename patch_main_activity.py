import re

with open('./app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

content = content.replace("val factory = MainViewModelFactory(repository, prefs)", "val factory = MainViewModelFactory(repository, prefs, applicationContext)")

with open('./app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
