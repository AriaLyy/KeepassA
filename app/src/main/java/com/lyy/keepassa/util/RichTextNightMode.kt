package com.lyy.keepassa.util

import android.graphics.Color
import android.text.style.ForegroundColorSpan
import android.widget.TextView
import com.arialyy.frame.util.ResUtil
import com.lyy.keepassa.R
import com.lyy.keepassa.util.KpaUtil

/**
 * zzhoujay RichText 3.0.8 / markdown 1.0.5 把标题/引用等颜色硬编码在
 * StyleBuilderImpl 静态字段里,夜间模式不可读。资源覆盖无效,
 * 反射在新版 Android 上不可靠。这里改为:渲染完成后遍历 Spanned,
 * 把过暗的 ForegroundColorSpan 替换为夜间友好色。
 *
 * 调用方式:RichText.fromMarkdown(md).done { fixDarkSpans(tv) }.into(tv)
 */
object RichTextNightMode {

  private val DARK_THRESHOLD = 0.45f

  fun fixDarkSpans(textView: TextView) {
    if (!KpaUtil.isNightMode()) return

    val src = textView.text as? android.text.Spanned ?: return
    val spans = src.getSpans(0, src.length, ForegroundColorSpan::class.java) ?: return
    val needFix = spans.any { luminance(it.foregroundColor) < DARK_THRESHOLD }
    if (!needFix) return

    // TextView 默认把 Spanned 包成 SpannedString(不可变),必须转成可变的 Spannable 再写回
    val mutable = android.text.SpannableString.valueOf(src)
    val targetColor = ResUtil.getColor(R.color.text_black_color)
    val linkColor = ResUtil.getColor(R.color.color_4E85DB)
    mutable.getSpans(0, mutable.length, ForegroundColorSpan::class.java).forEach { span ->
      if (luminance(span.foregroundColor) < DARK_THRESHOLD) {
        val s = mutable.getSpanStart(span)
        val e = mutable.getSpanEnd(span)
        val flags = mutable.getSpanFlags(span)
        val repl = if (isBlue(span.foregroundColor)) linkColor else targetColor
        mutable.removeSpan(span)
        mutable.setSpan(ForegroundColorSpan(repl), s, e, flags)
      }
    }
    textView.setText(mutable)
  }

  private fun luminance(color: Int): Float {
    val r = Color.red(color) / 255f
    val g = Color.green(color) / 255f
    val b = Color.blue(color) / 255f
    return 0.299f * r + 0.587f * g + 0.114f * b
  }

  private fun isBlue(color: Int): Boolean {
    val r = Color.red(color)
    val g = Color.green(color)
    val b = Color.blue(color)
    return b > r && b > g
  }
}
