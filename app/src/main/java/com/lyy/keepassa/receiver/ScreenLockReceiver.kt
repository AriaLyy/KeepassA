/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.preference.PreferenceManager
import com.arialyy.frame.util.ResUtil
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.util.KdbUtil.isNull
import com.lyy.keepassa.util.KeepassAUtil

/**
 * @Author laoyuyu
 * @Description screen receiver, when the user lock screen, lock the db
 * @Date 2021/2/1
 **/
class ScreenLockReceiver : BroadcastReceiver() {
  override fun onReceive(
    context: Context?,
    intent: Intent?
  ) {
    // if the user lock screen, lock the db
    if (intent?.action.equals(Intent.ACTION_SCREEN_OFF) && PreferenceManager.getDefaultSharedPreferences(BaseApp.APP)
            .getBoolean(context?.getString(R.string.set_key_lock_screen_auto_lock_db), false)) {
      if (BaseApp.isLocked || BaseApp.KDB.isNull()){
        return
      }
      KeepassAUtil.instance.lock()
      return
    }
  }

}
