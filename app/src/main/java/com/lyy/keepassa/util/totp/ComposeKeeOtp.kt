/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.util.totp

import android.net.Uri
import com.blankj.utilcode.util.ConvertUtils
import com.blankj.utilcode.util.EncodeUtils
import com.keepassdroid.database.PwEntryV4
import com.lyy.keepassa.util.totp.TokenCalculator.HashAlgorithm
import com.lyy.keepassa.util.totp.TokenCalculator.HashAlgorithm.SHA256
import com.lyy.keepassa.util.totp.TokenCalculator.HashAlgorithm.SHA512

/**
 * 兼容KeeOtp2插件的totp获取
 * @Author laoyuyu
 * @Description
 * @Date 4:12 PM 2023/9/20
 **/
object ComposeKeeOtp : IOtpCompose {
  override fun getOtpPass(entry: PwEntryV4): Pair<Int, String?> {
    return getKeeOtp2Totp(entry)
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
}