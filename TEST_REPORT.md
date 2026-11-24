# DebugMate 验证报告

2026-11-23，JDK 21、Gradle 9.4.1、Android SDK 36。使用工作区可写的 Gradle 缓存和 SDK 副本构建。

| 检查 | 结果 | 证据 |
|---|---|---|
| Debug APK | PASS | app/build/outputs/apk/debug/app-debug.apk |
| JVM 单测 | PASS | 13 项，包含 JSON 7 项、HTTP 2 项、转换工具 3 项及原有模板测试 1 项 |
| Release 编译及打包 | PASS（跳过 Lint） | app/build/outputs/apk/release/app-release-unsigned.apk |
| 完整 Release Lint | FAIL | 离线缓存缺少 datastore-preferences-jvm 和 lifecycle desktop 依赖 |
| 导航仪器测试编译与打包 | PASS | assembleDebugAndroidTest |
| 真机导航、视觉对照及主题切换 | NOT_TESTED | adb devices 启动时报 Cannot mkdir '\\.android': Permission denied，未取得运行截图 |
| BLE、Classic 与本机 Logcat 真机验证 | NOT_TESTED | 待真实外设、授权、断连、取消、超时及日志观察 |
| ADB、Room 会话 | FAIL | 尚未实现完整需求 |

最终验证任务为 `:app:assembleDebug :app:testDebugUnitTest :app:assembleRelease :app:assembleDebugAndroidTest --offline -x lintVitalAnalyzeRelease -x lintVitalReportRelease -x lintVitalRelease`。Gradle 报 BUILD SUCCESSFUL。详细输出在 .tmp/verify.log。

硬件回归需使用 BLE 外设和 SPP 串口设备，分别检查首次授权、拒绝、扫描无结果、主动断开、被动断开、特征属性限制、CCCD 通知、超过 20 字节连续写入、UTF-8 与 HEX 输入、旋转及退出后的资源释放。ADB 仍需先实现传输协议，再验证配对拒绝、USB 拔出、Shell 取消和目标日志过滤。
