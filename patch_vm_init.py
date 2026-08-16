import re

with open('./app/src/main/java/com/example/ui/MainViewModel.kt', 'r') as f:
    content = f.read()

# Remove the init block entirely
content = re.sub(
    r'\s+init \{\s+viewModelScope\.launch \{\s+repository\.savedIps\.collect \{ savedEntities ->\s+if \(!scannerEngine.*?\}\s+\}\s+\}\s+\}',
    '',
    content,
    flags=re.DOTALL
)

with open('./app/src/main/java/com/example/ui/MainViewModel.kt', 'w') as f:
    f.write(content)
