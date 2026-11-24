package com.test.tool.data

import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class ApiResult(val status: Int, val elapsed: Long, val headers: String, val body: String)
class ApiRepository {
 private val client = OkHttpClient.Builder().callTimeout(30, TimeUnit.SECONDS).followRedirects(false).build()
 suspend fun execute(method: String, url: String, headers: String, body: String): ApiResult = suspendCancellableCoroutine { continuation ->
  val started = System.nanoTime()
  val request = try {
   require(url.startsWith("https://") || url.startsWith("http://")) { "HTTP(S) URL required" }
   Request.Builder().url(url).apply {
    headers.lineSequence().filter { it.isNotBlank() }.forEach {
     val split = it.indexOf(':'); require(split > 0) { "Invalid header" }
     addHeader(it.substring(0, split).trim(), it.substring(split + 1).trim())
    }
    method(method, if (method in listOf("GET", "HEAD")) null else body.toRequestBody("text/plain; charset=utf-8".toMediaType()))
   }.build()
  } catch (e: Exception) { continuation.resumeWithException(e); return@suspendCancellableCoroutine }
  val call = client.newCall(request)
  continuation.invokeOnCancellation { call.cancel() }
  call.enqueue(object : Callback {
   override fun onFailure(call: Call, e: IOException) { if (continuation.isActive) continuation.resumeWithException(e) }
   override fun onResponse(call: Call, response: Response) {
    try {
     val result = response.use {
      val source = it.body?.source()
      source?.request(1024 * 1024L + 1)
      val bytes = source?.buffer?.readByteArray() ?: byteArrayOf()
      require(bytes.size <= 1024 * 1024) { "Response exceeds 1 MiB" }
      ApiResult(it.code, (System.nanoTime() - started) / 1000000, it.headers.names().joinToString("\n") { name -> "$name: " + if (name.lowercase() in setOf("set-cookie", "authorization", "cookie")) "[REDACTED]" else it.header(name).orEmpty() }, bytes.toString(Charsets.UTF_8))
     }
     if (continuation.isActive) continuation.resume(result)
    } catch(e: Exception) { if (continuation.isActive) continuation.resumeWithException(e) }
   }
  })
 }
}

