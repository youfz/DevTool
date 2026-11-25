package com.test.tool.data

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID
import java.util.concurrent.Executors

@SuppressLint("MissingPermission")
class RadioRepository(private val context: Context) {
 data class Peer(val address: String, val name: String?, val rssi: Int?, val classic: Boolean)
 data class State(val status: Int = 0, val peers: List<Peer> = emptyList(), val address: String = "", val characteristics: List<BluetoothGattCharacteristic> = emptyList(), val log: List<String> = emptyList(), val error: String = "", val classic: Boolean = false)
 val state = MutableStateFlow(State())
 private val handler = Handler(Looper.getMainLooper())
 private val adapter get() = context.getSystemService(BluetoothManager::class.java)?.adapter ?: error("Bluetooth unavailable")
 private var scan: ScanCallback? = null
 private var gatt: BluetoothGatt? = null
 private var socket: BluetoothSocket? = null
 private val reader = Executors.newSingleThreadExecutor()
 private val writer = Executors.newSingleThreadExecutor()
 private var writes = java.util.ArrayDeque<ByteArray>()
 private var lastChunk = byteArrayOf()
 private var writing: BluetoothGattCharacteristic? = null
 private var busy = false
 private var generation = 0
 private val timeout = Runnable { fail("Bluetooth timeout") }
 private fun log(line: String) {
  state.value = state.value.copy(log = (state.value.log + ("${java.time.LocalTime.now().withNano(0)} $line")).takeLast(100))
 }
 fun close() {
  generation++
  handler.removeCallbacks(timeout)
  scan?.let { runCatching { adapter.bluetoothLeScanner?.stopScan(it) } }; scan = null
  runCatching { gatt?.close() }; gatt = null
  runCatching { socket?.close() }; socket = null
  writes.clear(); writing=null
  busy = false
  state.value = State(peers = state.value.peers, log = state.value.log)
 }
 private fun fail(message: String) { close(); state.value = state.value.copy(error=message) }
 fun discover(classic: Boolean) {
  close()
  state.value = state.value.copy(peers = emptyList())
  runCatching {
   check(adapter.isEnabled) { "Bluetooth disabled" }
   if(classic) {
    val peers = adapter.bondedDevices
     .filter { it.type != BluetoothDevice.DEVICE_TYPE_LE }
     .map { Peer(it.address, it.name, rssi = null, classic = true) }
    state.value = state.value.copy(peers = peers)
   } else {
    val callback = object : ScanCallback() {
     override fun onScanResult(type: Int, result: ScanResult) {
      if(scan !== this) return
      val device = result.device
      val peer = Peer(device.address, result.scanRecord?.deviceName, result.rssi, classic = false)
      val current = state.value
      val peers = mergeScanResult(current.peers, peer) { runCatching { device.name }.getOrNull() }
      state.value = current.copy(peers = peers)
     }
     override fun onScanFailed(code: Int) { if(scan === this) fail("BLE scan: $code") }
    }
    scan = callback
    val scanner = checkNotNull(adapter.bluetoothLeScanner)
    state.value = state.value.copy(status=1)
    scanner.startScan(callback)
    handler.postDelayed({ if(scan === callback) { runCatching { scanner.stopScan(callback) }; scan=null; state.value=state.value.copy(status=0) } },12000)
   }
  }.onFailure { fail(it.message.orEmpty()) }
 }
 fun connect(peer: Peer) {
  close()
  state.value = state.value.copy(status = 2, address = peer.address, classic = peer.classic)
  handler.postDelayed(timeout,15000)
  val epoch = generation
  runCatching {
   val device = adapter.getRemoteDevice(peer.address)
   if(peer.classic) {
    val channel = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"))
    socket = channel
    reader.execute {
     try {
      channel.connect()
      handler.post { if(epoch==generation) { handler.removeCallbacks(timeout); state.value=state.value.copy(status=3) } }
      val buffer = ByteArray(512)
      while(true) {
       val n = channel.inputStream.read(buffer)
       if(n < 0) break
       val message = hex(buffer.copyOf(n))
       handler.post { if(epoch==generation) log("RX $message") }
      }
      handler.post { if(epoch==generation) close() }
     } catch(e: Exception) { handler.post { if(epoch==generation) fail(e.message.orEmpty()) } }
    }
   } else gatt = device.connectGatt(context,false,callback,BluetoothDevice.TRANSPORT_LE)
  }.onFailure { fail(it.message.orEmpty()) }
 }
 private val callback = object : BluetoothGattCallback() {
  override fun onConnectionStateChange(g: BluetoothGatt,status: Int,newState: Int) { handler.post {
   if(g !== gatt) return@post
   if(status == 0 && newState == BluetoothProfile.STATE_CONNECTED) {
    if(!g.discoverServices()) fail("GATT discovery")
   } else if(newState == BluetoothProfile.STATE_DISCONNECTED || status != 0) fail("GATT disconnected ($status)")
  } }
  override fun onServicesDiscovered(g: BluetoothGatt,status: Int) { handler.post {
   if(g !== gatt) return@post
   handler.removeCallbacks(timeout)
   if(status == 0) state.value=state.value.copy(status=3,characteristics=g.services.flatMap { it.characteristics }) else fail("GATT $status")
  } }
  override fun onCharacteristicRead(g: BluetoothGatt,c: BluetoothGattCharacteristic,status: Int) = finish(g,status,"RX "+hex(c.value ?: byteArrayOf()))
  override fun onCharacteristicWrite(g: BluetoothGatt,c: BluetoothGattCharacteristic,status: Int) = finish(g,status,"TX "+hex(lastChunk))
  override fun onDescriptorWrite(g: BluetoothGatt,d: BluetoothGattDescriptor,status: Int) = finish(g,status,"CCCD")
  override fun onCharacteristicChanged(g: BluetoothGatt,c: BluetoothGattCharacteristic) {
   val message=hex(c.value ?: byteArrayOf()); handler.post { if(g===gatt) log("RX $message") }
  }
 }
 private fun finish(g: BluetoothGatt,status: Int,message: String) { handler.post {
  if(g===gatt) {
   handler.removeCallbacks(timeout); busy=false
   log(if(status==0) message else "ERROR GATT $status")
   if(status==0 && writing!=null && writes.isNotEmpty()) {
    runCatching { check(writeNext(g)); busy=true; handler.postDelayed(timeout,10000) }.onFailure { fail(it.message.orEmpty()) }
   } else { writes.clear(); writing=null }
  }
 } }
 private fun writeNext(g: BluetoothGatt): Boolean {
  val c=checkNotNull(writing)
  val bytes=writes.removeFirst()
  lastChunk=bytes
  return if(android.os.Build.VERSION.SDK_INT >= 33) g.writeCharacteristic(c,bytes,c.writeType)==BluetoothStatusCodes.SUCCESS
  else { c.value=bytes; g.writeCharacteristic(c) }
 }
 fun operate(c: BluetoothGattCharacteristic?, action: Int, bytes: ByteArray) {
  runCatching {
   socket?.let { channel ->
    val epoch=generation
    writer.execute { runCatching { channel.outputStream.write(bytes) }.onSuccess { handler.post { if(epoch==generation) log("TX "+hex(bytes)) } }.onFailure { handler.post { if(epoch==generation) fail(it.message.orEmpty()) } } }
    return
   }
   val g = checkNotNull(gatt); val characteristic=checkNotNull(c)
   check(!busy) { "GATT busy" }
   val accepted = when(action) {
    0 -> g.readCharacteristic(characteristic)
    1 -> {
     require(bytes.isNotEmpty() && bytes.size <= 65536) { "1–65536 bytes" }
     writes.clear()
     for(offset in bytes.indices step 20) writes.add(bytes.copyOfRange(offset,minOf(bytes.size,offset+20)))
     writing=characteristic
     characteristic.writeType = if(characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT else BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
     writeNext(g)
    }
    else -> {
     val d=checkNotNull(characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")))
     check(g.setCharacteristicNotification(characteristic,true))
     d.value=if(characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE else BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
     g.writeDescriptor(d)
    }
   }
   check(accepted) { "GATT rejected operation" }; busy=true; handler.postDelayed(timeout,10000)
  }.onFailure { log("ERROR "+it.message) }
 }
 private fun hex(bytes: ByteArray) = bytes.joinToString(" ") { "%02X".format(it) }
}

internal fun mergeScanResult(
 peers: List<RadioRepository.Peer>,
 result: RadioRepository.Peer,
 cachedName: () -> String? = { null }
): List<RadioRepository.Peer> {
 val previous = peers.firstOrNull { it.address == result.address }
 val name = result.name?.takeIf { it.isNotBlank() }
  ?: previous?.name
  ?: cachedName()?.takeIf { it.isNotBlank() }
 return (peers.filterNot { it.address == result.address } + result.copy(name = name))
  .sortedWith(compareByDescending<RadioRepository.Peer> { it.rssi }.thenBy { it.address })
}


