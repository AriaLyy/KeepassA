/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.create

import android.view.View
import com.arialyy.frame.base.BaseDialog
import com.keepassdroid.database.security.ProtectedString
import com.lyy.keepassa.R
import com.lyy.keepassa.databinding.DialogAddAttrStrBinding
import com.lyy.keepassa.event.CreateAttrStrEvent
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.widget.expand.AttrStrItemView
import org.greenrobot.eventbus.EventBus

/**
 * 创建自定义字段的对话框
 */
class CreateCustomStrDialog(
  val isEdit: Boolean = false,
  val itemView: AttrStrItemView? = null
) : BaseDialog<DialogAddAttrStrBinding>(),
    View.OnClickListener {

  private var key: String? = null
  private var value: ProtectedString? = null

  override fun setLayoutId(): Int {
    return R.layout.dialog_add_attr_str
  }

  override fun initData() {
    super.initData()
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

  fun setData(
    key: String,
    str: ProtectedString
  ) {
    this.key = key
    this.value = str
  }

  override fun onClick(v: View?) {
    if (v!!.id == R.id.enter) {
      if (binding.strKey.text.toString().trim().isEmpty()) {
        HitUtil.toaskShort(getString(R.string.error_attr_str_null))
        return
      }
      if (isEdit) {
        EventBus.getDefault()
            .post(
                CreateAttrStrEvent(
                    binding.strKey.text.toString(),
                    ProtectedString(binding.cb.isChecked, binding.strValue.text.toString()),
                    isEdit,
                    itemView
                )
            )
      } else {
        EventBus.getDefault()
            .post(
                CreateAttrStrEvent(
                    binding.strKey.text.toString(),
                    ProtectedString(binding.cb.isChecked, binding.strValue.text.toString()),
                    isEdit
                )
            )
      }
    }
    dismiss()
  }

}