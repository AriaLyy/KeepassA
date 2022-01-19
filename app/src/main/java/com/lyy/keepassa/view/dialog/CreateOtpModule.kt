/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.dialog

import com.keepassdroid.database.PwEntryV4
import com.lyy.keepassa.base.BaseModule

/**
 * @Author laoyuyu
 * @Description
 * @Date 3:38 下午 2022/1/19
 **/
class CreateOtpModule : BaseModule() {
  lateinit var entry: PwEntryV4



  // fun handleOtpStr(str: String) = flow<Pair<Boolean, String?>> {
  //   if (!str.startsWith("otpauth://")){
  //     emit(Pair(false, ResUtil.getString(R.string.error_qr_code_str)))
  //     return@flow
  //   }
  //   val uri = Uri.parse()
  // }
}