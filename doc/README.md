# DebugMate — Codex CLI 开发资料包

Android 移动端综合调试工具。此包是**需求、UI 资源、架构约束和开发提示词**，不是已编译或完整可运行的 Android 工程。请先查看 `AGENTS.md`，再按 `prompts/` 顺序生成项目。

## 目标
- Kotlin + XML + Material Components + MVVM + Repository + Coroutines/Flow。
- 默认深色主题，支持浅色与跟随系统；设置持久化。
- P0：JSON 工具、BLE/Classic 蓝牙、HTTP API 调试、Wireless/USB ADB、日志/历史。
- 不使用 Hilt：手动依赖注入/简单 AppContainer，便于直接构建。

## 文件
- `docs/01_PRD.md`：范围、用户故事、验收标准
- `docs/02_UI_UX.md`：页面、导航、双主题、交互规范
- `docs/03_ARCHITECTURE.md`：分层、权限、安全、技术约束
- `docs/04_FEATURES.md`：模块级细节和边界
- `docs/05_DATA_CONTRACTS.md`：数据模型、状态和接口
- `docs/06_ROADMAP.md`：开发分期、完成定义
- `docs/07_CODEX_GUIDE.md`：Codex CLI 命令和使用步骤
- `assets/mockups/`：参考视觉图；图中内容为设计示意，代码以文档为准
- `assets/icons/`：原创简易矢量图标
- `android-reference/`：可复制的主题资源与字符串示例

## 立即开始（Windows PowerShell）
```powershell
mkdir DebugMate
cd DebugMate
# 将本压缩包的内容解压到当前目录
codex
```
在 Codex 里输入：`阅读 AGENTS.md 和 prompts/01_bootstrap.md，按要求生成可编译的 Android 项目，先不要实现后续模块。`

> Android SDK、Gradle 和设备连接环境需要本机具备；真实蓝牙、ADB 功能需要物理设备验证。请不要把图片中的伪数据当作真实测试结果。
