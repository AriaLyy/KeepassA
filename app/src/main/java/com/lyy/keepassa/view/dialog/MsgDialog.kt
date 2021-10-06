/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.dialog

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.arialyy.frame.util.ResUtil
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseDialog
import com.lyy.keepassa.databinding.DialogMsgBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @author laoyuyu
 * @date 2021/9/5
 */
@Route(path = "/dialog/msgDialog")
class MsgDialog : BaseDialog<DialogMsgBinding>(), View.OnClickListener {

  @Autowired(name = "enterBtTextColor")
  @JvmField
  var enterBtTextColor: Int = R.color.text_blue_color

  @Autowired(name = "cancelBtTextColor")
  @JvmField
  var cancelBtTextColor: Int = R.color.text_gray_color

  @Autowired(name = "coverBtTextColor")
  @JvmField
  var coverBtTextColor: Int = R.color.text_blue_color

  @Autowired(name = "msgTitle")
  @JvmField
  var msgTitle: CharSequence = ""

  @Autowired(name = "msgContent")
  @JvmField
  var msgContent: CharSequence = ""

  @Autowired(name = "showCancelBt")
  @JvmField
  var showCancelBt: Boolean = true // 显示取消按钮

  @Autowired(name = "showEnterBt")
  @JvmField
  var showEnterBt: Boolean = true // 显示确认按钮

  @Autowired(name = "showCoverBt")
  @JvmField
  var showCoverBt: Boolean = false // 显示覆盖按钮

  @Autowired(name = "showCountDownTimer")
  @JvmField
  var showCountDownTimer: Pair<Boolean, Int> = Pair(false, 5) // 显示确认按钮倒计时定时器，倒计时5s

  @Autowired(name = "interceptBackKey")
  @JvmField
  var interceptBackKey: Boolean = false // 是否拦截返回键

  @Autowired(name = "msgTitleEndIcon")
  @JvmField
  var msgTitleEndIcon: Drawable? = null

  @Autowired(name = "msgTitleStartIcon")
  @JvmField
  var msgTitleStartIcon: Drawable? = null

  @Autowired(name = "enterText")
  @JvmField
  var enterText: CharSequence = ""

  @Autowired(name = "coverText")
  @JvmField
  var coverText: CharSequence = ""

  @Autowired(name = "cancelText")
  @JvmField
  var cancelText: CharSequence = ""

  @Autowired(name = "btnClickListener")
  @JvmField
  var btnClickListener: OnMsgBtClickListener? = null

  override fun setLayoutId(): Int {
    return R.layout.dialog_msg
  }

  override fun initData() {
    super.initData()
    ARouter.getInstance().inject(this)

    msgTitleEndIcon?.let {
      binding.tvTitle.setEndIcon(it)
    }

    msgTitleStartIcon?.let {
      binding.tvTitle.setLeftIcon(it)
    }

    handleCountDown()
    if (interceptBackKey) {
      dialog!!.setOnKeyListener { _, keyCode, _ ->
        return@setOnKeyListener keyCode == KeyEvent.KEYCODE_BACK
      }
    }
    binding.dialog = this
  }

  /**
   * 设置标题左边icon
   */
  fun setTitleStartIcon(icon: Drawable): MsgDialog {
    msgTitleStartIcon = icon
    return this
  }

  /**
   * 设置标题右边icon
   */
  fun setTitleEndIcon(icon: Drawable): MsgDialog {
    msgTitleEndIcon = icon
    return this
  }

  /**
   * 处理倒计时，倒计时结束前，确认按钮，取消按钮，覆盖按钮都不可选择
   */
  @SuppressLint("SetTextI18n")
  private fun handleCountDown() {
    if (showCountDownTimer.first) {
      setBtnsEnable(false)
      lifecycleScope.launch(Dispatchers.Main) {
        val oldText = binding.enter.text
        for (i in showCountDownTimer.second downTo 1) {
          binding.enter.text = "$oldText (${i} s) "
          withContext(Dispatchers.IO) {
            delay(1000)
          }
        }
        binding.enter.text = oldText
        setBtnsEnable(true)
      }
    }
  }

  private fun setBtnsEnable(enable: Boolean) {
    val btns = arrayListOf(binding.cover, binding.enter, binding.cancel)
    for (btn in btns) {
      if (btn.visibility == View.GONE) {
        continue
      }
      if (enable) {
        btn.isEnabled = true
        btn.setTextColor(ResUtil.getColor(R.color.text_blue_color))
        btn.background = ResUtil.getDrawable(R.drawable.ripple_white_selector)
      } else {
        btn.isEnabled = false
        btn.setTextColor(ResUtil.getColor(R.color.text_gray_color))
        btn.setBackgroundColor(ResUtil.getColor(R.color.transparent))
      }
    }
  }

  override fun onClick(v: View?) {
    when (v!!.id) {
      R.id.enter -> {
        btnClickListener?.onEnter(v as Button)
      }
      R.id.cancel -> {
        btnClickListener?.onCancel(v as Button)
      }
      R.id.cover -> {
        btnClickListener?.onCover(v as Button)
      }
    }
    dismiss()
  }

}