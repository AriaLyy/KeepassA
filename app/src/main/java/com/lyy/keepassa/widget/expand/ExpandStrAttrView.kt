/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.widget.expand

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.arialyy.frame.util.StringUtil
import com.keepassdroid.database.security.ProtectedString
import timber.log.Timber

/**
 * 可展开的自定义属性view
 */
class ExpandStrAttrView(
  context: Context,
  attr: AttributeSet
) : LinearLayout(context, attr), View.OnClickListener {

  private val TAG = StringUtil.getClassName(this)
  private val data = LinkedHashMap<String, ProtectedString>()
  private val childMap = LinkedHashMap<String, AttrStrItemView>()
  private var expandAnim: ValueAnimator? = null
  private var hintAnim: ValueAnimator? = null
  private val animDuring = 200L
  private var strViewListener: OnAttrStrViewClickListener? = null

  /**
   * 高级属性的view点击事件
   */
  interface OnAttrStrViewClickListener {
    fun onClickListener(
      v: AttrStrItemView,
      key: String,
      str: ProtectedString,
      position: Int
    )
  }

  init {
    orientation = VERTICAL
  }

  override fun removeAllViews() {
    data.clear()
    childMap.clear()
    super.removeAllViews()
  }

  /**
   * 设置高级属性的view点击事件
   */
  public fun setOnStrViewClickListener(listener: OnAttrStrViewClickListener) {
    this.strViewListener = listener
  }

  fun setValue(map: LinkedHashMap<String, ProtectedString>) {
    data.putAll(map)
    if (data.isNotEmpty()) {
      var i = 0
      for (d in data) {
        val child = AttrStrItemView(context, d.key, d.value)
        child.setOnClickListener(this)
        child.isClickable = true
        addView(child, i)
        childMap[d.key] = child
        i++
      }
    }
    invalidate()
  }

  fun addValue(
    key: String,
    value: ProtectedString
  ) {
    data[key] = value
    val child = AttrStrItemView(context, key, value)
    child.setOnClickListener(this)
    addView(child, childCount)
    childMap[key] = child
    invalidate()
  }

  fun removeValue(key: String) {
    if (key.isEmpty() || !data.keys.contains(key)) {
      Timber.e("key【$key】错误")
      return
    }
    removeView(childMap[key])
    childMap.remove(key)
    invalidate()
  }

  /**
   * 更新数据
   */
  fun updateKeyValue(
    v: AttrStrItemView,
    key: String,
    value: ProtectedString
  ) {
    v.updateValue(key, value)
    invalidate()
  }

  /**
   * 展开
   */
  public fun expand() {
    if (hintAnim != null && hintAnim!!.isRunning) {
      hintAnim!!.cancel()
    }
    alpha = 0f
    visibility = View.VISIBLE
    if (expandAnim == null) {
      expandAnim = ValueAnimator.ofFloat(0f, 1f)
      expandAnim!!.addUpdateListener { animation ->
        alpha = animation.animatedValue as Float
        requestLayout()
      }
    }
    expandAnim!!.duration = animDuring
    expandAnim!!.start()
  }

  /**
   * 隐藏
   */
  fun hint() {
    if (expandAnim != null && expandAnim!!.isRunning) {
      expandAnim!!.cancel()
    }
    if (hintAnim == null) {
      hintAnim = ValueAnimator.ofFloat(1.0f, 0f)
      hintAnim!!.addUpdateListener { animation ->
        alpha = animation.animatedValue as Float
        requestLayout()
      }
      hintAnim!!.addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
          super.onAnimationEnd(animation)
          visibility = View.GONE
        }
      })
    }
    hintAnim!!.duration = animDuring
    hintAnim!!.start()
  }

  override fun onClick(v: View?) {
    if (strViewListener != null) {
      strViewListener!!.onClickListener(
          v!! as AttrStrItemView, (v as AttrStrItemView).titleStr, v.valueInfo,
          childMap.values.indexOf(v)
      )
    }
  }

}