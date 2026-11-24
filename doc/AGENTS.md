# Codex 项目指令（必须遵守）

1. 先阅读 README.md 与 docs/ 全部文档；冲突时以 docs/01_PRD.md 的 P0 验收为准，安全约束优先。
2. Android 原生 Kotlin + XML，MVVM、Repository、Coroutines/StateFlow、ViewBinding；不使用 Compose 和 Hilt。依赖通过 AppContainer/工厂注入。
3. 深色主题为默认；支持浅色、跟随系统；使用 AppCompatDelegate + DataStore。不得硬编码颜色和用户可见文案。
4. 分模块开发，优先编译通过再继续；每次修改说明文件、测试和未完成项。
5. 不伪造设备连接、HTTP 成功、ADB 运行结果；模拟数据必须显著标记 Demo。
6. Bluetooth、USB、ADB、Logcat 必须处理权限、取消、超时、断连、资源释放及生命周期。
7. 普通第三方 App 无法读取其他应用的完整本机 logcat；本机模式只提供可访问的数据，明确展示限制。
8. Wireless ADB 必须实现 Android 11+ 配对/认证与 ADB 协议；USB ADB 使用 Android USB Host API + ADB 协议，不是调用 PC adb 可执行文件。
9. 用户提供的 URL/令牌/设备数据视为不可信；避免在日志和历史中持久化明文密码、密钥、Authorization 等敏感信息。
10. 默认 minSdk 26，targetSdk 与 compileSdk 使用本地稳定可用版本并说明；所有依赖需实际可解析，不要杜撰 Maven 坐标。
11. P0 完成定义：能构建、单测可运行、主要页面可导航、功能按 docs 验收；不能验证的硬件功能标记为待设备验证。
12. 保持增量提交友好，禁止无关大规模重构；每个阶段结束输出命令、构建结果和下一步。
