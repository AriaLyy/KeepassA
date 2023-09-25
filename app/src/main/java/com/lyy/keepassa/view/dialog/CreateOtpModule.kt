/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.dialog

import com.keepassdroid.database.security.ProtectedString
import com.lyy.keepassa.base.BaseModule
import com.lyy.keepassa.entity.KeeOtp2Bean
import com.lyy.keepassa.entity.KeepassXcBean
import com.lyy.keepassa.entity.OtpBeans
import com.lyy.keepassa.entity.TimeOtpBean
import com.lyy.keepassa.entity.TotpType
import com.lyy.keepassa.entity.TotpType.STEAM
import com.lyy.keepassa.entity.TrayTotpBean
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * @Author laoyuyu
 * @Description
 * @Date 3:38 下午 2022/1/19
 **/
class CreateOtpModule : BaseModule() {
  // lateinit var entry: PwEntryV4

  val otpFlow = MutableStateFlow<OtpBeans?>(null)

  fun createOtpBeans(
    totpType: TotpType,
    entryTitle: String,
    entryUserName: String,
    secret: String,
    time: Int,
    len: Int,
    arithmetic: String
  ): OtpBeans {
    return OtpBeans(
      trayTotp = createTrayTotpBean(secret, time, totpType),
      keepassxc = createKeepassXcBean(
        entryTitle,
        entryUserName,
        secret,
        time,
        len,
        arithmetic
      ),
      keeOtp2 = createKeepOtp2(secret, time, len, arithmetic)
    )
  }

  private fun createKeepOtp2(
    secret: String,
    time: Int,
    len: Int,
    arithmetic: String
  ): KeeOtp2Bean {
    return KeeOtp2Bean(
      otpBean = TimeOtpBean(
        secret = ProtectedString(true, secret),
        length = len,
        algorithm = ProtectedString(true, arithmetic),
        period = time
      )
    )
  }

  private fun createTrayTotpBean(secret: String, time: Int, totpType: TotpType): TrayTotpBean {
    return TrayTotpBean(
      ProtectedString(true, secret).apply {
        isOtpPass = true
      },
      ProtectedString(false, "$time;${if (totpType == STEAM) "S" else "6"}")
    )
  }

  private fun createKeepassXcBean(
    entryTitle: String,
    entryUserName: String,
    secret: String,
    time: Int,
    len: Int,
    arithmetic: String
  ): KeepassXcBean {
    val seedStr =
      "otpauth://totp/$entryTitle:$entryUserName?secret=${secret}&period=$time&digits=$len&issuer=$entryTitle&algorithm=$arithmetic"

    return KeepassXcBean(otp = ProtectedString(true, seedStr))
  }
}