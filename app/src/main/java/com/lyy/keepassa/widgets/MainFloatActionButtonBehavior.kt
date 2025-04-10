/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.widgets

import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams
import androidx.core.view.ViewCompat
import com.lyy.keepassa.util.InterpolatorConstance

public class MainFloatActionButtonBehavior(
  context: Context,
  attributeSet: AttributeSet
) : CoordinatorLayout.Behavior<MainFloatActionButton>(context, attributeSet) {

  private var outAnim: ObjectAnimator? = null
  private var inAnim: ObjectAnimator? = null

  override fun onNestedScroll(
    coordinatorLayout: CoordinatorLayout,
    child: MainFloatActionButton,
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
    if (dyConsumed > 0 && (outAnim == null || !outAnim!!.isRunning)) { // 向下滑动
      animateOut(child)
      return
    }
    if (dyConsumed < 0 && (inAnim == null || !inAnim!!.isRunning)) { // 向上滑动
      animateIn(child)
    }
  }

  override fun onNestedPreScroll(
    coordinatorLayout: CoordinatorLayout,
    child: MainFloatActionButton,
    target: View,
    dx: Int,
    dy: Int,
    consumed: IntArray,
    type: Int
  ) {
    if (dy > 0 && (outAnim == null || !outAnim!!.isRunning)) { // 向下滑动
      animateOut(child)
      return
    }
    if (dy < 0 && (inAnim == null || !inAnim!!.isRunning)) { // 向上滑动
      animateIn(child)
    }
  }

  override fun onStartNestedScroll(
    coordinatorLayout: CoordinatorLayout,
    child: MainFloatActionButton,
    directTargetChild: View,
    target: View,
    axes: Int,
    type: Int
  ): Boolean {
    return axes == ViewCompat.SCROLL_AXIS_VERTICAL
  }

  // FAB移出屏幕动画（隐藏动画）
  private fun animateOut(fab: MainFloatActionButton) {

    val layoutParams =
      fab.layoutParams as LayoutParams
    val bottomMargin = layoutParams.bottomMargin
    if (fab.translationY >= fab.height + bottomMargin.toFloat()) {
      return
    }

    if (outAnim != null && outAnim!!.isRunning) {
      outAnim!!.cancel()
    }
    fab.callback?.onHint(fab)
    outAnim =
      ObjectAnimator.ofFloat(fab, View.TRANSLATION_Y, 0f, fab.height + bottomMargin.toFloat())
    outAnim!!.duration = 200
    outAnim!!.interpolator = InterpolatorConstance.easeOutCubic
    outAnim!!.start()
  }

  // FAB移入屏幕动画（显示动画）
  private fun animateIn(fab: View) {

    if (fab.translationY <= 0f) {
      return
    }

    if (inAnim != null && inAnim!!.isRunning) {
      inAnim!!.cancel()
    }
    inAnim = ObjectAnimator.ofFloat(fab, View.TRANSLATION_Y, fab.translationY, 0f)
    inAnim!!.duration = 200
    inAnim!!.interpolator = InterpolatorConstance.easeInCubic
    inAnim!!.start()
  }
}