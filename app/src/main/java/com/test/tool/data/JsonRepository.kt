package com.test.tool.data

import com.google.gson.*
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import java.io.StringReader

class JsonRepository {
 private val pretty = GsonBuilder().setPrettyPrinting().create()
 fun parse(input: String): JsonElement {
  require(input.toByteArray().size <= 1024 * 1024) { "Input exceeds 1 MiB" }
  val reader = JsonReader(StringReader(input)).apply { strictness = Strictness.STRICT }
  require(reader.peek() != JsonToken.END_DOCUMENT) { "Empty JSON at line 1 column 1" }
  val result = JsonParser.parseReader(reader)
  require(reader.peek() == JsonToken.END_DOCUMENT) { "Trailing input at ${reader.path}" }
  return result
 }
 fun format(input: String) = pretty.toJson(parse(input))
 fun minify(input: String) = parse(input).toString()
 fun compare(a: String, b: String) = parse(a) == parse(b)
 fun tree(input: String): String = buildString {
  fun visit(value: JsonElement, path: String, depth: Int) {
   require(depth < 128) { "Maximum tree depth 128" }
   append("  ".repeat(depth)).append(path).append(": ")
   when {
    value.isJsonObject -> { append("{ }\n"); value.asJsonObject.entrySet().forEach { visit(it.value, it.key, depth + 1) } }
    value.isJsonArray -> { append("[${value.asJsonArray.size()}]\n"); value.asJsonArray.forEachIndexed { i, item -> visit(item, "[$i]", depth + 1) } }
    else -> append(value).append('\n')
   }
  }
  visit(parse(input), "$", 0)
 }
}
