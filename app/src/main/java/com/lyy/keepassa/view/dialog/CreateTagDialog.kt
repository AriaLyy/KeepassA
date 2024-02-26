/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.dialog

import android.view.View
import android.widget.ArrayAdapter
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.arialyy.frame.util.ResUtil
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseDialog
import com.lyy.keepassa.databinding.DialogCreateTagBinding
import com.lyy.keepassa.util.KdbUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @Author laoyuyu
 * @Description
 * @Date 7:53 PM 2023/10/26
 **/
@Route(path = "/dialog/createTag")
class CreateTagDialog : BaseDialog<DialogCreateTagBinding>() {

  companion object {
    val createTagFlow = MutableSharedFlow<String?>(0)
    val tagCacheSet = hashSetOf<String>()
  }

  override fun setLayoutId(): Int {
    return R.layout.dialog_create_tag
  }

  override fun initData() {
    super.initData()
    ARouter.getInstance().inject(this)
    binding.msgTitle = ResUtil.getString(R.string.create_tag)

    binding.clicker = object : DialogBtnClicker {
      override fun onEnter(v: View) {
        dismiss()
        lifecycleScope.launch {
          val tag = binding.edTag.text.toString().trim()
          createTagFlow.emit(tag)
          tagCacheSet.add(tag)
        }
      }

      override fun onCancel(v: View) {
        dismiss()
        lifecycleScope.launch {
          createTagFlow.emit(null)
        }
      }
    }
    binding.edTag.doAfterTextChanged {
      binding.enableEnterBt = (it?.length ?: 0) > 0
    }
    handleTagList()
  }

  private fun handleTagList() {
    binding.edTag.threshold = 1 // 设置输入几个字符后开始出现提示 默认是2
    binding.edTag.setOnFocusChangeListener { _, hasFocus ->
      if (hasFocus) {
        binding.edTag.showDropDown()
      }
    }

    lifecycleScope.launch {
      if (tagCacheSet.isEmpty()) {
        withContext(Dispatchers.IO) {
          tagCacheSet.addAll(KdbUtil.getAllTags())
        }
      }

      binding.edTag.setAdapter(
        ArrayAdapter(
          requireContext(),
          R.layout.android_simple_dropdown_item_1line,
          tagCacheSet.toArray()
        )
      )

    }
  }
}