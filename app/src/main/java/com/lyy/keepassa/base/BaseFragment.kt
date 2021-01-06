/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.base

import android.content.Intent
import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.Slide
import android.transition.TransitionSet
import android.view.Gravity
import android.view.View
import androidx.databinding.ViewDataBinding
import com.arialyy.frame.core.AbsFragment
import com.lyy.keepassa.util.AutoLockDbUtil
import com.lyy.keepassa.util.KeepassAUtil

abstract class BaseFragment<VB : ViewDataBinding> : AbsFragment<VB>() {

  override fun onActivityCreated(savedInstanceState: Bundle?) {
//    setWindowAnim()
    super.onActivityCreated(savedInstanceState)
  }

  override fun dataCallback(
    result: Int,
    obj: Any
  ) {

  }

  fun getRootView(): View = mRootView

  private fun setWindowAnim() {
    // salide 为滑入，其它动画效果参考：https://github.com/lgvalle/Material-Animations
    // 第一次进入activity的动画
    val enterSet = TransitionSet()
    enterSet.addTransition(Slide(Gravity.END))
        .addTransition(ChangeBounds()) // 右边进入左边
    enterSet.duration = 400
    enterSet.excludeTarget(android.R.id.navigationBarBackground, true) // 导航栏不参与动画
    enterSet.excludeTarget(android.R.id.statusBarBackground, true) // 状态栏不参与动画
    enterTransition = enterSet

    // 退出当前activity的动画
    val exitSet = TransitionSet()
    exitSet.addTransition(Slide(Gravity.START))
        .addTransition(ChangeBounds()) // 左边到右边
    exitSet.duration = 400
    exitSet.excludeTarget(android.R.id.navigationBarBackground, true) // 导航栏不参与动画
    exitSet.excludeTarget(android.R.id.statusBarBackground, true) // 状态栏不参与动画
    exitTransition = exitSet

    // 重新进入activity的动画
    val reEnterSet = TransitionSet()
    reEnterSet.addTransition(Slide(Gravity.END))
        .addTransition(ChangeBounds())
    reEnterSet.duration = 400
    reEnterSet.excludeTarget(android.R.id.navigationBarBackground, true) // 导航栏不参与动画
    reEnterSet.excludeTarget(android.R.id.statusBarBackground, true) // 状态栏不参与动画
    returnTransition = reEnterSet

  }

  override fun onResume() {
    super.onResume()
    if ( KeepassAUtil.instance.isStartQuickLockActivity(this)) {
      if (BaseApp.isLocked) {
        AutoLockDbUtil.get().startLockWorkerNow()
        return
      }
      if ( KeepassAUtil.instance.isRunningForeground(requireActivity())) {
        AutoLockDbUtil.get().resetTimer()
        return
      }
    }
  }

  override fun onDelayLoad() {

  }

  override fun startActivity(
    intent: Intent?,
    options: Bundle?
  ) {

    super.startActivity(intent, options)
  }
}