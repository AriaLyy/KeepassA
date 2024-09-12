/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.main

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.view.View
import android.view.ViewAnimationUtils
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.arialyy.frame.router.Routerfit
import com.arialyy.frame.util.ResUtil
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.ScreenUtils
import com.lahm.library.EasyProtectorLib
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.BaseModule
import com.lyy.keepassa.databinding.ActivityMainBinding
import com.lyy.keepassa.router.DialogRouter
import com.lyy.keepassa.util.InterpolatorConstance
import com.lyy.keepassa.util.KpaUtil
import com.lyy.keepassa.view.main.chain.AutoFillPermissionsChain
import com.lyy.keepassa.view.main.chain.DevBirthdayChain
import com.lyy.keepassa.view.main.chain.DialogChain
import com.lyy.keepassa.view.main.chain.DonateChain
import com.lyy.keepassa.view.main.chain.IMainDialogInterceptor
import com.lyy.keepassa.view.main.chain.NotifyPermissionsChain
import com.lyy.keepassa.view.main.chain.ReviewChain
import com.lyy.keepassa.view.main.chain.VersionLogChain
import com.lyy.keepassa.widget.BubbleTextView
import com.lyy.keepassa.widget.BubbleTextView.OnIconClickListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

class MainModule : BaseModule() {

  override fun onCleared() {
    super.onCleared()
  }

  fun startRevealAnim(binding: ActivityMainBinding) {
    val vAnim = AnimatorSet()
    val revealAnimal = ViewAnimationUtils.createCircularReveal(
      binding.root,
      ScreenUtils.getScreenWidth() / 2,
      ScreenUtils.getScreenHeight() / 2,
      0.toFloat(),
      max(ScreenUtils.getScreenWidth().toFloat(), ScreenUtils.getScreenHeight().toFloat())
    )

    val contentAnim2 = ObjectAnimator.ofFloat(binding.root, View.ALPHA, 0f, 1f)

    vAnim.duration = 400
    vAnim.doOnStart {
      binding.root.alpha = 0f
    }
    vAnim.doOnEnd {
      ActivityUtils.getActivityList().forEach {
        if (KpaUtil.isHomeActivity(it)) {
          ActivityUtils.finishActivity(it)
        }
      }
    }
    vAnim.playTogether(revealAnimal, contentAnim2)
    vAnim.interpolator = InterpolatorConstance.easeInCubic
    vAnim.start()
  }

  fun getAddIcon(): VectorDrawableCompat? {
    return ResUtil.getSvgIcon(R.drawable.ic_add_24px, R.color.color_FFFFFF)
  }

  /**
   * 安全级别对话框
   * root 红色，提示危险
   */
  fun setEcoIcon(
    cxt: Context,
    btText: BubbleTextView
  ) {
    val needCheckEnv = PreferenceManager.getDefaultSharedPreferences(cxt)
      .getBoolean(cxt.resources.getString(R.string.set_key_need_root_check), true)
    if (!needCheckEnv) {
      btText.clearIcon(BubbleTextView.LOCATION_RIGHT)
      return
    }

    var vector = ResUtil.getSvgIcon(R.drawable.ic_eco, R.color.green)
    var msg = cxt.getString(R.string.hint_security_green)
    if (EasyProtectorLib.checkIsRoot()) {
      vector = ResUtil.getSvgIcon(R.drawable.ic_eco, R.color.red)
      msg = cxt.getString(R.string.hint_security_red)
    } else if (EasyProtectorLib.checkIsRunningInEmulator(cxt) {
//          BuglyLog.d(TAG, it)
      }) {
      vector = ResUtil.getSvgIcon(R.drawable.ic_eco, R.color.yellow)
      msg = cxt.getString(R.string.hint_security_yellow)
    }
    btText.setEndIcon(vector!!)
    btText.setOnIconClickListener(object : OnIconClickListener {
      override fun onClick(
        view: BubbleTextView,
        index: Int
      ) {
        if (index == 2) {
          Routerfit.create(DialogRouter::class.java).showMsgDialog(
            msgContent = msg,
            showCancelBt = false,
            msgTitleEndIcon = vector
          )
        }
      }
    })
  }

  fun showInfoDialog(activity: MainActivity) {

    viewModelScope.launch {
      withContext(Dispatchers.IO) {
        delay(600)
      }
      val list = arrayListOf<IMainDialogInterceptor>().apply {
        add(VersionLogChain())
        add(DevBirthdayChain())
        add(DonateChain())
        add(ReviewChain())
        add(NotifyPermissionsChain())
        add(AutoFillPermissionsChain())
        // add(TipChain())
      }
      DialogChain(activity, list, 0).proceed(activity)

    }
  }

  /**
   * 检查是否有记录
   */
  fun checkHasHistoryRecord() = liveData {
    BaseApp.dbRecord?.let {
      if (BaseApp.dbRecord == null || it.localDbUri.isEmpty()) {
        emit(false)
        return@liveData
      }
      val b = withContext(Dispatchers.IO) {
        val dao = BaseApp.appDatabase.entryRecordDao()
        dao.hasRecord(it.localDbUri) > 0
      }
      emit(b)
    }

  }
}