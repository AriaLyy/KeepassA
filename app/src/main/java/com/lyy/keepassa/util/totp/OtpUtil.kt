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

object OtpUtil {

  /**
   * 获取totp密码
   * @return first period， second 密码
   */
  @SuppressLint("DefaultLocale") fun getOtpPass(entry: PwEntryV4): Pair<Int, String?> {
    val seed = entry.strings["otp"]?.toString()
    val otpCompose = when {
      seed != null -> {
        if (!seed.startsWith("otpauth") && seed.startsWith("key")) {
          ComposeKeeOtp
        } else {
          ComposeKeepassxc
        }
      }

      entry.strings["TOTP Settings"] != null -> {
        ComposeKeeTrayTotp
      }

      isKeeOtp(entry) -> {
        ComposeKeeOtp
      }

      else -> null
    }
    return otpCompose?.getOtpPass(entry) ?: Pair(-1, null)
  }

  /**
   * 判断是否是KeeOtp2插件
   */
  private fun isKeeOtp(entry: PwEntryV4): Boolean {
    for (str in entry.strings) {
      val key = str.toString()
      if (key.startsWith("TimeOtp") || key.startsWith("HmacOtp")) {
        return true
      }
    }
    return false
  }
}