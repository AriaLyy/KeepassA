/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnticipateOvershootInterpolator
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageButton
import com.example.uiwidget.R
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainExpandFloatActionButton(
  context: Context,
  attributeSet: AttributeSet
) : LinearLayout(context, attributeSet), View.OnClickListener {

  private var fab: FloatingActionButton
  private var addKey: AppCompatImageButton
  private var addGroup: AppCompatImageButton
  private var animIsRunning = false
  private var itemListener: OnItemClickListener? = null
  public var isShowOperate = false

  public interface OnItemClickListener {
    fun onKeyClick()

    fun onGroupClick()
  }

  init {
    LayoutInflater.from(context)
        .inflate(R.layout.layout_expadn_float_action_bt, this, true)
    fab = findViewById(R.id.ex_fab)
    addKey = findViewById(R.id.add_key)
    addGroup = findViewById(R.id.add_group)
    fab.setOnClickListener(this)
    addKey.setOnClickListener(this)
    addGroup.setOnClickListener(this)
  }

  public fun setOnItemClickListener(listener: OnItemClickListener) {
    this.itemListener = listener
  }

  /**
   * fab 按钮的操作
   */
  public fun showMoreOperate() {
    animIsRunning = true
    isShowOperate = true
    addKey.visibility = View.VISIBLE
    addKey.layoutParams.width = 0
    addKey.layoutParams.height = 0
    addGroup.visibility = View.VISIBLE
    addGroup.layoutParams.width = 0
    addGroup.layoutParams.height = 0

    val valueAnimator = ValueAnimator.ofInt(0, resources.getDimension(R.dimen.fab_bt_size).toInt())
        .setDuration(300)
    valueAnimator.addUpdateListener { animation ->
      addKey.layoutParams.width = animation.animatedValue as Int
      addKey.layoutParams.height = animation.animatedValue as Int
      addKey.requestLayout()

      addGroup.layoutParams.width = animation.animatedValue as Int
      addGroup.layoutParams.height = animation.animatedValue as Int
      addGroup.requestLayout()
    }
    valueAnimator.interpolator = AccelerateDecelerateInterpolator()
    valueAnimator.addListener(object : AnimatorListenerAdapter() {
      override fun onAnimationEnd(animation: Animator?) {
        super.onAnimationEnd(animation)
        animIsRunning = false
      }
    })
    valueAnimator.start()
    val fabAnim = ObjectAnimator.ofFloat(fab, "rotation", 0f, 45f)
    fabAnim.duration = 200
    fabAnim.interpolator = AnticipateOvershootInterpolator()
    fabAnim.start()
  }

  fun hintMoreOperateNoAnim(){
    addKey.visibility = View.GONE
    addGroup.visibility = View.GONE
    fab.rotation = 0f
    isShowOperate = false
  }

  /**
   * 隐藏fab 的更多操作
   */
  public fun hintMoreOperate() {
    animIsRunning = true
    isShowOperate = false
    val w = resources.getDimension(R.dimen.fab_bt_size)
        .toInt()
    addKey.layoutParams.width = w
    addKey.layoutParams.height = w
    addGroup.layoutParams.width = w
    addGroup.layoutParams.height = w

    val valueAnimator = ValueAnimator.ofInt(w, 0)
        .setDuration(400)
    valueAnimator.addUpdateListener { animation ->
      addKey.layoutParams.width = animation.animatedValue as Int
      addKey.layoutParams.height = animation.animatedValue as Int
      addKey.requestLayout()

      addGroup.layoutParams.width = animation.animatedValue as Int
      addGroup.layoutParams.height = animation.animatedValue as Int
      addGroup.requestLayout()
    }
    valueAnimator.interpolator = AccelerateDecelerateInterpolator()
    valueAnimator.addListener(object : AnimatorListenerAdapter() {
      override fun onAnimationEnd(animation: Animator?) {
        super.onAnimationEnd(animation)
        animIsRunning = false
      }
    })
    valueAnimator.start()

    val fabAnim = ObjectAnimator.ofFloat(fab, "rotation", 45f, 0f)
    fabAnim.duration = 200
    fabAnim.interpolator = AnticipateOvershootInterpolator()
    fabAnim.start()
  }

  override fun onClick(v: View?) {
    when (v!!.id) {
      R.id.ex_fab -> {
        if (animIsRunning) {
          return
        }
        if (isShowOperate) {
          hintMoreOperate()
        } else {
          showMoreOperate()
        }
      }
      R.id.add_key -> itemListener?.onKeyClick()
      R.id.add_group -> itemListener?.onGroupClick()
    }
  }

}