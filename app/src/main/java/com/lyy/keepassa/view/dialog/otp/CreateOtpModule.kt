/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.dialog.otp

import com.lyy.keepassa.base.BaseModule
import com.lyy.keepassa.entity.IOtpBean
import com.lyy.keepassa.util.totp.OtpEnum
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * @Author laoyuyu
 * @Description
 * @Date 3:38 下午 2022/1/19
 **/
class CreateOtpModule : BaseModule() {
  companion object {
    val otpFlow = MutableSharedFlow<Pair<OtpEnum, IOtpBean?>>(0)
  }
}