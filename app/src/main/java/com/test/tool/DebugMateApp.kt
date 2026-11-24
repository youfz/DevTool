package com.test.tool

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import com.test.tool.data.*

private val Context.settings by preferencesDataStore("settings")
class AppContainer(context: Context) {
 val json = JsonRepository()
 val radio = RadioRepository(context.applicationContext)
 val api = ApiRepository()
 val themes = ThemeRepository(context)
 val history = HistoryRepository(context)
}
class DebugMateApp : Application() {
 lateinit var container: AppContainer
 override fun onCreate() {
  super.onCreate()
  AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
  container = AppContainer(this)
 }
}
class ThemeRepository(private val context: Context) {
 private val key = intPreferencesKey("theme")
 val mode = context.settings.data.catch { emit(emptyPreferences()) }.map { it[key] ?: AppCompatDelegate.MODE_NIGHT_YES }
 suspend fun set(mode: Int) { context.settings.edit { it[key] = mode } }
}

