/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.util

import android.annotation.SuppressLint
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.security.ProtectedString
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.util.totp.Base32String
import com.lyy.keepassa.util.totp.TokenCalculator
import com.lyy.keepassa.util.totp.TokenCalculator.HashAlgorithm
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.Locale

object OtpUtil {

  /**
   * 获取totp密码
   * @return first period， second 密码
   */
  @SuppressLint("DefaultLocale") fun getOtpPass(entry: PwEntryV4): Pair<Int, String?> {

    val otp = entry.strings["otp"]
    // 适配keepassc的 密码
    if (otp != null) {
      return getOtpPass(otp.toString())
    }

    // 修复1.7之前的bug
    if (isSteamEntry(entry)) {
      fix1_7bug(entry)
    }
    val isSteam = isSteam(entry)
    val totpSetting = entry.strings["TOTP Settings"]
    var period = TokenCalculator.TOTP_DEFAULT_PERIOD
    var digits = TokenCalculator.TOTP_DEFAULT_DIGITS
    if (totpSetting != null) {
      val s = totpSetting.toString()
          .split(";")
      if (!s.isNullOrEmpty() && s.size == 2) {
        period = s[0].toInt()
        digits = if (isSteam) TokenCalculator.STEAM_DEFAULT_DIGITS else s[1].toInt()
      }
    }

    return Pair(period, getTotpPass(entry.strings["TOTP Seed"].toString(), period, digits, isSteam))
  }

  /**
   * 1.7之前的版本创建totp时，会将 TOTP Settings 字段设置为 30;S，而S表示的是Steam
   */
  private fun fix1_7bug(entry: PwEntryV4) {
    val totpSetting = entry.strings["TOTP Settings"]
    if (totpSetting != null) {
      val tempArray = totpSetting.toString()
          .split(";")
      if (!tempArray.isNullOrEmpty() && tempArray.size == 2 && !tempArray[1].equals("S", true)) {
        entry.strings["TOTP Settings"] = ProtectedString(false, "${tempArray[0]};S")
        GlobalScope.launch {
          KdbUtil.saveDb(true)
        }
      }
    }
  }

  private fun isSteam(entry: PwEntryV4): Boolean {
    val otpSettings = entry.strings["TOTP Settings"]
    if (otpSettings != null) {
      val tempArray = otpSettings.toString()
          .split(";")
      return tempArray[1] == "S"
    }

    return false
  }

  /**
   * 判断是否是steam的条目，1.7之前的版本创建totp时，会将 TOTP Settings 字段设置为 30;S，而S表示的是Steam
   */
  private fun isSteamEntry(entry: PwEntryV4): Boolean {
    return entry.getUrl()
        .contains("steampowered", ignoreCase = true) ||
        entry.customData.any {
          it.value.equals(
              "androidapp://com.valvesoftware.android.steam.community",
              true
          )
        }
  }

  private fun getTotpPass(
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
      HitUtil.toaskShort(BaseApp.APP.getString(R.string.totp_key_error))
      e.printStackTrace()
    }

    return null
  }

  private fun getOtpPass(seed: String?): Pair<Int, String?> {
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
          Log.e("getTOTPPass", "不识别的类型：${uri.host}")
          null
        }
      }
      return Pair(period.toInt(), pass)
    } catch (e: Exception) {
      HitUtil.toaskShort(BaseApp.APP.getString(R.string.totp_key_error))
      e.printStackTrace()
    }
    return Pair(period.toInt(), null)
  }
}