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
import com.blankj.utilcode.util.ConvertUtils
import com.blankj.utilcode.util.EncodeUtils
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.security.ProtectedString
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.util.totp.Base32String
import com.lyy.keepassa.util.totp.TokenCalculator
import com.lyy.keepassa.util.totp.TokenCalculator.HashAlgorithm
import com.lyy.keepassa.util.totp.TokenCalculator.HashAlgorithm.SHA256
import com.lyy.keepassa.util.totp.TokenCalculator.HashAlgorithm.SHA512
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
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
      return getOtpPass(entry, otp.toString())
    }

    // 修复1.7之前的bug
    if (isSteamEntry(entry)) {
      fix1_7bug(entry)
    }

    val totpSetting = entry.strings["TOTP Settings"]
    if (totpSetting != null) {
      return getKeeTrayTotp(entry, totpSetting)
    }

    if (isKeeOtp(entry)) {
      return getKeeOtp2Totp(entry)
    }


    return Pair(-1, null)
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

  /**
   * 兼容KeeOtp2插件的totp获取
   */
  private fun getKeeOtp2Totp(entry: PwEntryV4): Pair<Int, String?> {
    // 默认的32位
    val def32 = entry.strings["TimeOtp-Secret-Base32"]
    if (def32.toString().isNotEmpty()) {
      return Pair(
        TokenCalculator.TOTP_DEFAULT_PERIOD,
        getTotpPass(
          def32.toString(),
          TokenCalculator.TOTP_DEFAULT_PERIOD,
          TokenCalculator.TOTP_DEFAULT_DIGITS,
          false
        )
      )
    }

    // 自定义的totp
    val secretTotp = entry.strings.keys.find { it.startsWith("TimeOtp-Secret") }
    if (secretTotp != null) {
      val lenStr = entry.strings["TimeOtp-Length"]
      val algorithmStr = entry.strings["TimeOtp-Algorithm"]
      val periodStr = entry.strings["TimeOtp-Period"]
      val len = lenStr?.toString()?.toInt() ?: TokenCalculator.TOTP_DEFAULT_PERIOD
      var algorithm = HashAlgorithm.SHA1
      if (algorithmStr != null) {
        algorithm = when (algorithmStr.toString()) {
          "HMAC-SHA-256" -> SHA256
          "HMAC-SHA-512" -> SHA512
          else -> HashAlgorithm.SHA1
        }
      }
      val period = periodStr?.toString()?.toInt() ?: TokenCalculator.TOTP_DEFAULT_PERIOD
      val seedByte = when {
        entry.strings["TimeOtp-Secret-Base64"] != null -> {
          EncodeUtils.base64Decode(secretTotp)
        }
        entry.strings["TimeOtp-Secret-Base32"] != null -> {
          Base32String.decode(secretTotp)
        }
        entry.strings["TimeOtp-Secret-Hex"] != null -> {
          // hex
          ConvertUtils.hexString2Bytes(secretTotp)
        }
        else -> {
          // utf-8
          secretTotp.toByteArray(Charsets.UTF_8)
        }
      }

      return Pair(
        period,
        TokenCalculator.TOTP_RFC6238(seedByte, period, len, algorithm)
      )
    }

    // hotp
    val secretHotp = entry.strings.keys.find { it.startsWith("HmacOtp-Secret") }
    if (secretHotp != null) {
      val algorithmStr = entry.strings["HmacOtp-Algorithm"]
      var algorithm = HashAlgorithm.SHA1
      if (algorithmStr != null) {
        algorithm = when (algorithmStr.toString()) {
          "HMAC-SHA-256" -> SHA256
          "HMAC-SHA-512" -> SHA512
          else -> HashAlgorithm.SHA1
        }
      }
      val counterStr = entry.strings["HmacOtp-Counter"]
      val counter = counterStr?.toString()?.toLong() ?: 0L
      val lenStr = entry.strings["HmacOtp-Length"]
      val len = lenStr?.toString()?.toInt() ?: TokenCalculator.TOTP_DEFAULT_PERIOD

      val seedByte = when {
        entry.strings["HmacOtp-Secret-Base64"] != null -> {
          EncodeUtils.base64Decode(secretHotp)
        }
        entry.strings["HmacOtp-Secret-Base32"] != null -> {
          Base32String.decode(secretHotp)
        }
        entry.strings["HmacOtp-Secret-Hex"] != null -> {
          // hex
          ConvertUtils.hexString2Bytes(secretHotp)
        }
        else -> {
          // utf-8
          secretHotp.toByteArray(Charsets.UTF_8)
        }
      }

      return Pair(
        counter.toInt(),
        TokenCalculator.HOTP(seedByte, counter, len, algorithm)
      )
    }

    //keeotp
    val keepOtp = entry.strings["otp"]
    if (keepOtp != null) {
      val uri = Uri.parse("otp://laoyuyu.me/?${keepOtp.toString()}")
      val key = uri.getQueryParameter("key")
      val type = uri.getQueryParameter("type")
      val len = uri.getQueryParameter("size") ?: TokenCalculator.TOTP_DEFAULT_DIGITS.toString()
      val hashMode = uri.getQueryParameter("otpHashMode")
      val encoding = uri.getQueryParameter("encoding")
      val counter = uri.getQueryParameter("counter") ?: "0"
      val period = uri.getQueryParameter("step") ?: TokenCalculator.TOTP_DEFAULT_PERIOD.toString()

      val algorithm = when (hashMode) {
        "Sha256" -> SHA256
        "Sha512" -> SHA512
        else -> HashAlgorithm.SHA1
      }
      val seedByte = when (encoding) {
        "Base64" -> {
          EncodeUtils.base64Decode(key)
        }
        "UTF8" -> {
          key!!.toByteArray(Charsets.UTF_8)
        }
        "Hex" -> {
          // hex
          ConvertUtils.hexString2Bytes(key)
        }
        else -> {
          // base32
          Base32String.decode(key)
        }
      }

      val token = when (type) {
        "Hotp" -> {
          TokenCalculator.HOTP(seedByte, counter.toLong(), len.toInt(), algorithm)
        }
        "Steam" -> {
          TokenCalculator.TOTP_Steam(seedByte, period.toInt(), len.toInt(), algorithm)
        }
        else -> {
          // totp
          TokenCalculator.TOTP_RFC6238(seedByte, period.toInt(), len.toInt(), algorithm)
        }
      }
      return Pair(if (type == "Hotp") counter.toInt() else period.toInt(), token)
    }
    return Pair(TokenCalculator.TOTP_DEFAULT_PERIOD, null)
  }

  /**
   * 兼容KeeTrayTOTP插件的totp获取
   */
  private fun getKeeTrayTotp(entry: PwEntryV4, totpSetting: ProtectedString): Pair<Int, String?> {
    val isSteam = isSteam(entry)
    var period = TokenCalculator.TOTP_DEFAULT_PERIOD
    var digits = TokenCalculator.TOTP_DEFAULT_DIGITS
    val s = totpSetting.toString()
      .split(";")
    if (!s.isNullOrEmpty() && s.size == 2) {
      period = s[0].toInt()
      digits = if (isSteam) TokenCalculator.STEAM_DEFAULT_DIGITS else s[1].toInt()
    }
    return Pair(
      period,
      getTotpPass(entry.strings["TOTP Seed"].toString(), period, digits, isSteam)
    )
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

  private fun getOtpPass(entry: PwEntryV4, seed: String?): Pair<Int, String?> {
    if (seed?.startsWith("otpauth") == false && seed.startsWith("key")) {
      return getKeeOtp2Totp(entry)
    }

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
      HitUtil.toaskShort(BaseApp.APP.getString(R.string.totp_key_error))
      e.printStackTrace()
    }
    return Pair(period.toInt(), null)
  }
}