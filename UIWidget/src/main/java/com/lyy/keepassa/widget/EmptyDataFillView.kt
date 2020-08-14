package com.lyy.keepassa.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageView
import com.example.uiwidget.R

/**
 * 空数据填充view
 */
class EmptyDataFillView(
  context: Context,
  attrs: AttributeSet?
) : RelativeLayout(context, attrs) {

  private val icon: AppCompatImageView
  private val text: TextView

  init {
    LayoutInflater.from(context)
        .inflate(R.layout.layout_empty_data_fill, this, true)
    icon = findViewById(R.id.edf_icon)
    text = findViewById(R.id.edf_txt)
    val a = context.obtainStyledAttributes(attrs, R.styleable.EmptyDataFillView)
    val tx = a.getString(R.styleable.EmptyDataFillView_edf_text)
    val drawable = a.getDrawable(R.styleable.EmptyDataFillView_edf_icon)
    a.recycle()
    if (tx != null) {
      text.text = tx
    }
    if (drawable != null) {
      icon.setImageDrawable(drawable)
    }
  }

  public fun setIcon(@DrawableRes res: Int) {
    icon.setImageResource(res)
  }

  public fun setText(str: CharSequence) {
    text.text = str
  }

}