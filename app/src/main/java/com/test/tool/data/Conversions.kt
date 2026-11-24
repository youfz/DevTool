package com.test.tool.data

import java.util.Base64
import java.util.zip.CRC32
import java.time.Instant

object Conversions {
 fun apply(tool: Int, input: String, reverse: Boolean): String {
  val bytes = input.toByteArray()
  return when(tool) {
   10 -> if(reverse) {
    val hex = input.filterNot(Char::isWhitespace)
    require(hex.length % 2 == 0 && hex.all { it.digitToIntOrNull(16) != null }) { "Invalid HEX" }
    hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray().decodeToString()
   } else bytes.joinToString(" ") { "%02X".format(it) }
   11 -> if(reverse) Base64.getDecoder().decode(input.trim()).decodeToString() else Base64.getEncoder().encodeToString(bytes)
   12 -> "%08X".format(CRC32().apply { update(bytes) }.value)
   13 -> {
    val value = input.trim()
    val number = value.toLongOrNull()
    when { number == null -> Instant.parse(value).toEpochMilli().toString()
     value.trimStart('-').length <= 10 -> Instant.ofEpochSecond(number).toString()
     else -> Instant.ofEpochMilli(number).toString() }
   }
   else -> error("Unknown tool")
  }
 }
}