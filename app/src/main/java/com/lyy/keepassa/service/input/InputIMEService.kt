/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.service.input

import android.inputmethodservice.InputMethodService
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import com.lyy.keepassa.R
import com.lyy.keepassa.util.KLog

/**
 * 输入法
 * https://developer.android.com/guide/topics/text/creating-input-method?hl=zh-cn
 */
class InputIMEService : InputMethodService(), View.OnClickListener {
  private val TAG = "InputIMEService"

  /**
   * 当 IME 首次显示时，系统会调用 onCreateInputView() 回调。在此方法的实现中，您可以创建要在 IME 窗口中显示的布局，并将布局返回系统。
   */
  override fun onCreateInputView(): View {

    val layout = LayoutInflater.from(this)
        .inflate(R.layout.layout_kpa_ime, null) as ViewGroup
    for (i in 0..layout.childCount) {
      val child = layout.getChildAt(i)
      if (child != null && child.isClickable) {
        child.setOnClickListener(this)
      }
    }
    return layout
  }

  /**
   * 输入法被唤起，开始输入
   */
  override fun onStartInputView(
    info: EditorInfo?,
    restarting: Boolean
  ) {
    super.onStartInputView(info, restarting)
    KLog.d(TAG, "pkgName = ${info?.packageName}")
  }

  override fun onClick(v: View) {
    when (v.id) {
      R.id.btLock -> {

      }
      R.id.btAccount -> {

      }
      R.id.btPass -> {

      }
      R.id.btClose -> { // 关键软键盘
        requestHideSelf(InputMethodManager.HIDE_NOT_ALWAYS)
      }
      R.id.btChangeIme -> { // 选择输入法
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showInputMethodPicker()
      }
      R.id.btTotp -> {

      }
      R.id.btOtherInfo -> {

      }
      R.id.btBackspace -> { // 回退键

      }
      R.id.btEnter -> { // 回车键

      }
    }
  }

}