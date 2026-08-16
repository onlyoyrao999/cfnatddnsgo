import re

with open('README.md', 'r', encoding='utf-8') as f:
    content = f.read()

# Replace the title and any references
content = content.replace('CF IP 优选与代理', 'CF IP 优选与DNS')

new_feedback = """## 🤝 贡献与反馈

CMLiussss 技术交流群

 @onlyno999 交流改进


TG交流群：t.me/CMLiussss 

如果您觉得这个工具对您有帮助，不妨点个 ⭐️ Star 支持一下！

如果您在使用过程中遇到 Bug 或有新功能的建议，欢迎提交 Issues 或发起 Pull Request。"""

if "## 🤝 贡献与反馈" in content:
    content = re.sub(r'## 🤝 贡献与反馈.*', new_feedback, content, flags=re.DOTALL)
else:
    content += "\n\n" + new_feedback

with open('README.md', 'w', encoding='utf-8') as f:
    f.write(content)
