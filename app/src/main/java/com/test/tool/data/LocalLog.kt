package com.test.tool.data

import android.os.Process
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.ArrayDeque

/** Reads only this process. Log entries stay in memory. */
class LocalLog {
 private val ring=ArrayDeque<String>()
 val lines=MutableStateFlow<List<String>>(emptyList())
 val running=MutableStateFlow(false)
 private var process: java.lang.Process?=null
 private var job: Job?=null
 private var epoch=0
 fun start(scope: CoroutineScope) {
  stop()
  val session=epoch
  running.value=true
  job=scope.launch(Dispatchers.IO) {
   var child:java.lang.Process?=null
   try {
    val opened=ProcessBuilder("logcat","-v","threadtime","--pid="+Process.myPid().toString(),"-T","1").redirectErrorStream(true).start()
    child=opened
    synchronized(this@LocalLog) { if(session!=epoch) return@launch; process=opened }
    opened.inputStream.bufferedReader().useLines { stream ->
     for(line in stream) synchronized(this@LocalLog) {
      if(session!=epoch) return@launch
      if(ring.size>=200) ring.removeFirst()
      ring.addLast(line.take(2048))
      lines.value=ring.toList()
     }
    }
   } catch(e:Exception) {
    synchronized(this@LocalLog) { if(session==epoch) lines.value=listOf("ERROR "+e.message) }
   } finally {
    child?.destroy()
    synchronized(this@LocalLog) { if(session==epoch) { process=null; running.value=false } }
   }
  }
 }
 @Synchronized fun stop() { epoch++; process?.destroy(); process=null; job?.cancel(); job=null; running.value=false }
 @Synchronized fun clear() { ring.clear(); lines.value=emptyList() }
}