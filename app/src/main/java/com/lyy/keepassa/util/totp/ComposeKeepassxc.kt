/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.util.totp

import android.net.Uri
import android.text.TextUtils
import com.keepassdroid.database.PwEntryV4
import com.lyy.keepassa.R.string
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.totp.TokenCalculator.HashAlgorithm
import timber.log.Timber
import java.util.Locale

object ComposeKeepassxc:IOtpCompose {
  override fun getOtpPass(entry: PwEntryV4): Pair<Int, String?> {
    return getPass(entry)
  }

  private fun getPass(entry: PwEntryV4): Pair<Int, String?> {
    val seed = entry.strings["otp"]?.toString()
    var isSteam = false
    val uri = Uri.parse(seed)
    val secret = uri.getQueryParameter("secret")
    var counter = uri.getQueryParameter("counter")
    var issuer = uri.getQueryParameter("issuer")
    var period = uri.getQueryParameter("period")
    var digits = uri.getQueryParameter("digits")
    var algorithm = uri.getQueryParameter("algorithm")
    val encoder = uri.getQueryParameter("encoder")

    if (encoder != null && encoder.equals("steam", ignoreCase = true)) {
      isSteam = true
    }

    if (period == null) {
      period = TokenCalculator.TOTP_DEFAULT_PERIOD.toString()
    }

    if (counter == null) {
      counter = TokenCalculator.HOTP_INITIAL_COUNTER.toString()
    }

    if (digits == null) {
      digits = TokenCalculator.TOTP_DEFAULT_DIGITS.toString()
    }

    if (algorithm == null) {
      algorithm = TokenCalculator.DEFAULT_ALGORITHM.toString()
    }

    if (TextUtils.isEmpty(secret)) {
      return Pair(period.toInt(), null)
    }

    try {
      val b = Base32String.decode(secret)
      if (isSteam) {
        val pass = TokenCalculator.TOTP_Steam(
          b, TokenCalculator.TOTP_DEFAULT_PERIOD, TokenCalculator.STEAM_DEFAULT_DIGITS,
          TokenCalculator.DEFAULT_ALGORITHM
        )
        return Pair(period.toInt(), pass)
      }
      val pass = when (uri.host) {
        "totp", "TOTP" -> {
          TokenCalculator.TOTP_RFC6238(
            b,
            period.toInt(),
            digits.toInt(),
            HashAlgorithm.valueOf(algorithm.toUpperCase(Locale.ROOT))
          )
        }
        "hotp", "HOTP" -> {
          TokenCalculator.HOTP(
            b,
            counter.toLong(),
            digits.toInt(),
            HashAlgorithm.valueOf(algorithm.toUpperCase(Locale.ROOT))
          )
        }
        else -> {
          Timber.e("不识别的类型：${uri.host}")
          null
        }
      }
      return Pair(period.toInt(), pass)
    } catch (e: Exception) {
      HitUtil.toaskShort(BaseApp.APP.getString(string.totp_key_error))
      Timber.e(e)
    }
    return Pair(period.toInt(), null)
  }
}