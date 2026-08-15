# CF IP Scanner & DNS Sync 🚀

**CF IP Scanner & DNS Sync** 是一款基于 Android 平台的 Cloudflare 优选 IP 扫描与自动 DNS 解析同步工具。它可以帮助您快速扫描 Cloudflare 边缘节点，找出延迟最低的优质 IP，并严格按照节点区域自动将其同步到您的 Cloudflare 域名解析记录中。

## ✨ 核心功能

*   ⚡ **高速 IP 扫描引擎**: 快速测速并筛选出当前网络环境下延迟最低的 Cloudflare 节点 IP。
*   🎯 **精准节点过滤**: 支持根据机场代码 / 数据中心（Colo，例如 `LAX`, `SJC`, `HKG`, `NRT`, `SIN` 等）进行精准的节点过滤和分类。
*   🔄 **Cloudflare DNS 自动/手动同步**: 
    *   直接对接 Cloudflare API，将优选出的 IP 自动更新至您的指定域名。
    *   支持为不同域名独立配置多条同步规则（例如：将 `lax.yourdomain.com` 专门绑定洛杉矶节点）。
    *   **严格过滤机制**：严格遵循过滤规则（如无符合要求节点，则停止同步并报错，拒绝跨区乱匹配）。
*   🗄️ **本地 IP 优选库**: 内置 Room 本地数据库，自动为您保存并长期维护最新、最快的 100 个有效 IP。
*   🛡️ **本地代理服务**: 支持一键开启本地 Proxy 服务（默认端口 1234），可将优选流量路由给其他应用使用。

## 📦 下载与安装

本项目配置了全自动的 GitHub Actions CI/CD 流水线，代码推送后会自动打包并发布，无需手动编译。

👉 **[前往 Releases 页面下载最新版](../../releases)**

**提供的安装包变体：**
*   `arm64-v8a` (推荐绝大多数现代手机使用，体积小)
*   `armeabi-v7a` (兼容老旧安卓设备)
*   `x86` / `x86_64` (适用于安卓模拟器)
*   `universal` (全架构通用安装包)

## 📖 快速上手指南

1. **测速扫描**: 打开 App 进入「Scanner」页面，点击 "START HIGH-SPEED SCAN" 开始获取并测速 IP。
2. **分类查看**: 在扫描结果中，点击中部的标签（如 `SJC`, `LAX` 等）可以快速筛选特定区域的优选 IP。
3. **配置 DNS 同步规则**: 进入「CF Sync」页面，点击 `+` 按钮，填入您的 Cloudflare API 密钥、Zone ID，设置目标域名和匹配规则（Colo Filter 填入对应的机场码，如 `LAX`）。
4. **执行同步**: 开启规则开关后，点击 "Sync All" 或单条规则的 "Sync Now"，App 会自动从最新扫描结果或本地 IP 库中提取最优 IP，并自动更新至 Cloudflare。

## 🛠️ 本地构建与开发

如果您是开发者并希望自行编译此项目：

1. 将项目 Clone 到本地环境：
   ```bash
   git clone https://github.com/onlyoyrao999/cfnatddns.git
   ```
2. 确保您的环境已安装 Java 21。
3. 使用 Android Studio 打开项目。
4. 执行打包命令：
   ```bash
   ./gradlew assembleDebug
   ```

## 🤝 贡献与反馈

如果您在使用过程中遇到 Bug 或有新功能的建议，欢迎提交 [Issues](../../issues) 或发起 Pull Request。

如果您觉得这个工具对您有帮助，不妨点个 ⭐️ **Star** 支持一下！
