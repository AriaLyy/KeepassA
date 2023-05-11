/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.dialog

import com.alibaba.android.arouter.facade.annotation.Route
import com.arialyy.frame.util.ResUtil
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseDialog
import com.lyy.keepassa.base.KeyConstance
import com.lyy.keepassa.databinding.DialogTipBinding
import com.lyy.keepassa.util.CommonKVStorage
import com.lyy.keepassa.util.doClick

/**
 * @Author laoyuyu
 * @Description
 * @Date 4:05 PM 2023/5/10
 **/
@Route(path = "/dialog/tipsDialog")
class TipsDialog : BaseDialog<DialogTipBinding>() {
  override fun setLayoutId(): Int {
    return R.layout.dialog_tip
  }

  override fun initData() {
    super.initData()
    val vector = ResUtil.getSvgIcon(R.drawable.ic_lightbulb_on, R.color.colorPrimary)
    binding.layoutTitle.tvTitle.setLeftIcon(vector!!)
    binding.cancel.doClick {
      dismiss()
    }

    binding.cbShow.isChecked = CommonKVStorage.getBoolean(KeyConstance.KEY_DONT_SHOW_TIP, false)

    binding.cbShow.setOnCheckedChangeListener { _, isChecked ->
      CommonKVStorage.put(KeyConstance.KEY_DONT_SHOW_TIP, isChecked)
    }
  }

  private fun bindingContent() {
  }
}