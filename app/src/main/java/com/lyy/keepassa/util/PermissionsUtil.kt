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
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.text.Html
import android.view.autofill.AutofillManager
import android.widget.Button
import com.arialyy.frame.router.Routerfit
import com.arialyy.frame.util.ResUtil
import com.blankj.utilcode.util.RomUtils
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.router.DialogRouter
import com.lyy.keepassa.view.dialog.OnMsgBtClickListener
import timber.log.Timber
import java.lang.reflect.Method

object PermissionsUtil {

  /**
   * 是否需要弹出后台启动提示弹窗
   */
  fun needShowBackgroundStartDialog(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      return false
    }
    val am = context.getSystemService(AutofillManager::class.java)
    if (!am.isAutofillSupported) {
      Timber.i("不支持自动填充")
      return false
    }
    if (am.hasEnabledAutofillServices() && !isCanBackgroundStart()) {
      Timber.i("已经打开了自动填充")
      return true
    }
    return false
  }

  /**
   * 显示弹出框提示用户打开后台启动界面的权限
   */
  fun showAutoFillMsgDialog(context: Context, msg: String) {
//    val IS_HOWED_AUTO_FILL_HINT_DIALOG = "IS_HOWED_AUTO_FILL_HINT_DIALOG"
//    val isShowed =
//      SharePreUtil.getBoolean(
//        Constance.PRE_FILE_NAME,
//        context,
//        IS_HOWED_AUTO_FILL_HINT_DIALOG
//      )
//
//    if (!isShowed) {
    Routerfit.create(DialogRouter::class.java).showMsgDialog(
      msgContent = Html.fromHtml(BaseApp.APP.getString(R.string.hint_background_start, msg)),
      showCancelBt = true,
      cancelText = ResUtil.getString(R.string.cancel),
      enterText = ResUtil.getString(R.string.open_setting),
      btnClickListener = object : OnMsgBtClickListener {
        override fun onEnter(v: Button) {
          PermissionPageManagement.goToSetting(context)
        }

        override fun onCancel(v: Button) {
        }
      }
    )
//      SharePreUtil.putBoolean(
//        Constance.PRE_FILE_NAME,
//        context,
//        IS_HOWED_AUTO_FILL_HINT_DIALOG,
//        true
//      )
//    } else {
//      Timber.i("已显示过自动填充对话框，不再重复显示")
//    }
  }

  fun isCanBackgroundStart(): Boolean {
    if (RomUtils.isXiaomi()) {
      return miuiCanBackgroundStart()
    }
    if (RomUtils.isVivo()) {
      return vivoBackgroundStartAllowed()
    }
    if (RomUtils.isHuawei()) {
      return hwBackgroundStartAllowed(BaseApp.APP)
    }
    return true
  }

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
          val index = cursor.getColumnIndex("currentstate")
          if (index == -1) {
            false
          } else {
            val currentmode = cursor.getInt(index)
            cursor.close()
            currentmode == 0
          }
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

  fun hwBackgroundStartAllowed(context: Context): Boolean {
    try {
      val c = Class.forName("com.huawei.android.app.AppOpsManagerEx")
      val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
      val m: Method = c.getDeclaredMethod(
        "checkHwOpNoThrow",
        AppOpsManager::class.java,
        Int::class.java,
        Int::class.java,
        String::class.java
      )
      return m.invoke(
        c.newInstance(), ops, 100000, Process.myUid(), context.packageName
      ) as Int == AppOpsManager.MODE_ALLOWED
    } catch (e: Exception) {
      return false
    }
  }

  /**
   * 检查oppo 是否被允许后台启动
   *
   * @return true 允许后台启动
   */
  fun oppoBackgroundStartAllowed(): Boolean {
    return Settings.canDrawOverlays(BaseApp.APP)
  }

  // fun testCheckHwOp() {
  //   val c = Class.forName("com.huawei.android.app.AppOpsManagerEx")
  //   val m: Method = c.getDeclaredMethod(
  //     "checkHwOpNoThrow",
  //     AppOpsManager::class.java,
  //     Int::class.javaPrimitiveType,
  //     Int::class.javaPrimitiveType,
  //     String::class.java
  //   )
  //   val bundle: Bundle = getNoteParamInt()
  //   val op = bundle.getInt(KEY_OP_CODES)
  //   val packageName = bundle.getString(KEY_PKG_NAME)
  //   val uid = bundle.getInt(KEY_UID)
  //   val checkResult = m.invoke(
  //     c.newInstance(), arrayOf<Any?>(
  //       context.getSystemService(
  //         Context.APP_OPS_SERVICE
  //       ) as AppOpsManager, op, uid, packageName
  //     )
  //   ) as Int
  //   com.sun.corba.se.impl.activation.ServerMain.printResult("check result:$checkResult    op:$op uid:$uid packageName:$packageName")
  // }
}