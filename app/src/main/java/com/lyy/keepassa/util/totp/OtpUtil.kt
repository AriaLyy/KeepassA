/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.util.totp

import android.annotation.SuppressLint
import com.keepassdroid.database.PwEntryV4
import com.lyy.keepassa.util.otpIsKeeOtp2
import com.lyy.keepassa.util.otpIsKeeTrayTotp
import com.lyy.keepassa.util.otpIsKeepOtp
import com.lyy.keepassa.util.otpKeepass
import com.lyy.keepassa.util.otpKeepassXC

object OtpUtil {

  /**
   * 获取totp密码
   * @return first period， second 密码
   */
  @SuppressLint("DefaultLocale") fun getOtpPass(entry: PwEntryV4): Pair<Int, String?> {
    val otpCompose = when {

      entry.otpIsKeeTrayTotp() -> {
        ComposeKeeTrayTotp
      }

      entry.otpIsKeepOtp() -> {
        ComposeKeeOtp
      }

      entry.otpKeepassXC() -> {
        ComposeKeepassxc
      }

      entry.otpKeepass() -> {
        ComposeKeepass
      }

      entry.otpIsKeeOtp2() -> {
        ComposeKeeOtp2
      }

      else -> null
    }
    return otpCompose?.getOtpPass(entry) ?: Pair(-1, null)
  }
}