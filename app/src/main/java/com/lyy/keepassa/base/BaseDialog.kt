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
import androidx.fragment.app.FragmentManager
import com.arialyy.frame.base.FrameDialog
import com.lyy.keepassa.util.LanguageUtil
import timber.log.Timber

/**
 * @Author laoyuyu
 * @Description
 * @Date 2021/1/13
 **/
abstract class BaseDialog<VB : ViewDataBinding> : FrameDialog<VB>() {

  private var onDismissListener: OnDialogDismissListener? = null

  fun setOnDismissListener(dismissListener: OnDialogDismissListener) {
    this.onDismissListener = dismissListener
  }

  override fun onAttach(context: Context) {
    super.onAttach(LanguageUtil.setLanguage(context, BaseApp.currentLang))
  }

  override fun show(manager: FragmentManager, tag: String?) {
    if (manager.isStateSaved) {
      Timber.d("ac 已经保存状态了，不再启动对话框")
      return
    }
    if (isAdded || manager.findFragmentByTag(tag) != null) {
      Timber.d("fragment 已经被add")
      return
    }
    super.show(manager, tag)
  }

  override fun dismiss() {
    if (!isVisible) {
      Timber.d("fragment 还没被加载")
      return
    }
    if (childFragmentManager.isStateSaved) {
      Timber.d("状态已经保存，不再dismiss")
      return
    }
    onDismissListener?.onDismiss()
    super.dismiss()
  }

  override fun onDestroy() {
    super.onDestroy()
    onDismissListener = null
  }
}