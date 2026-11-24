package com.test.tool

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import androidx.appcompat.widget.AppCompatEditText

class JsonEditor(context: Context) : AppCompatEditText(context) {
 private val scale = resources.displayMetrics.density
 private val gutter = Paint(Paint.ANTI_ALIAS_FLAG).apply {
  color=context.getColor(R.color.dm_text_secondary)
  textAlign=Paint.Align.RIGHT
  textSize=12*resources.displayMetrics.scaledDensity
 }
 init {
  setPadding((48*scale).toInt(),(12*scale).toInt(),(8*scale).toInt(),(12*scale).toInt())
  setHorizontallyScrolling(true)
 }
 override fun onDraw(canvas: Canvas) {
  super.onDraw(canvas)
  val lines=layout ?: return
  val first=lines.getLineForVertical(scrollY)
  val last=lines.getLineForVertical(scrollY+height)
  for(i in first..last) canvas.drawText((i+1).toString(),scrollX+36*scale,(totalPaddingTop+lines.getLineBaseline(i)).toFloat(),gutter)
 }
 fun markError(message:String) {
  val numbers=Regex("line (\\d+) column (\\d+)").find(message) ?: return
  val line=numbers.groupValues[1].toIntOrNull()?.minus(1) ?: return
  val column=numbers.groupValues[2].toIntOrNull()?.minus(1) ?: return
  val source=layout ?: return
  if(line !in 0 until source.lineCount) return
  val offset=(source.getLineStart(line)+column).coerceIn(source.getLineStart(line),source.getLineEnd(line))
  requestFocus()
  setSelection(offset,(offset+1).coerceAtMost(length()))
  error=message
 }
}