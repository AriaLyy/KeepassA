/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.util.totp

import android.text.TextUtils
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.security.ProtectedString
import com.lyy.keepassa.R.string
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.entity.GoogleOtpBean
import com.lyy.keepassa.entity.KeepassXcBean
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.getKeepassXcBean
import com.lyy.keepassa.util.otpIsKeepassXcSteam
import com.lyy.keepassa.util.totp.TokenCalculator.HashAlgorithm
import timber.log.Timber
import java.util.Locale

object ComposeKeepassxc : IOtpCompose {

  const val KEY_SEED = "otp"
  const val KEY_ENCODER = "encoder"
  const val KEY_STEAM = "steam"
  const val KEY_SECRET = "secret"
  const val KEY_COUNTER = "counter"
  const val KEY_ISSUER = "issuer"
  const val KEY_PERIOD = "period"
  const val KEY_DIGITS = "digits"
  const val KEY_ALGORITHM = "algorithm"

  override fun getOtpPass(entry: PwEntryV4): Pair<Int, String?> {
    return getPass(entry)
  }

  private fun getPass(entry: PwEntryV4): Pair<Int, String?> {
    val bean = entry.getKeepassXcBean()

    if (TextUtils.isEmpty(bean.secret)) {
      return Pair(0, null)
    }

    try {
      val b = Base32String.decode(bean.secret)
      if (entry.otpIsKeepassXcSteam()) {
        val pass = TokenCalculator.TOTP_Steam(
          b, TokenCalculator.TOTP_DEFAULT_PERIOD, TokenCalculator.STEAM_DEFAULT_DIGITS,
          TokenCalculator.DEFAULT_ALGORITHM
        )
        return Pair(bean.period, pass)
      }
      val arithmetic = when (bean.algorithm) {
        HashAlgorithm.SHA256 -> "SHA256"
        HashAlgorithm.SHA512 -> "SHA512"
        else -> "SHA1"
      }
      val pass = when (bean.host) {
        "totp", "TOTP" -> {
          TokenCalculator.TOTP_RFC6238(
            b,
            bean.period,
            bean.digits,
            HashAlgorithm.valueOf(arithmetic.toUpperCase(Locale.ROOT))
          )
        }

        "hotp", "HOTP" -> {
          TokenCalculator.HOTP(
            b,
            bean.counter?.toLong() ?: TokenCalculator.HOTP_INITIAL_COUNTER.toLong(),
            bean.digits,
            HashAlgorithm.valueOf(arithmetic.toUpperCase(Locale.ROOT))
          )
        }

        else -> {
          Timber.e("不识别的类型：${bean.host}")
          null
        }
      }
      return Pair(bean.period, pass)
    } catch (e: Exception) {
      HitUtil.toaskShort(BaseApp.APP.getString(string.totp_key_error))
      Timber.e(e)
    }
    return Pair(bean.period, null)
  }

}