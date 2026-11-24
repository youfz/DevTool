# 03 技术架构

## 基线
Kotlin、Android XML、AppCompat/Material Components、ViewBinding、MVVM、Repository、Coroutines/StateFlow、OkHttp、Room、DataStore；无 Hilt，使用手动 AppContainer。Gradle Kotlin DSL 推荐。

## 模块建议
- `app`：Application、MainActivity、导航、主题、DI 装配。
- `core/model`、`core/storage`、`core/ui`、`core/session`。
- `feature/json`、`feature/bluetooth`、`feature/api`、`feature/adb`、`feature/tools`。
初始可使用单 app module + package 分层，稳定后再拆 Gradle module，避免早期构建复杂度。

## 关键设计
- UI 仅渲染 StateFlow<UiState>，事件从 ViewModel 转给 Repository。
- 蓝牙连接/ADB 会话由应用级 SessionManager 管理，ViewModel 不直接持有长期 Socket。
- 日志事件写入有界 Channel/Flow，批量入库/刷新；大文本按文件保存，Room 只存索引。
- OkHttp 构建动态 HTTP 请求，处理取消、超时、重定向与错误响应。
- JSON 使用实际可用的解析库并保留解析异常位置；不要用正则验证 JSON。
- BLE 使用 BluetoothLeScanner + BluetoothGatt；串行化 GATT 操作并处理 API 33+ 回调变化。
- Classic 使用 BluetoothSocket RFCOMM，连接在 IO 线程运行。
- ADB 封装 `AdbTransport`（Wireless、USB），配对/认证/ADB 协议与 Shell/Logcat 能力分层；先选经过验证的兼容库，若不满足再实现协议，禁止假实现。

## Android 权限/限制
- Android 12+ 蓝牙扫描与连接按需请求 BLUETOOTH_SCAN/CONNECT；Android 11 及更早按版本处理位置权限和蓝牙权限。
- USB Host：`UsbManager` 枚举设备、用户授权、接口/端点认领，正确释放资源。
- Wireless debugging 依赖目标设备开启无线调试与配对，网络可达；非所有版本均支持。
- 本机全局 logcat 对普通 App 不开放，不能以 READ_LOGS 权限声明规避。
- Android 后台运行和前台服务限制需按当前 targetSdk 与用途评估，避免默认常驻。

## 安全
- 默认不记录 Authorization、Cookie、配对密钥、敏感 body；提供脱敏开关与导出预览。
- URL、命令和 JSON 均可能包含用户敏感数据，存储/分享前确认。
- 证书验证保持开启；调试允许自签名时必须显式配置并警示风险，禁止全局忽略 TLS 校验。
