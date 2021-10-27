/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.util

import androidx.preference.PreferenceManager
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