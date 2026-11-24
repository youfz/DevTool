# 07 Codex CLI 使用指南

## Windows PowerShell
```powershell
mkdir D:\AndroidSpace\DebugMate
# 解压 DebugMate_Codex_Starter.zip 到 D:\AndroidSpace\DebugMate
cd D:\AndroidSpace\DebugMate
codex
```

启动后先输入：
`先阅读 AGENTS.md、README.md、docs/ 和 prompts/01_bootstrap.md；列出实施计划，然后执行第一阶段。`

之后依次输入：
`按 prompts/02_json_api.md 开发 JSON 与 API，运行单测并修复编译错误。`
`按 prompts/03_bluetooth.md 开发蓝牙模块；无法真机验证的功能列出测试步骤。`
`按 prompts/04_adb.md 开发 ADB；先确认可用库和协议兼容性，禁止伪实现。`
`按 prompts/05_quality.md 做集成验收。`

建议每完成一阶段提交 Git，避免一次性生成所有代码导致上下文过大。Codex 只能在当前环境具备 Android SDK/设备/权限时实际构建或真机测试。
