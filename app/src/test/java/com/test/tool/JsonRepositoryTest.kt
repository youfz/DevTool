package com.test.tool
import com.test.tool.data.JsonRepository
import org.junit.Assert.*
import org.junit.Test
class JsonRepositoryTest {
 private val repo = JsonRepository()
 @Test fun objectsIgnoreKeyOrder() { assertTrue(repo.compare("{\"a\":1,\"b\":2}", "{\"b\":2,\"a\":1}")) }
 @Test fun arraysPreserveOrder() { assertFalse(repo.compare("[1,2]", "[2,1]")) }
 @Test fun formatRoundTrip() { assertEquals("{\"a\":[true,null,\"你好\"]}",repo.minify(repo.format("{\"a\":[true,null,\"你好\"]}"))) }
 @Test fun errorIncludesLocation() {
  try { repo.parse("{\n\"a\": }"); fail() } catch(e: Exception) { assertTrue(e.toString().contains("line 2")); assertTrue(e.toString().contains("column")) }
 }
 @Test fun rejectsInvalidSyntax() {
  listOf("", "null true", "{'a':1}", "[1,]", "{a:1}", "NaN", "/*x*/ true").forEach { input ->
   assertTrue(input, runCatching { repo.parse(input) }.isFailure)
  }
 }
 @Test fun largeArray() { assertEquals(10000, repo.parse((0 until 10000).joinToString(",","[","]")).asJsonArray.size()) }
 @Test fun treeContainsPaths() { assertTrue(repo.tree("{\"a\":[1]}").contains("[0]: 1")) }
}
