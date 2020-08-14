package com.lyy.keepassa.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatRadioButton
import com.example.uiwidget.R

/**
 * 可设置右边图标的radioButton
 */
class BubbleRadioButton(
  context: Context,
  attributeSet: AttributeSet
) : AppCompatRadioButton(context, attributeSet) {
  private val iconSize: Int

  private var iconClickListener: OnIconClickListener? = null

  public interface OnIconClickListener {
    /**
     * 0：点击左边的icon，1：点击顶部的icon，2：点击右边的icon，3：点击底部的icon
     */
    fun onClick(
      view: BubbleRadioButton
    )
  }

  init {
    val ta = context.obtainStyledAttributes(attributeSet, R.styleable.BubbleRadioButton)
    iconSize = ta.getDimension(
        R.styleable.BubbleRadioButton_rb_icon_size,
        25.toPx().toFloat()
    )
        .toInt()
    val rightIcon = ta.getDrawable(R.styleable.BubbleRadioButton_rb_right_icon)

    if (rightIcon != null) {
      rightIcon.bounds = Rect(0, 0, iconSize, iconSize)
    }
    setCompoundDrawables(
        compoundDrawables[0], compoundDrawables[1], rightIcon, compoundDrawables[3]
    )
    ta.recycle()
  }

  @SuppressLint("ClickableViewAccessibility")
  override fun onTouchEvent(event: MotionEvent?): Boolean {
    val clickIconIndex = isTouchIcon(event!!)
    if (clickIconIndex == -1) {
      return super.onTouchEvent(event)
    }

    if (iconClickListener != null) {
      iconClickListener!!.onClick(this)
    }

    return true
  }

  public fun setOnIconClickListener(listener: OnIconClickListener) {
    iconClickListener = listener
  }

  /**
   * 检查是否触摸在icon上
   * @return -1：没有点击右边icon，0：点击右边的icon
   */
  private fun isTouchIcon(event: MotionEvent): Int {
    if (compoundDrawables.isNotEmpty() && event.action == MotionEvent.ACTION_DOWN) {
//      Log.d("TAG", "ex = ${event.x}, ey = ${event.y}")
      val contentRect = contentRect()

      val icon = compoundDrawables[2]
      compoundDrawables
      if (icon != null) {
        val l = contentRect.right
        val t = contentRect.top
        val r = l + icon.bounds.width()
        val b = t + icon.bounds.height()

        val temp = RectF(l.toFloat(), t.toFloat(), r.toFloat(), b.toFloat())
        if (temp.contains(event.x, event.y)) {
          return 0
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