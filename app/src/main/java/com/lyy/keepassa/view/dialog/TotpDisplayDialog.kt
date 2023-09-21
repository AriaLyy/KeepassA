/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.dialog

import android.view.View
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.arialyy.frame.util.ResUtil
import com.keepassdroid.database.PwEntryV4
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.BaseDialog
import com.lyy.keepassa.databinding.DialogTotpDisplayBinding
import com.lyy.keepassa.util.ClipboardUtil
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.totp.OtpUtil
import java.util.UUID

/**
 * @Author laoyuyu
 * @Description
 * @Date 8:16 下午 2022/1/5
 **/
@Route(path = "/dialog/totpDisplay")
class TotpDisplayDialog : BaseDialog<DialogTotpDisplayBinding>() {

  @Autowired(name = "uuid")
  lateinit var uuid: String
  private var curToTp = ""

  override fun initData() {
    super.initData()
    ARouter.getInstance().inject(this)
    binding.dialog = this
    val entry = BaseApp.KDB.pm.entries[UUID.fromString(uuid)] as PwEntryV4
    curToTp = OtpUtil.getOtpPass(entry).second ?: "error"
    binding.tvTotp.text = curToTp
    binding.cvTime.startAnim {
      curToTp = OtpUtil.getOtpPass(entry).second ?: "error"
      binding.tvTotp.text = curToTp
    }
  }

  override fun setLayoutId(): Int {
    return R.layout.dialog_totp_display
  }

  fun onClick(v: View) {
    ClipboardUtil.get().copyDataToClip(curToTp)
    HitUtil.toaskShort(ResUtil.getString(R.string.hint_copy_totp))
    dismiss()
  }
}