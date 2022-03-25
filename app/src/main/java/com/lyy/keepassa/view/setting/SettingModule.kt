/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.setting

import android.content.Context
import android.net.Uri
import com.keepassdroid.utils.UriUtil
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.BaseModule
import com.lyy.keepassa.util.KdbUtil
import com.lyy.keepassa.util.KpaUtil
import com.lyy.keepassa.util.QuickUnLockUtil
import com.lyy.keepassa.util.cloud.DbSynUtil

/**
 * 设置页面的module
 */
class SettingModule : BaseModule() {

  /**
   * 修改数据库密码
   */
  fun modifyDbName(newDbName: String, callback: (Int) -> Unit) {
    BaseApp.KDB.pm.name = newDbName
    KpaUtil.kdbService.saveDbByForeground(callback = callback)
  }

  /**
   * 修改密码
   */
  fun modifyPass(
    context: Context,
    newPass: String,
    callback: (Int) -> Unit
  ) {
    if (BaseApp.dbKeyPath == null || BaseApp.dbKeyPath.isEmpty()) {
      BaseApp.KDB.pm.setMasterKey(newPass, null)
    } else {
      val ios = UriUtil.getUriInputStream(
        context, Uri.parse(QuickUnLockUtil.decryption(BaseApp.dbKeyPath))
      )
      BaseApp.KDB.pm.setMasterKey(newPass, ios)
    }
    KpaUtil.kdbService.saveDbByBackground(callback = callback)
  }
}