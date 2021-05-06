/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.base

import android.content.Context
import android.os.Bundle
import androidx.databinding.ViewDataBinding
import com.arialyy.frame.core.AbsBottomSheetDialogFragment
import com.lyy.keepassa.util.LanguageUtil

abstract class BaseBottomSheetDialogFragment<VB : ViewDataBinding> : AbsBottomSheetDialogFragment<VB>() {

  override fun onAttach(context: Context) {
    super.onAttach(LanguageUtil.setLanguage(context, BaseApp.currentLang))
  }

  override fun init(savedInstanceState: Bundle?) {
  }
}