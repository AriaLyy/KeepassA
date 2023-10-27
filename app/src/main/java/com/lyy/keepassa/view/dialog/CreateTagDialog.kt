/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.dialog

import android.view.View
import androidx.core.widget.doBeforeTextChanged
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Route
import com.arialyy.frame.util.ResUtil
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseDialog
import com.lyy.keepassa.databinding.DialogCreateTagBinding
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

/**
 * @Author laoyuyu
 * @Description
 * @Date 7:53 PM 2023/10/26
 **/
@Route(path = "/dialog/createTag")
class CreateTagDialog : BaseDialog<DialogCreateTagBinding>() {

  companion object {
    val createTagFlow = MutableSharedFlow<String?>(1)
  }

  override fun setLayoutId(): Int {
    return R.layout.dialog_create_tag
  }

  override fun initData() {
    super.initData()
    binding.msgTitle = ResUtil.getString(R.string.create_tag)
    binding.clicker = object : DialogBtnClicker {
      override fun onEnter(v: View) {
        lifecycleScope.launch {
          createTagFlow.emit(binding.edTag.text.toString().trim())
        }
      }

      override fun onCancel(v: View) {
        dismiss()
        lifecycleScope.launch {
          createTagFlow.emit(null)
        }
      }
    }
    binding.edTag.doBeforeTextChanged { text, _, _, _ ->
      binding.enableEnterBt = (text?.length ?: 0) > 0
    }
  }
}