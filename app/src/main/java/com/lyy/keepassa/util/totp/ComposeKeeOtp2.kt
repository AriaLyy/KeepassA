/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.util.totp

import com.blankj.utilcode.util.ConvertUtils
import com.blankj.utilcode.util.EncodeUtils
import com.keepassdroid.database.PwEntryV4
import com.lyy.keepassa.util.totp.ComposeKeepass.HMAC_SHA_256
import com.lyy.keepassa.util.totp.ComposeKeepass.HMAC_SHA_512
import com.lyy.keepassa.util.totp.ComposeKeepass.HmacOtp_Algorithm
import com.lyy.keepassa.util.totp.ComposeKeepass.HmacOtp_Counter
import com.lyy.keepassa.util.totp.ComposeKeepass.HmacOtp_Secret
import com.lyy.keepassa.util.totp.ComposeKeepass.HmacOtp_Secret_Base32
import com.lyy.keepassa.util.totp.ComposeKeepass.HmacOtp_Secret_Base64
import com.lyy.keepassa.util.totp.ComposeKeepass.HmacOtp_Secret_Hex
import com.lyy.keepassa.util.totp.ComposeKeepass.TimeOtp_Algorithm
import com.lyy.keepassa.util.totp.ComposeKeepass.TimeOtp_Length
import com.lyy.keepassa.util.totp.ComposeKeepass.TimeOtp_Period
import com.lyy.keepassa.util.totp.ComposeKeepass.TimeOtp_Secret
import com.lyy.keepassa.util.totp.ComposeKeepass.TimeOtp_Secret_Base32
import com.lyy.keepassa.util.totp.ComposeKeepass.TimeOtp_Secret_Base64
import com.lyy.keepassa.util.totp.ComposeKeepass.TimeOtp_Secret_Hex
import com.lyy.keepassa.util.totp.TokenCalculator.HashAlgorithm
import com.lyy.keepassa.util.totp.TokenCalculator.HashAlgorithm.SHA256
import com.lyy.keepassa.util.totp.TokenCalculator.HashAlgorithm.SHA512

/**
 * 兼容KeeOtp2插件的totp获取
 * @Author laoyuyu
 * @Description
 * @Date 4:12 PM 2023/9/20
 **/
@Deprecated("有异常，未实现")
object ComposeKeeOtp2 : IOtpCompose {

  override fun getOtpPass(entry: PwEntryV4): Pair<Int, String?> {
    return getKeeOtp2Totp(entry)
  }

  /**
   * 兼容KeeOtp2插件的totp获取
   */
  private fun getKeeOtp2Totp(entry: PwEntryV4): Pair<Int, String?> {
    // 默认的32位
    val def32 = entry.strings[TimeOtp_Secret_Base32]
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
    val secretTotp = entry.strings.keys.find { it.startsWith(TimeOtp_Secret) }
    if (secretTotp != null) {
      val lenStr = entry.strings[TimeOtp_Length]
      val algorithmStr = entry.strings[TimeOtp_Algorithm]
      val periodStr = entry.strings[TimeOtp_Period]
      val len = lenStr?.toString()?.toInt() ?: TokenCalculator.TOTP_DEFAULT_PERIOD
      var algorithm = HashAlgorithm.SHA1
      if (algorithmStr != null) {
        algorithm = when (algorithmStr.toString()) {
          HMAC_SHA_256 -> SHA256
          HMAC_SHA_512 -> SHA512
          else -> HashAlgorithm.SHA1
        }
      }
      val period = periodStr?.toString()?.toInt() ?: TokenCalculator.TOTP_DEFAULT_PERIOD
      val seedByte = when {
        entry.strings[TimeOtp_Secret_Base64] != null -> {
          EncodeUtils.base64Decode(secretTotp)
        }

        entry.strings[TimeOtp_Secret_Base32] != null -> {
          Base32String.decode(secretTotp)
        }

        entry.strings[TimeOtp_Secret_Hex] != null -> {
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



    return Pair(-1, null)
  }
}