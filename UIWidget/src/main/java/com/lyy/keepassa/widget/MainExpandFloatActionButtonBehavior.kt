/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.widget

import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams
import androidx.core.view.ViewCompat

public class MainExpandFloatActionButtonBehavior(
  context: Context,
  attributeSet: AttributeSet
) : CoordinatorLayout.Behavior<MainExpandFloatActionButton>(context, attributeSet) {

  private var outAnim: ObjectAnimator? = null
  private var inAnim: ObjectAnimator? = null

  override fun onNestedScroll(
    coordinatorLayout: CoordinatorLayout,
    child: MainExpandFloatActionButton,
    target: View,
    dxConsumed: Int,
    dyConsumed: Int,
    dxUnconsumed: Int,
    dyUnconsumed: Int,
    type: Int,
    consumed: IntArray
  ) {
    super.onNestedScroll(
        coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed,
        dyUnconsumed, type, consumed
    )
    if (dyConsumed > 0) { // 向下滑动
      if (outAnim == null || !outAnim!!.isRunning) {
        animateOut(child)
      }
    } else if (dyConsumed < 0) { // 向上滑动
      animateIn(child)
    }
  }

  override fun onNestedPreScroll(
    coordinatorLayout: CoordinatorLayout,
    child: MainExpandFloatActionButton,
    target: View,
    dx: Int,
    dy: Int,
    consumed: IntArray,
    type: Int
  ) {
    if (dy > 0) { // 向下滑动
      if (outAnim == null || !outAnim!!.isRunning) {
        animateOut(child)
      }
    } else if (dy < 0) { // 向上滑动
      if (inAnim == null || !inAnim!!.isRunning) {
        animateIn(child)
      }
    }
  }

  override fun onStartNestedScroll(
    coordinatorLayout: CoordinatorLayout,
    child: MainExpandFloatActionButton,
    directTargetChild: View,
    target: View,
    axes: Int,
    type: Int
  ): Boolean {
    return axes == ViewCompat.SCROLL_AXIS_VERTICAL
  }

  // FAB移出屏幕动画（隐藏动画）
  private fun animateOut(fab: MainExpandFloatActionButton) {

    val layoutParams =
      fab.layoutParams as LayoutParams
    val bottomMargin = layoutParams.bottomMargin
    if (fab.translationY >= fab.height + bottomMargin.toFloat()) {
      return
    }

    if (outAnim != null && outAnim!!.isRunning) {
      outAnim!!.cancel()
    }
    if (fab.isShowOperate) {
      fab.hintMoreOperateNoAnim()
    }
    outAnim = ObjectAnimator.ofFloat(fab, "translationY", 0f, fab.height + bottomMargin.toFloat())
    outAnim!!.duration = 200
    outAnim!!.interpolator = LinearInterpolator()
    outAnim!!.start()
  }

  // FAB移入屏幕动画（显示动画）
  private fun animateIn(fab: MainExpandFloatActionButton) {

    if (fab.translationY <= 0f) {
      return
    }

    if (inAnim != null && inAnim!!.isRunning) {
      inAnim!!.cancel()
    }
    inAnim = ObjectAnimator.ofFloat(fab, "translationY", fab.translationY, 0f)
    inAnim!!.duration = 200
    inAnim!!.interpolator = LinearInterpolator()
    inAnim!!.start()

  }

}