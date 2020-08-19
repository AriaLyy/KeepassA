/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.util;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.Service;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import com.lyy.keepassa.base.BaseApp;
import java.lang.reflect.Method;
import java.util.List;

public class PermissionsUtil {
  private static final String TAG = "PerUtil";

  /**
   * 检查miui 是否被允许后台启动
   *
   * @return true 允许后台启动
   */
  public static boolean miuiBackgroundStartAllowed(Context context) {
    AppOpsManager ops = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
    try {
      int op = 10021;
      Method method = ops.getClass()
          .getMethod("checkOpNoThrow", new Class[] { int.class, int.class, String.class });
      Integer result = (Integer) method.invoke(ops, op, android.os.Process.myUid(),
          BaseApp.APP.getPackageName());
      return result == AppOpsManager.MODE_ALLOWED;
    } catch (Exception e) {
      Log.e(TAG, "not support");
    }
    return false;
  }

  /**
   * 判断vivo后台弹出界面 1未开启 0开启
   */
  public static boolean vivoBackgroundStartAllowed(Context context) {
    String packageName = context.getPackageName();
    Uri uri2 =
        Uri.parse("content://com.vivo.permissionmanager.provider.permission/start_bg_activity");
    String selection = "pkgname = ?";
    String[] selectionArgs = new String[] { packageName };
    try {
      Cursor cursor = context
          .getContentResolver()
          .query(uri2, null, selection, selectionArgs, null);
      if (cursor != null) {
        if (cursor.moveToFirst()) {
          int currentmode = cursor.getInt(cursor.getColumnIndex("currentstate"));
          cursor.close();
          return currentmode == 0;
        } else {
          cursor.close();
          return false;
        }
      }
    } catch (Throwable throwable) {
      throwable.printStackTrace();
    }
    return false;
  }

  /**
   * 判断当前应用是否处于前台
   */
  public static boolean isAppForeground(Context context) {
    ActivityManager activityManager =
        (ActivityManager) context.getSystemService(Service.ACTIVITY_SERVICE);
    if (activityManager == null) {
      return false;
    }
    List<ActivityManager.RunningAppProcessInfo> runningAppProcessInfoList =
        activityManager.getRunningAppProcesses();
    if (runningAppProcessInfoList == null) {
      Log.d(TAG, "runningAppProcessInfoList is null!");
      return false;
    }

    for (ActivityManager.RunningAppProcessInfo processInfo : runningAppProcessInfoList) {
      if (processInfo.processName.equals(context.getPackageName())
          && (processInfo.importance ==
          ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND)) {
        return true;
      }
    }
    return false;
  }
}