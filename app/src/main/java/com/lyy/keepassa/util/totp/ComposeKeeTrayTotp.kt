/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.util.totp

import com.arialyy.frame.router.Routerfit
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.security.ProtectedString
import com.lyy.keepassa.router.ServiceRouter

/**
 * 兼容KeeTrayTOTP插件的totp获取
 */
object ComposeKeeTrayTotp : IOtpCompose {
  private val kdbService by lazy {
    Routerfit.create(ServiceRouter::class.java).getDbSaveService()
  }

  override fun getOtpPass(entry: PwEntryV4): Pair<Int, String?> {
    // 修复1.7之前的bug
    if (isSteamEntry(entry)) {
      fix1_7bug(entry)
    }

    return getKeeTrayTotp(entry)
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

  /**
   * 1.7之前的版本创建totp时，会将 TOTP Settings 字段设置为 30;S，而S表示的是Steam
   */
  private fun fix1_7bug(entry: PwEntryV4) {
    val totpSetting = entry.strings["TOTP Settings"]
    if (totpSetting != null) {
      val tempArray = totpSetting.toString()
        .split(";")
      if (tempArray.isNotEmpty() && tempArray.size == 2 && !tempArray[1].equals("S", true)) {
        entry.strings["TOTP Settings"] = ProtectedString(false, "${tempArray[0]};S")
        kdbService.saveDbByBackground()
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
   * 兼容KeeTrayTOTP插件的totp获取
   */
  private fun getKeeTrayTotp(entry: PwEntryV4): Pair<Int, String?> {

    val totpSetting = entry.strings["TOTP Settings"]
    val isSteam = isSteam(entry)
    var period = TokenCalculator.TOTP_DEFAULT_PERIOD
    var digits = TokenCalculator.TOTP_DEFAULT_DIGITS
    val s = totpSetting.toString()
      .split(";")
    if (s.isNotEmpty() && s.size == 2) {
      period = s[0].toInt()
      digits = if (isSteam) TokenCalculator.STEAM_DEFAULT_DIGITS else s[1].toInt()
    }
    return Pair(
      period,
      getTotpPass(entry.strings["TOTP Seed"].toString(), period, digits, isSteam)
    )
  }
}