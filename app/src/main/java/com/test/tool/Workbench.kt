package com.test.tool

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.*
import android.widget.*
import androidx.core.graphics.ColorUtils
import com.google.android.material.card.MaterialCardView

class Workbench(private val parent: LinearLayout, private val open: (Int) -> Unit) {
 private val context: Context get() = parent.context
 private fun dp(n: Int) = (n * context.resources.displayMetrics.density).toInt()
 private fun label(value: String, size: Float, bold: Boolean = false) = TextView(context).apply {
  text = value
  textSize = size
  if (bold) setTypeface(typeface, Typeface.BOLD)
 }
 private data class Tile(val id: Int, val name: Int, val icon: Int, val color: Int, val hint: Int = 0)
 private val primary = buildList {
  add(Tile(5,R.string.modules_json,R.drawable.dm_json,R.color.dm_primary,R.string.json_hint))
  add(Tile(7,R.string.modules_bt,R.drawable.dm_bt,R.color.dm_cyan,R.string.bt_hint))
  if (context.resources.getBoolean(R.bool.show_debug_entries)) {
   add(Tile(6,R.string.modules_api,R.drawable.dm_api,R.color.dm_green,R.string.api_hint))
   add(Tile(9,R.string.modules_adb,R.drawable.dm_terminal,R.color.dm_purple,R.string.adb_hint))
  }
 }
 private val utility = listOf(
  Tile(10,R.string.hex,R.drawable.dm_json,R.color.dm_cyan),
  Tile(11,R.string.base64,R.drawable.dm_terminal,R.color.dm_green),
  Tile(12,R.string.crc,R.drawable.dm_tools,R.color.dm_amber),
  Tile(13,R.string.timestamp,R.drawable.dm_history,R.color.dm_purple))
 private fun section(title: Int) {
  parent.addView(label(context.getString(title),18f,true).apply { setPadding(dp(4),dp(16),0,dp(6)) })
 }
 private fun tiles(items: List<Tile>, columns: Int) {
  for(group in items.chunked(columns)) {
   val row = LinearLayout(context)
   parent.addView(row)
   for(tile in group) {
    val tint = context.getColor(tile.color)
    val card = MaterialCardView(context).apply {
     radius = dp(14).toFloat()
     cardElevation = 0f
     strokeWidth = dp(1)
     strokeColor = ColorUtils.setAlphaComponent(tint,65)
     setCardBackgroundColor(context.getColor(R.color.dm_surface))
     isFocusable = true
     setOnClickListener { open(tile.id) }
    }
    val stack = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(2),dp(12),dp(2),dp(12)) }
    stack.addView(ImageView(context).apply {
     setImageResource(tile.icon); setColorFilter(tint); setPadding(dp(7),dp(7),dp(7),dp(7))
     background = GradientDrawable().apply { cornerRadius = dp(10).toFloat(); setColor(ColorUtils.setAlphaComponent(tint,35)) }
    },dp(38),dp(38))
    stack.addView(label(context.getString(tile.name),14f,true).apply { setPadding(0,dp(6),0,0); gravity=Gravity.CENTER })
    if(tile.hint != 0) stack.addView(label(context.getString(tile.hint),12f).apply { setTextColor(context.getColor(R.color.dm_text_secondary)) })
    stack.isClickable = false
    card.addView(stack)
    row.addView(card,LinearLayout.LayoutParams(0,WRAP_CONTENT,1f).apply { setMargins(dp(3),dp(4),dp(3),dp(4)) })
   }
  }
 }
 fun show(home: Boolean, rows: List<String>, connected: Boolean) {
  if(home) {
   val hero = LinearLayout(context).apply {
    orientation = LinearLayout.VERTICAL
    setPadding(dp(22),dp(20),dp(22),dp(20))
    background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(context.getColor(R.color.dm_primary),context.getColor(R.color.dm_hero_end))).apply { cornerRadius = dp(16).toFloat() }
    setOnClickListener { open(2) }
    isFocusable = true
   }
   for ((title,size) in listOf(R.string.workbench to 22f,R.string.tagline to 14f,(if(connected) R.string.connected else R.string.no_device) to 13f)) {
    hero.addView(label(context.getString(title),size,title==R.string.workbench).apply { setTextColor(context.getColor(android.R.color.white)); if(title!=R.string.workbench) setPadding(0,dp(6),0,0) })
   }
   parent.addView(hero)
  }
  section(R.string.quick); tiles(primary,2)
  section(R.string.common); tiles(utility,if(context.resources.configuration.fontScale > 1.25f) 2 else 4)
  if(home) {
   section(R.string.recent)
   rows.take(4).ifEmpty { listOf(context.getString(R.string.empty)) }.forEach {
    parent.addView(label(it,14f).apply { setPadding(dp(12),dp(12),dp(12),dp(12)); setBackgroundColor(context.getColor(R.color.dm_surface)); setOnClickListener { open(4) } })
   }
  }
 }
}


