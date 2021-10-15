/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.base

import android.content.Context
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.lifecycleScope
import com.arialyy.frame.base.FrameDialog
import com.lyy.keepassa.util.LanguageUtil
import kotlinx.coroutines.launch

/**
 * @Author laoyuyu
 * @Description
 * @Date 2021/1/13
 **/
abstract class BaseDialog<VB : ViewDataBinding>:FrameDialog<VB>() {

  override fun onAttach(context: Context) {
    super.onAttach(LanguageUtil.setLanguage(context, BaseApp.currentLang))
  }

  override fun show() {
    lifecycleScope.launch {
      super.show()
    }
  }

  override fun dismiss() {
    lifecycleScope.launch {
      super.dismiss()
    }
  }
}