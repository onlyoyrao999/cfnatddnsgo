import re

with open('./app/src/main/java/com/example/data/model/Models.kt', 'r') as f:
    content = f.read()

content = re.sub(
    r'val useTls: Boolean = true',
    r'val useTls: Boolean = true,\n    val maxPerColo: Int = 10',
    content
)

with open('./app/src/main/java/com/example/data/model/Models.kt', 'w') as f:
    f.write(content)
