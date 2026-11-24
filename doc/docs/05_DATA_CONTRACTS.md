# 05 数据与接口契约

```kotlin
enum class ThemeMode { DARK, LIGHT, SYSTEM }
enum class ConnectionStatus { DISCONNECTED, CONNECTING, CONNECTED, ERROR }
enum class EventDirection { TX, RX, SYSTEM }
enum class DebugKind { JSON, BLE, CLASSIC, HTTP, ADB, LOGCAT }
data class DebugEvent(val id: String, val sessionId: String, val timestampMs: Long, val kind: DebugKind, val direction: EventDirection, val summary: String, val payloadPath: String? = null)
data class DebugSession(val id: String, val kind: DebugKind, val title: String, val startedAtMs: Long, val endedAtMs: Long? = null)
data class ApiRequestDraft(val method: String, val url: String, val headers: Map<String,String>, val body: String?)
data class ApiResponseData(val statusCode: Int?, val durationMs: Long, val headers: Map<String,String>, val body: String?, val error: String?)
```

## Repository 接口（建议）
```kotlin
interface ThemeRepository { val themeMode: kotlinx.coroutines.flow.Flow<ThemeMode>; suspend fun setThemeMode(mode: ThemeMode) }
interface JsonRepository { fun format(input: String): Result<String>; fun minify(input: String): Result<String>; fun compare(left: String, right: String): JsonDiff }
interface ApiRepository { suspend fun execute(request: ApiRequestDraft): ApiResponseData; fun cancel(requestId: String) }
interface BluetoothRepository { val state: kotlinx.coroutines.flow.StateFlow<ConnectionStatus>; suspend fun scan(); suspend fun connect(id: String); suspend fun disconnect() }
interface AdbRepository { val state: kotlinx.coroutines.flow.StateFlow<ConnectionStatus>; suspend fun connect(config: AdbConnectionConfig); suspend fun execute(command: String): String; suspend fun disconnect() }
```
接口仅为开发指导草案，具体类型、取消语义和流式输出可在实现时完善；BLE 与 Classic 建议拆分仓库接口。`JsonDiff`、`AdbConnectionConfig` 由实现阶段定义。

## 数据库建议
`debug_sessions(id, kind, title, started_at, ended_at)`、`debug_events(id, session_id, timestamp, kind, direction, summary, payload_path)`、`api_history(id, created_at, method, url_redacted, status, duration_ms)`。为 session_id/timestamp 建索引；删除历史时同步删除附件文件。
