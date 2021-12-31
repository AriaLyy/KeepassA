/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.fingerprint

import android.annotation.TargetApi
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewAnimationUtils
import android.view.animation.AccelerateInterpolator
import android.widget.Button
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.arialyy.frame.router.Routerfit
import com.arialyy.frame.util.ResUtil
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseActivity
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.databinding.ActivityFingerprintBinding
import com.lyy.keepassa.router.DialogRouter
import com.lyy.keepassa.util.FingerprintUtil
import com.lyy.keepassa.view.dialog.OnMsgBtClickListener

/**
 * 指纹签名验证，切换功能后，如果不重新验证指纹，弹出对话框提示验证
 */
@TargetApi(Build.VERSION_CODES.M)
@Route(path = "/setting/fingerprint")
class FingerprintActivity : BaseActivity<ActivityFingerprintBinding>() {
  private lateinit var module: FingerprintModule
  private val closedFragment: FingerprintCloseFragment by lazy {
    FingerprintCloseFragment()
  }
  private val fingerprintDescFragment: FingerprintDescFragment by lazy {
    FingerprintDescFragment()
  }

  companion object {
    const val FLAG_CLOSE = 1
    const val FLAG_FULL_UNLOCK = 2
    const val FLAG_QUICK_UNLOCK = 3

    fun toFingerprintActivity(ac: FragmentActivity) {
      ARouter.getInstance()
        .build("/setting/fingerprint")
        .withOptionsCompat(
          ActivityOptionsCompat.makeSceneTransitionAnimation(ac)
        )
        .navigation(ac)
    }
  }

  override fun setLayoutId(): Int {
    return R.layout.activity_fingerprint
  }

  override fun initData(savedInstanceState: Bundle?) {
    super.initData(savedInstanceState)
    module = ViewModelProvider(this).get(FingerprintModule::class.java)
    if (!FingerprintUtil.hasBiometricPrompt(this) || BaseApp.dbRecord == null) {
      finishAfterTransition()
      return
    }
    toolbar.title = getString(R.string.fingerprint_unlock)

    // 需要放在getQuickUnlockRecord之前
    binding.swBt.setOnCheckedChangeListener { _, isChecked ->
      changeBg(isChecked)
    }

    module.getQuickUnlockRecord(BaseApp.dbRecord!!.localDbUri)
      .observe(this, Observer { record ->
        if (record == null) {
          module.oldFlag = FLAG_CLOSE
          module.curFlag = FLAG_CLOSE
          binding.swBt.isChecked = false
          supportFragmentManager.beginTransaction()
            .replace(R.id.flContent, closedFragment)
            .commitAllowingStateLoss()
          return@Observer
        }
        if (!record.isUseFingerprint) {
          module.oldFlag = FLAG_QUICK_UNLOCK
          module.curFlag = FLAG_QUICK_UNLOCK
          binding.swBt.isChecked = true
          return@Observer
        }
        binding.swBt.isChecked = true
        module.oldFlag = FLAG_FULL_UNLOCK
        module.curFlag = FLAG_FULL_UNLOCK
      })
  }

  override fun finishAfterTransition() {
    // 当前标志不为关闭，并且当前标志和进入的标志不一致，则需要提示用户验证指纹
    if (module.curFlag != FLAG_CLOSE && module.curFlag != module.oldFlag) {
      Routerfit.create(DialogRouter::class.java).toMsgDialog(
        msgContent = ResUtil.getString(R.string.hint_finger_print_verfiy),
        btnClickListener = object : OnMsgBtClickListener {
          override fun onCover(v: Button) {
          }

          override fun onEnter(v: Button) {
            fingerprintDescFragment.showBiometricPrompt()
          }

          override fun onCancel(v: Button) {
            super@FingerprintActivity.finishAfterTransition()
          }
        }
      )
        .show()
    } else {
      super@FingerprintActivity.finishAfterTransition()
    }
  }

  override fun onBackPressed() {
//    super.onBackPressed()
    finishAfterTransition()
  }

  /**
   * 切换fragment改变背景
   */
  private fun changeBg(isChecked: Boolean) {
    val view = findViewById<View>(R.id.vBg)
    if (isDestroyed || isFinishing || !view.isAttachedToWindow) {
      return
    }
    if (isChecked) {
      // 打开
      binding.closeHint.text = getString(R.string.open1)
      binding.flSwitch.setBackgroundColor(getColor(R.color.grey600))
      supportFragmentManager.beginTransaction()
        .replace(R.id.flContent, fingerprintDescFragment)
        .commitAllowingStateLoss()
    } else {
      // 关闭
      module.curFlag = FLAG_CLOSE
      binding.closeHint.text = getString(R.string.disable)
      binding.flSwitch.setBackgroundColor(getColor(R.color.colorAccent))
//      HitUtil.toaskShort("${getString(R.string.fingerprint_unlock)}${getString(R.string.closed)}")
      module.deleteQuickInfo()
      supportFragmentManager.beginTransaction()
        .replace(R.id.flContent, closedFragment)
        .commitAllowingStateLoss()
    }

    val finalRadius = view.width.coerceAtLeast(view.height)
    val anim = ViewAnimationUtils.createCircularReveal(
      view, if (isChecked) view.right else 0, 0, 0f, finalRadius.toFloat()
    )
    view.setBackgroundResource(
      if (isChecked) R.color.colorPrimary else R.color.grey600
    )
    anim.duration = resources.getInteger(R.integer.anim_duration_long)
      .toLong()
    anim.interpolator = AccelerateInterpolator()
    anim.start()
  }
}