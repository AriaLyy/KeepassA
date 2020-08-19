/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.demo

import android.os.Bundle
import android.view.View
import com.arialyy.frame.core.AbsActivity
import com.lyy.demo.databinding.ActivityRegCodeBinding

class RegCodeActivity : AbsActivity<ActivityRegCodeBinding>() {
  val userName = "lyy222"

  override fun setLayoutId(): Int {
    return R.layout.activity_reg_code
  }

  override fun initData(savedInstanceState: Bundle?) {
    super.initData(savedInstanceState)
    binding.tvUser.append(userName)

  }

  fun onClick(v: View) {
    when (v.id) {
      R.id.btCreateCode -> {

      }
      R.id.btVerifyCode -> {

      }
    }

  }
}