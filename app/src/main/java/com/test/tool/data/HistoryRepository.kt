package com.test.tool.data
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
class HistoryRepository(context: Context) {
 private val prefs = context.getSharedPreferences("history", Context.MODE_PRIVATE)
 private val mutable = MutableStateFlow(prefs.getString("entries", "").orEmpty().lines().filter { it.isNotBlank() })
 val entries = mutable.asStateFlow()
 @Synchronized fun add(kind: String) {
  mutable.value = (listOf("${java.text.DateFormat.getDateTimeInstance().format(java.util.Date())} · $kind") + mutable.value).take(200)
  prefs.edit().putString("entries", mutable.value.joinToString("\n")).apply()
 }
 fun clear() { mutable.value = emptyList(); prefs.edit().clear().apply() }
}
