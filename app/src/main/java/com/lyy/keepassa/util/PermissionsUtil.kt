/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.util;

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Process
import com.lyy.keepassa.base.BaseApp
import timber.log.Timber

object PermissionsUtil {

  /**
   * 检查miui 是否被允许后台启动
   *
   * @return true 允许后台启动
   */
  fun miuiCanBackgroundStart(): Boolean {
    val ops = BaseApp.APP.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    try {
      val op = 10021
      val method = ops.javaClass.getMethod(
        "checkOpNoThrow",
        Int::class.java, Int::class.java, String::class.java
      )
      val result = method.invoke(ops, op, Process.myUid(), BaseApp.APP.packageName) as Int
      return result == AppOpsManager.MODE_ALLOWED
    } catch (e: Exception) {
      Timber.e(e, "not support")
    }
    return false
  }

  /**
   * 启动miui权限打开界面
   */
  fun miuiStartPermissionsUI(context: Context){
    val xiaomiBackGroundIntent = Intent().apply {
      action = "miui.intent.action.APP_PERM_EDITOR"
      addCategory(Intent.CATEGORY_DEFAULT)
      putExtra("extra_pkgname", BaseApp.APP.packageName)
    }
    context.startActivity(xiaomiBackGroundIntent)
  }

  /**
   * 判断vivo后台弹出界面 1未开启 0开启
   */
  fun vivoBackgroundStartAllowed(): Boolean {
    val packageName = BaseApp.APP.packageName
    val uri2 =
      Uri.parse("content://com.vivo.permissionmanager.provider.permission/start_bg_activity")
    val selection = "pkgname = ?"
    val selectionArgs = arrayOf(packageName)
    try {
      val cursor = BaseApp.APP
        .contentResolver
        .query(uri2, null, selection, selectionArgs, null);
      if (cursor != null) {
        return if (cursor.moveToFirst()) {
          val currentmode = cursor.getInt(cursor.getColumnIndex("currentstate"))
          cursor.close()
          currentmode == 0
        } else {
          cursor.close()
          false
        }
      }
    } catch (throwable: Throwable) {
      Timber.d(throwable)
    }
    return false
  }
}