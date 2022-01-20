/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.util

import androidx.preference.PreferenceManager
import com.keepassdroid.database.PwEntryV4
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp

/**
 * isOpenQuickLock
 * @return true already open quick lock
 */
fun BaseApp.isOpenQuickLock(): Boolean {
  return PreferenceManager.getDefaultSharedPreferences(this)
    .getBoolean(applicationContext.getString(R.string.set_quick_unlock), false)
}

fun PwEntryV4.hasNote():Boolean{
  for (str in this.strings){
    if (str.key.equals(PwEntryV4.STR_NOTES, true)){
      return true
    }
  }
  return false
}

fun PwEntryV4.hasTOTP(): Boolean {
  for (str in this.strings) {
    if (str.key.equals(PwEntryV4.STR_NOTES, true)
      || str.key.equals(PwEntryV4.STR_PASSWORD, true)
      || str.key.equals(PwEntryV4.STR_TITLE, true)
      || str.key.equals(PwEntryV4.STR_URL, true)
      || str.key.equals(PwEntryV4.STR_USERNAME, true)
    ) {
      continue
    }

    // 增加TOP密码字段
    if (str.key.startsWith("TOTP", ignoreCase = true)
      || str.key.startsWith("OTP", ignoreCase = true)
      || str.key.startsWith("HmacOtp", ignoreCase = true)
      || str.key.startsWith("TimeOtp", ignoreCase = true)
    ) {
      return true
    }
  }
  return false
}