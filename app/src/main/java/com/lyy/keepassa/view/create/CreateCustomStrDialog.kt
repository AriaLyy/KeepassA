/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.create

import android.view.View
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.keepassdroid.database.security.ProtectedString
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseDialog
import com.lyy.keepassa.databinding.DialogAddAttrStrBinding
import com.lyy.keepassa.entity.CommonState
import com.lyy.keepassa.event.AttrStrEvent
import com.lyy.keepassa.util.HitUtil
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

/**
 * 创建自定义字段的对话框
 */
@Route(path = "/dialog/customStrDialog")
class CreateCustomStrDialog : BaseDialog<DialogAddAttrStrBinding>(),
  View.OnClickListener {
  companion object {
    val CustomStrFlow = MutableSharedFlow<AttrStrEvent?>(0)
  }

  @Autowired(name = "key")
  @JvmField
  var key: String? = null

  @Autowired(name = "value")
  @JvmField
  var value: ProtectedString? = null

  @Autowired(name = "position")
  @JvmField
  var position: Int = 0

  override fun setLayoutId(): Int {
    return R.layout.dialog_add_attr_str
  }

  override fun initData() {
    super.initData()
    ARouter.getInstance().inject(this)
    binding.cancel.setOnClickListener(this)
    binding.enter.setOnClickListener(this)
    if (key != null) {
      binding.strKey.setText(key)
    }
    if (value != null) {
      binding.strValue.setText(value.toString())
      binding.cb.isChecked = value!!.isProtected
    }
  }

  override fun onClick(v: View?) {
    if (v!!.id == R.id.enter) {
      if (binding.strKey.text.toString().trim().isEmpty()) {
        HitUtil.toaskShort(getString(R.string.error_attr_str_null))
        return
      }
      lifecycleScope.launch {
        CustomStrFlow.emit(
          AttrStrEvent(
            if (key != null) CommonState.MODIFY else CommonState.CREATE,
            binding.strKey.text.toString(),
            ProtectedString(binding.cb.isChecked, binding.strValue.text.toString()),
            position
          )
        )
      }
    }
    dismiss()
  }
}