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
import kotlinx.parcelize.Parcelize

@Parcelize
data class OtpBeans(
  val trayTotp: TrayTotpBean? = null,
  val keeOtp2: KeeOtp2Bean? = null,
  val keepassxc: KeepassXcBean? = null
) : Parcelable

/**
 * TrayTotp 的实体
 */
@Parcelize
data class TrayTotpBean(
  /**
   * TOTP Seed
   */
  val seed: ProtectedString,
  /**
   * TOTP Settings
   */
  val settings: ProtectedString
) : Parcelable

/**
 *  KeeOtp2 的实体
 */
@Parcelize
data class KeeOtp2Bean(
  val otpBean: TimeOtpBean? = null,
  val hmac: HmacOtpBean? = null
) : Parcelable

/**
 * keepassxc的实体
 */
@Parcelize
data class KeepassXcBean(
  val otp: ProtectedString
) : Parcelable

/**
 * KeeOtp2 插件的otpHmac
 */
@Parcelize
data class HmacOtpBean(
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
  val algorithm: String,
  val counter: String,
) : Parcelable

/**
 * KeeOtp2 插件的otpbean
 */
@Parcelize
data class TimeOtpBean(
  /**
   * TimeOtp-Secret-Hex
   * TimeOtp-Secret-Base32
   * TimeOtp-Secret-Base64
   */
  val secret: ProtectedString,
  /**
   * [6-8]
   */
  val length: Int,
  /**
   * HMAC-SHA-1
   * HMAC-SHA-256
   * HMAC-SHA-512
   */
  val algorithm: ProtectedString,
  /**
   * 更新时间，默认30s
   */
  val period: Int,

  ) : Parcelable