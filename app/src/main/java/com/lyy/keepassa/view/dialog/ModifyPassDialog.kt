/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.dialog

import android.text.InputType
import android.view.View
import com.arialyy.frame.base.BaseDialog
import com.lyy.keepassa.R
import com.lyy.keepassa.databinding.DialogModifyPassBinding
import com.lyy.keepassa.event.ModifyPassEvent
import com.lyy.keepassa.util.HitUtil
import org.greenrobot.eventbus.EventBus

/**
 * 修改密码对话框
 */
class ModifyPassDialog : BaseDialog<DialogModifyPassBinding>(), View.OnClickListener {
  private var isShowPass = false

  override fun setLayoutId(): Int {
    return R.layout.dialog_modify_pass
  }

  override fun initData() {
    super.initData()
    handlePassLayout()
    binding.enter.setOnClickListener(this)
    binding.cancel.setOnClickListener(this)
  }

  /**
   * 处理密码
   */
  private fun handlePassLayout() {
    binding.passwordLayout.endIconDrawable = resources.getDrawable(R.drawable.ic_view_off)

    binding.passwordLayout.setEndIconOnClickListener {
      isShowPass = !isShowPass
      if (isShowPass) {
        binding.passwordLayout.endIconDrawable = resources.getDrawable(R.drawable.ic_view)
        binding.enterPasswordLayout.visibility = View.GONE
        binding.password.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
      } else {
        binding.passwordLayout.endIconDrawable =
          resources.getDrawable(R.drawable.ic_view_off)
        binding.enterPasswordLayout.visibility = View.VISIBLE
        binding.password.inputType =
          InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_CLASS_TEXT
      }
      // 将光标移动到最后
      binding.password.setSelection(binding.password.text!!.length)
      binding.password.requestFocus()
    }
  }

  override fun onClick(v: View?) {
    val pass = binding.password.text.toString()
        .trim()
    val enterPass = binding.enterPassword.text.toString()
        .trim()

    if (v!!.id == R.id.enter) {
      if (pass.length < 6) {
        HitUtil.toaskShort(getString(R.string.error_db_pass_too_short))
        return
      }
      if (pass.isEmpty()) {
        HitUtil.toaskShort(getString(R.string.error_pass_null))
        return
      }

      if (pass.isNotEmpty() && pass != enterPass) {
        HitUtil.toaskShort(getString(R.string.error_pass_unfit))
        return
      }
      EventBus.getDefault().post(ModifyPassEvent(pass))
    }
    dismiss()
  }
}