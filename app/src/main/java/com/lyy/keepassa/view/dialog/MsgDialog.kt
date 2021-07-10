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
import androidx.annotation.ColorInt
import androidx.lifecycle.lifecycleScope
import com.arialyy.frame.util.ResUtil
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseDialog
import com.lyy.keepassa.databinding.DialogMsgBinding
import com.lyy.keepassa.event.MsgDialogEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus

class MsgDialog : BaseDialog<DialogMsgBinding>(), View.OnClickListener {

  private var enterBtName: String? = null
  private var cancelBtName: String? = null
  private var coverBtName: String? = null

  @ColorInt private var enterBtTextColor: Int? = null
  @ColorInt private var cancelBtTextColor: Int? = null
  @ColorInt private var coverBtTextColor: Int? = null

  // 以下是构造参数
  var msgTitle: CharSequence = ""
  var msgContent: CharSequence = ""
  var showCancelBt: Boolean = true // 显示取消按钮
  var requestCode: Int = 0 // dialog请求码
  var showCoverBt: Boolean = false //显示覆盖按钮
  var showCountDownTimer: Pair<Boolean, Int> = Pair(false, 5) // 显示确认按钮倒计时定时器，倒计时5s
  var interceptBackKey: Boolean = false // 是否拦截返回键
  var msgTitleEndIcon: Drawable? = null
  var msgTitleStartIcon: Drawable? = null

  companion object {
    const val TYPE_ENTER = 1
    const val TYPE_CANCEL = 2
    const val TYPE_COVER = 3

    fun generate(body: MsgDialog.() -> MsgDialog): MsgDialog {
      return with(MsgDialog()) { body() }
    }
  }

  private var listener: OnBtClickListener? = null
  private var useEventBusSendMsg = false

  public interface OnBtClickListener {
    /**
     * @param type [TYPE_ENTER],[TYPE_CANCEL],[TYPE_COVER]
     */
    fun onBtClick(
      type: Int,
      view: View
    )
  }

  /**
   * 设置确认按钮文字
   */
  fun setEnterBtText(charSequence: CharSequence): MsgDialog {
    enterBtName = charSequence.toString()
    return this
  }

  /**
   * 设置覆盖钮的问题
   */
  fun setCoverBtText(charSequence: CharSequence): MsgDialog {
    coverBtName = charSequence.toString()
    return this
  }

  /**
   * 设置取消钮的问题
   */
  fun setCancelBtText(charSequence: CharSequence): MsgDialog {
    cancelBtName = charSequence.toString()
    return this
  }

  /**
   * 设置确认按钮文字颜色
   */
  fun setEnterBtTextColor(@ColorInt color: Int): MsgDialog {
    enterBtTextColor = color
    return this
  }

  /**
   * 设置取消按钮文字颜色
   */
  fun setCancelBtTextColor(@ColorInt color: Int): MsgDialog {
    cancelBtTextColor = color
    return this
  }

  /**
   * 设置覆盖按钮文字颜色
   */
  fun setCoverBtTextColor(@ColorInt color: Int): MsgDialog {
    coverBtTextColor = color
    return this
  }

  /**
   * 使用eventbus 替代回调发送事件
   */
  fun useEventBusSendMsg(): MsgDialog {
    this.useEventBusSendMsg = true
    return this
  }

  override fun setLayoutId(): Int {
    return R.layout.dialog_msg
  }

  override fun initData() {
    super.initData()
    binding.cancel.visibility = if (showCancelBt) View.VISIBLE else View.GONE
    binding.cover.visibility = if (showCoverBt) View.VISIBLE else View.GONE
    binding.title.text = msgTitle
    binding.msg.text = msgContent
    binding.enter.setOnClickListener(this)
    binding.cancel.setOnClickListener(this)
    binding.cover.setOnClickListener(this)
    enterBtName?.let {
      binding.enter.text = enterBtName
    }
    cancelBtName?.let {
      binding.cancel.text = cancelBtName
    }
    coverBtName?.let {
      binding.cover.text = coverBtName
    }
    enterBtTextColor?.let {
      binding.enter.setTextColor(enterBtTextColor!!)
    }

    cancelBtTextColor?.let {
      binding.cancel.setTextColor(cancelBtTextColor!!)
    }

    coverBtTextColor?.let {
      binding.cover.setTextColor(coverBtTextColor!!)
    }

    msgTitleEndIcon?.let {
      binding.title.setEndIcon(msgTitleEndIcon!!)
    }

    msgTitleStartIcon?.let {
      binding.title.setLeftIcon(msgTitleStartIcon!!)
    }

    handleCountDown()
    if (interceptBackKey) {
      dialog!!.setOnKeyListener { _, keyCode, _ ->
        return@setOnKeyListener keyCode == KeyEvent.KEYCODE_BACK
      }
    }
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
    val count = binding.btns.childCount
    for (index in 0 until count) {
      val child = binding.btns.getChildAt(index)
      if (child.visibility == View.GONE) {
        continue
      }
      if (enable) {
        child.isEnabled = true
        (child as Button).setTextColor(ResUtil.getColor(R.color.text_blue_color))
        child.background = requireContext().getDrawable(R.drawable.ripple_white_selector)
      } else {
        child.isEnabled = false
        (child as Button).setTextColor(ResUtil.getColor(R.color.text_gray_color))
        child.setBackgroundColor(ResUtil.getColor(R.color.transparent))
      }
    }
  }

  /**
   * [useEventBusSendMsg]
   */
//  @Deprecated("使用 useEventBusSendMsg() 替代")
  fun setOnBtClickListener(listener: OnBtClickListener) {
    this.listener = listener
  }

  override fun onClick(v: View?) {
    when (v!!.id) {
      R.id.enter -> {
        listener?.onBtClick(TYPE_ENTER, v)
        if (useEventBusSendMsg) {
          EventBus.getDefault()
              .post(MsgDialogEvent(TYPE_ENTER, requestCode = requestCode))
        }
      }
      R.id.cancel -> {
        listener?.onBtClick(TYPE_CANCEL, v)
        if (useEventBusSendMsg) {
          EventBus.getDefault()
              .post(MsgDialogEvent(TYPE_CANCEL, requestCode = requestCode))
        }
      }
      R.id.cover -> {
        listener?.onBtClick(TYPE_COVER, v)
        if (useEventBusSendMsg) {
          EventBus.getDefault()
              .post(MsgDialogEvent(TYPE_COVER, requestCode = requestCode))
        }
      }
    }
    dismiss()
  }


  fun build(): MsgDialog {
    return this
  }

}