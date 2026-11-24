package com.test.tool

import okhttp3.HttpUrl.Companion.toHttpUrl
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel(val container: AppContainer) : ViewModel() {
 val localLog=com.test.tool.data.LocalLog()
 var logFilter=""
 fun startLog() { localLog.start(viewModelScope) }
 var historyFilter = ""
 var classic = false
 var radioHex = false
 var radioDraft = ""
 var characteristicIndex = 0
 override fun onCleared() { container.radio.close(); localLog.stop() }
 var jsonTab = 0
 val toolInputs = mutableMapOf<Int,String>()
 val toolResults = mutableMapOf<Int,String>()
 val page = MutableStateFlow(0)
 val jsonInput = MutableStateFlow("")
 val jsonOther = MutableStateFlow("")
 val url = MutableStateFlow("")
 var apiTab = 0
 var params = ""
 var environment = ""
 var bearer = ""
 val headers = MutableStateFlow("")
 val body = MutableStateFlow("")
 val method = MutableStateFlow("GET")
 private val mutable = MutableStateFlow("")
 val result = mutable.asStateFlow()
 val busy = MutableStateFlow(false)
 private var task: Job? = null
 private var jsonJob: Job? = null
 val jsonResult = MutableStateFlow("")
 fun json(action: Int, valid: String, same: String, different: String) {
  jsonJob?.cancel()
  jsonJob = viewModelScope.launch {

   try {
    jsonResult.value = withContext(Dispatchers.Default) {
     when(action) {
      0 -> container.json.format(jsonInput.value)
      1 -> { container.json.parse(jsonInput.value); valid }
      2 -> container.json.tree(jsonInput.value)
      3 -> if(container.json.compare(jsonInput.value, jsonOther.value)) same else different
      else -> container.json.minify(jsonInput.value)
     }
    }
    container.history.add("JSON")
   } catch (e: CancellationException) { throw e } catch(e: Exception) { jsonResult.value = e.message.orEmpty() }

  }
 }
 fun send(render: (Int, Long, String, String) -> String) {
  if (busy.value) return
  task = viewModelScope.launch {
   busy.value = true
   try {
    fun pairs(raw: String) = raw.lineSequence().filter { it.isNotBlank() }.map {
     val index=it.indexOf('=')
     require(index>0) { "Expected key=value" }
     it.take(index).trim() to it.drop(index+1)
    }
    val variables=pairs(environment).toMap()
    fun expand(raw: String) = Regex("""\{\{([A-Za-z0-9_]+)\}\}""").replace(raw) { variables.getValue(it.groupValues[1]) }
    val target=expand(url.value).toHttpUrl().newBuilder()
    pairs(params).forEach { (key,value) -> target.addQueryParameter(expand(key),expand(value)) }
    val auth=if(bearer.isBlank()) "" else "\nAuthorization: Bearer "+expand(bearer)
    val r = container.api.execute(method.value,target.build().toString(),expand(headers.value)+auth,expand(body.value))
    mutable.value = render(r.status, r.elapsed, r.headers, r.body)
    container.history.add("HTTP ${r.status}")
   } catch(e: CancellationException) { throw e } catch(e: Exception) { mutable.value = e.javaClass.simpleName + ": " + e.message }
   finally { busy.value = false }
  }
 }
 fun cancel(message: String) { task?.cancel(); mutable.value = message }
}






