package com.test.tool
import com.test.tool.data.Conversions
import org.junit.Assert.*
import org.junit.Test
class ConversionsTest {
 @Test fun roundTripsUtf8() {
  for(tool in listOf(10,11)) assertEquals("你好",Conversions.apply(tool,Conversions.apply(tool,"你好",false),true))
 }
 @Test fun crcReferenceVector() { assertEquals("CBF43926",Conversions.apply(12,"123456789",false)) }
 @Test fun epochRoundTrip() {
  val iso=Conversions.apply(13,"1700000000",false)
  assertEquals("1700000000000",Conversions.apply(13,iso,false))
 }
}