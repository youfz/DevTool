# DebugMate 实施状态

Android Kotlin/XML，minSdk 26，compileSdk/targetSdk 36。AppContainer 组装仓库，Activity 观察 ViewModel 和设备状态。DataStore 保存默认深色、浅色及跟随系统设置。

| 页面 | 当前行为 | 剩余需求 |
|---|---|---|
| 首页与工具箱 | 双主题渐变工作台、设备状态、四个模块卡片、HEX、Base64、CRC32、时间戳、最近活动 | 设备截图、高字体缩放与横屏视觉验收 |
| JSON | 四个标签页、行号、错误位置选择、树节点逐层展开、格式化、压缩、结构比较、复制及 SAF 导出 | 大文档交互性能、错误高亮真机验收 |
| API | 方法、URL、Params、Headers、Body、Bearer、环境变量、超时、取消、响应头与正文、历史摘要 | Basic、持久环境集合、格式化响应标签、可配置超时 |
| 蓝牙 | 按版本申请权限，真实 BLE 扫描、RSSI、连接、服务特征选择、读、分段写、CCCD 订阅；已配对 Classic RFCOMM/SPP 连接，HEX/UTF-8 输入和有界 TX/RX 日志 | 真机验证、自动发送、接收编码切换、统计与导出 |
| 终端 | 读取本应用 PID 的 Logcat、筛选、暂停、清空和 SAF 导出 | 远程 Shell、目标设备日志 |
| ADB | 本机品牌、型号、系统版本、内存及存储信息 | 无线配对认证、USB Host ADB 协议及远程会话尚未实现。本机信息不构成 ADB 连接 |
| 记录 | 请求与 JSON 操作摘要、本地有界保存、搜索、导出和清理 | Room 会话数据库、连接事件、单条删除与轮转文件 |
