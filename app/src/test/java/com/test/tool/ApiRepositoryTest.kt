package com.test.tool
import com.test.tool.data.ApiRepository
import kotlinx.coroutines.*
import okhttp3.mockwebserver.*
import org.junit.Assert.*
import org.junit.Test
class ApiRepositoryTest {
 @Test fun statusAndRedaction() = runBlocking {
  val server = MockWebServer(); server.start()
  try {
   for(status in listOf(200,404,500)) {
    server.enqueue(MockResponse().setResponseCode(status).setHeader("Set-Cookie","secret").setBody("body"))
    val result = ApiRepository().execute("GET",server.url("/").toString(),"", "")
    assertEquals(status,result.status); assertEquals("body",result.body); assertFalse(result.headers.contains("secret"))
   }
  } finally { server.shutdown() }
 }
 @Test fun cancellationStopsSuspension() = runBlocking {
  val server = MockWebServer(); server.start()
  try {
   server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
   val job = launch { ApiRepository().execute("GET",server.url("/").toString(),"", "") }
   delay(100); withTimeout(2000) { job.cancelAndJoin() }; assertTrue(job.isCancelled)
  } finally { server.shutdown() }
 }
}
