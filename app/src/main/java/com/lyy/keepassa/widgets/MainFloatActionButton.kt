package com.lyy.keepassa.widgets

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.google.android.material.imageview.ShapeableImageView

/**
 * @Author laoyuyu
 * @Description
 * @Date 13:40 2024/9/10
 **/
class MainFloatActionButton(context: Context, attr: AttributeSet) :
  ShapeableImageView(context, attr) {

  interface OnOperateCallback {
    // fun onShow(view: MainFloatActionButton)

    fun onHint(view: MainFloatActionButton)
  }

  var callback: OnOperateCallback? = null
}