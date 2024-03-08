/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.util.totp

import com.keepassdroid.database.PwEntryV4
import com.lyy.keepassa.R.string
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.util.HitUtil
import timber.log.Timber

/**
 * otp
 * @Author laoyuyu
 * @Description
 * @Date 4:02 PM 2023/9/20
 **/
interface IOtpCompose {

  fun getTotpPass(
    seed: String?,
    period: Int,
    digits: Int = TokenCalculator.TOTP_DEFAULT_DIGITS,
    isSteam: Boolean
  ): String? {
    // 适配keepass totp插件的密码
    try {
      val b = Base32String.decode(seed)
      return if (isSteam) {
        TokenCalculator.TOTP_Steam(
          b, TokenCalculator.TOTP_DEFAULT_PERIOD, TokenCalculator.STEAM_DEFAULT_DIGITS,
          TokenCalculator.DEFAULT_ALGORITHM
        )
      } else {
        TokenCalculator.TOTP_RFC6238(b, period, digits, TokenCalculator.DEFAULT_ALGORITHM)
      }
    } catch (e: Exception) {
      HitUtil.toaskShort(BaseApp.APP.getString(string.totp_key_error))
      Timber.e(e)
    }

    return null
  }

  /**
   * 获取totp密码
   * @return first period， second 密码
   */
  fun getOtpPass(entry: PwEntryV4): Pair<Int, String?>

  // fun toOtpStringMap(bean: OtpBean): Map<String, ProtectedString> {
  //   return hashMapOf()
  // }
}