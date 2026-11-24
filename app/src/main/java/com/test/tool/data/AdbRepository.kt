package com.test.tool.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface AdbConnectionConfig {
 data class Wireless(val host: String, val port: Int) : AdbConnectionConfig
 data class Usb(val deviceId: Int) : AdbConnectionConfig
}
enum class ConnectionStatus { DISCONNECTED, CONNECTING, CONNECTED, ERROR, NOT_IMPLEMENTED }
/** No protocol implementation has been validated. Never reports a successful connection. */
class AdbRepository {
 private val mutable = MutableStateFlow(ConnectionStatus.NOT_IMPLEMENTED)
 val state = mutable.asStateFlow()
 suspend fun connect(config: AdbConnectionConfig): Nothing = throw UnsupportedOperationException("ADB transport not implemented")
 suspend fun execute(command: String): Nothing = throw UnsupportedOperationException("ADB shell not implemented")
 suspend fun disconnect() { mutable.value = ConnectionStatus.NOT_IMPLEMENTED }
}
