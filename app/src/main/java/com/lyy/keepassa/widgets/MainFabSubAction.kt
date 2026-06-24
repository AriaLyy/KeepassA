package com.lyy.keepassa.widgets

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import com.lyy.keepassa.databinding.LayoutMainFabSubActionBinding
import com.lyy.keepassa.util.loadImg

/**
 * @Author laoyuyu
 * @Description
 * @Date 19:38 2024/9/10
 **/
class MainFabSubAction(context: Context, attr: AttributeSet?) : FrameLayout(context, attr) {
  private val binding =
    LayoutMainFabSubActionBinding.inflate(LayoutInflater.from(context), this, true)

  fun setImg(@DrawableRes resId: Int) {
    binding.img.loadImg(resId)
  }

  fun setDrawable(drawable: Drawable?){
    binding.img.setImageDrawable(drawable)
  }
}