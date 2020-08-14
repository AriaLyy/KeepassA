package com.lyy.keepassa.widget

import android.content.Context
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatTextView
import com.example.uiwidget.R

/**
 * 图标可点击的textview
 */
class BubbleTextView(
  context: Context,
  attrs: AttributeSet?
) : AppCompatTextView(context, attrs) {
  private var iconSize: Int
  private var iconClickListener: OnIconClickListener? = null

  public interface OnIconClickListener {
    /**
     * 0：点击左边的icon，1：点击顶部的icon，2：点击右边的icon，3：点击底部的icon
     */
    fun onClick(
      view: BubbleTextView,
      index: Int
    )
  }

  constructor(context: Context) : this(context, null)

  init {
    val ta = context.obtainStyledAttributes(attrs, R.styleable.BubbleTextView)
    val iconSize = ta.getDimension(
        R.styleable.BubbleTextView_icon_size,
        24.toPx().toFloat()
    )
    val leftIcon = ta.getDrawable(R.styleable.BubbleTextView_left_icon)
    val topIcon = ta.getDrawable(R.styleable.BubbleTextView_top_icon)
    val rightIcon = ta.getDrawable(R.styleable.BubbleTextView_right_icon)
    val bottomIcon = ta.getDrawable(R.styleable.BubbleTextView_bottom_icon)

    if (leftIcon != null) {
      leftIcon.bounds = Rect(0, 0, iconSize.toInt(), iconSize.toInt())
    }
    if (topIcon != null) {
      topIcon.bounds = Rect(0, 0, iconSize.toInt(), iconSize.toInt())
    }
    if (rightIcon != null) {
      rightIcon.bounds = Rect(0, 0, iconSize.toInt(), iconSize.toInt())
    }
    if (bottomIcon != null) {
      bottomIcon.bounds = Rect(0, 0, iconSize.toInt(), iconSize.toInt())
    }
    setCompoundDrawables(leftIcon, topIcon, rightIcon, bottomIcon)
    this.iconSize = iconSize.toInt()
    ta.recycle()
    isClickable = true
  }

  fun setLeftIcon(drawable: Drawable) {
    drawable.bounds = Rect(0, 0, iconSize, iconSize)
    setCompoundDrawables(drawable, compoundDrawables[1], compoundDrawables[2], compoundDrawables[3])
  }

  fun setLeftIcon(@DrawableRes drawableId: Int) {
    val drawable = resources.getDrawable(drawableId, context.theme)
    drawable.bounds = Rect(0, 0, iconSize, iconSize)
    setCompoundDrawables(drawable, compoundDrawables[1], compoundDrawables[2], compoundDrawables[3])
  }

  fun setEndIcon(drawable: Drawable) {
    drawable.bounds = Rect(0, 0, iconSize, iconSize)
    setCompoundDrawables(compoundDrawables[0], compoundDrawables[1], drawable, compoundDrawables[3])
  }

  fun setEndIcon(@DrawableRes drawableId: Int) {
    val drawable = resources.getDrawable(drawableId, context.theme)
    drawable.bounds = Rect(0, 0, iconSize, iconSize)
    setCompoundDrawables(compoundDrawables[0], compoundDrawables[1], drawable, compoundDrawables[3])
  }

  override fun onTouchEvent(event: MotionEvent?): Boolean {
    val clickIconIndex = isTouchIcon(event!!)
    if (clickIconIndex == -1) {
      return super.onTouchEvent(event)
    }

    if (iconClickListener != null) {
      iconClickListener!!.onClick(this, clickIconIndex)
    }

    return true
  }

  public fun setOnIconClickListener(listener: OnIconClickListener) {
    iconClickListener = listener
  }

  /**
   * 检查是否触摸在icon上
   * 参考地址：https://www.jianshu.com/p/6f7673e51395 他写的也有问题，只需要参考图便可
   * @return -1：没有点击icon，0：点击左边的icon，1：点击顶部的icon，2：点击右边的icon，3：点击底部的icon
   */
  private fun isTouchIcon(event: MotionEvent): Int {
    if (compoundDrawables.isNotEmpty() && event.action == MotionEvent.ACTION_DOWN) {
//      Log.d("TAG", "ex = ${event.x}, ey = ${event.y}")
      val contentRect = contentRect()

      for (index in 0..3) {
        val icon = compoundDrawables[index]
        compoundDrawables
        if (icon != null) {

          var l = 0
          var t = 0
          var r = 0
          var b = 0

          if (index == 0) { // 左边的icon
            l = 0
            t = contentRect.top
          }

          if (index == 1) { // 顶部的icon
            // 需要考虑左边的图片是否存在，顶部或底部的图片总是在内容的中间
            l = (compoundPaddingLeft + contentRect.right - icon.bounds.width()) / 2
            t = 0
          }

          if (index == 2) { // 右边的icon
            l = contentRect.right
            t = contentRect.top
          }

          if (index == 3) { // 底部的icon
            l = (compoundPaddingLeft + contentRect.right - icon.bounds.width()) / 2
            t = contentRect.bottom
          }

          r = l + icon.bounds.width()
          b = t + icon.bounds.height()

          val temp = RectF(l.toFloat(), t.toFloat(), r.toFloat(), b.toFloat())
          if (temp.contains(event.x, event.y)) {
            return index
          }
        }
      }
    }
    return -1

  }

  /**
   * 文字内容区域
   */
  private fun contentRect(): Rect {
    return Rect(
        compoundPaddingLeft,
        compoundPaddingTop,
        width - compoundPaddingRight,
        height - compoundPaddingBottom
    )
  }

}