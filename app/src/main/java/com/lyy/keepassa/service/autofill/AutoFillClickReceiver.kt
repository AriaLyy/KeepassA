/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.service.autofill

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

/**
 * @Author laoyuyu
 * @Description
 * @Date 2021/7/10
 **/
class AutoFillClickReceiver: BroadcastReceiver() {
  companion object{
    const val ACTION_CLICK_OTHER = "ACTION_CLICK_OTHER"
  }

  override fun onReceive(context: Context, intent: Intent) {
    Timber.d("action = ${intent.action}")
  }
}