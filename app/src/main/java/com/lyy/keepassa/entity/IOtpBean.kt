/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.entity

import android.os.Parcelable
import com.keepassdroid.database.security.ProtectedString
import com.lyy.keepassa.util.totp.ComposeKeeTrayTotp
import com.lyy.keepassa.util.totp.ComposeKeepass
import com.lyy.keepassa.util.totp.ComposeKeepassxc
import com.lyy.keepassa.util.totp.SecretHexType
import com.lyy.keepassa.util.totp.TokenCalculator
import com.lyy.keepassa.util.totp.TokenCalculator.HashAlgorithm
import kotlinx.parcelize.Parcelize

interface IOtpBean

@Parcelize
data class OtpBeans(
  val trayTotp: TrayTotpBean? = null,
  val keeOtp2: KeeOtp2Bean? = null,
  val keepassxc: KeepassXcBean? = null,
  val keeOtp: KeepOtpBean? = null,
  val googleOtpBean: GoogleOtpBean? = null
) : Parcelable

/**
 * TrayTotp 的实体
 */
@Parcelize
data class TrayTotpBean(
  var secret: String,
  var period: Int,
  val isSteam: Boolean
) : Parcelable, IOtpBean

/**
 * KeepOtp实体
 */
@Parcelize
data class KeepOtpBean(
  val key: ProtectedString
) : Parcelable, IOtpBean

/**
 * Keepass 实体
 * [ComposeKeepass#toKeepassOtpMap]
 */
@Parcelize
data class KeepassBean(
  val otpBean: TimeOtp2Bean? = null,
  val hmac: HmacOtpBean? = null
) : Parcelable, IOtpBean {
}

/**
 *  KeeOtp2 的实体
 */
@Parcelize
data class KeeOtp2Bean(
  val otpBean: TimeOtp2Bean? = null,
  val hmac: HmacOtpBean? = null
) : Parcelable, IOtpBean

/**
 * keepassxc的实体
 */
@Parcelize
data class KeepassXcBean(
  val host: String = "totp",
  val title: String,
  val userName: String,
  val isSteam: Boolean,
  var encoder: String = "",
  var secret: String,
  val issuer: String,
  var period: Int,
  var digits: Int,
  var algorithm: HashAlgorithm,
  val counter: String? = ""
) : Parcelable, IOtpBean

@Parcelize
data class GoogleOtpBean(
  val secret: String
) : Parcelable, IOtpBean

fun GoogleOtpBean.toOtpStringMap(): Map<String, ProtectedString> {
  return hashMapOf<String, ProtectedString>().apply {
    put(ComposeKeepassxc.KEY_SEED, ProtectedString(true, secret))
  }
}

/**
 * KeeOtp2 插件的otpHmac
 */
@Parcelize
data class HmacOtpBean(
  val secretType: SecretHexType,
  /**
   * HmacOtp-Secret-Hex
   * HmacOtp-Secret-Base32
   * HmacOtp-Secret-Base64
   */
  val secret: String,
  /**
   * HMAC-SHA-1
   * HMAC-SHA-256
   * HMAC-SHA-512
   */
  val algorithm: HashAlgorithm,
  val counter: Int,
  val len: Int = TokenCalculator.TOTP_DEFAULT_DIGITS
) : Parcelable

/**
 * KeeOtp2 插件的otpbean
 */
@Parcelize
data class TimeOtp2Bean(
  val secretType: SecretHexType,
  /**
   * TimeOtp-Secret-Hex
   * TimeOtp-Secret-Base32
   * TimeOtp-Secret-Base64
   */
  var secret: String,
  /**
   * [6-8]
   */
  var digits: Int,
  /**
   * HMAC-SHA-1
   * HMAC-SHA-256
   * HMAC-SHA-512
   */
  var algorithm: HashAlgorithm,
  /**
   * 更新时间，默认30s
   */
  var period: Int,

  ) : Parcelable

fun KeepassBean.toOtpStringMap(): Map<String, ProtectedString> {
  val map = linkedMapOf<String, ProtectedString>()
  //totp
  otpBean?.let {
    map[ComposeKeepass.getSecretType(it.secretType)] = ProtectedString(true, it.secret)
    map[ComposeKeepass.TimeOtp_Length] = ProtectedString(false, it.digits.toString())
    map[ComposeKeepass.TimeOtp_Period] = ProtectedString(false, it.period.toString())
    map[ComposeKeepass.TimeOtp_Algorithm] = ProtectedString(
      false, when (it.algorithm) {
        HashAlgorithm.SHA256 -> ComposeKeepass.HMAC_SHA_256
        HashAlgorithm.SHA512 -> ComposeKeepass.HMAC_SHA_512
        else -> ComposeKeepass.HMAC_SHA_1
      }
    )
  }
  // hotp
  hmac?.let {
    map[ComposeKeepass.getSecretType(it.secretType)] = ProtectedString(true, it.secret)
    map[ComposeKeepass.HmacOtp_Counter] = ProtectedString(false, it.counter.toString())
  }

  return map
}

fun KeepassXcBean.toOtpStringMap(): Map<String, ProtectedString> {
  return hashMapOf<String, ProtectedString>().apply {
    val arithmetic = when (algorithm) {
      HashAlgorithm.SHA256 -> "SHA256"
      HashAlgorithm.SHA512 -> "SHA512"
      else -> "SHA1"
    }
    var seedStr =
      "otpauth://totp/${title}:${userName}?secret=${secret}&period=${period}&digits=${digits}&issuer=${userName}&algorithm=$arithmetic"

    if (isSteam) {
      seedStr += "&encoder=steam"
    }
    put(ComposeKeepassxc.KEY_SEED, ProtectedString(true, seedStr))
  }
}


fun TrayTotpBean.toOtpStringMap(): Map<String, ProtectedString> {
  return hashMapOf<String, ProtectedString>().apply {
    put(ComposeKeeTrayTotp.KEY_SEED, ProtectedString(true, secret))
    put(
      ComposeKeeTrayTotp.KEY_SETTING,
      ProtectedString(false, "${period};${if (isSteam) "S" else "6"}")
    )
  }
}