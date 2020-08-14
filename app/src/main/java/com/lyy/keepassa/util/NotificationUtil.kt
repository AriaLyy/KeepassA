package com.lyy.keepassa.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.TextUtils
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.service.DbOpenNotificationService

/**
 * 通知工具
 */
object NotificationUtil {

  /**
   * 打开数据库通知
   */
  fun startDbOpenNotify(context: Context) {
    if (!checkNeedNotify()) {
      return
    }
    val intent = Intent(context, DbOpenNotificationService::class.java).apply {
      putExtra(
          DbOpenNotificationService.KEY_NOTIFY_TYPE,
          DbOpenNotificationService.NOTIFY_TYPE_OPEN_DB
      )
    }
    startService(context, intent)
  }

  /**
   * 数据库已锁定，启动快速解锁
   */
  fun startQuickUnlockNotify(context: Context) {
    if (!checkNeedNotify()) {
      return
    }
    val intent = Intent(context, DbOpenNotificationService::class.java).apply {
      putExtra(
          DbOpenNotificationService.KEY_NOTIFY_TYPE,
          DbOpenNotificationService.NOTIFY_TYPE_QUICK_UNLOCK_DB
      )
    }
    startService(context, intent)
  }

  /**
   * 数据库已锁定通知
   */
  fun startDbLocked(context: Context) {
    if (!checkNeedNotify()) {
      return
    }
    val intent = Intent(context, DbOpenNotificationService::class.java).apply {
      putExtra(
          DbOpenNotificationService.KEY_NOTIFY_TYPE,
          DbOpenNotificationService.NOTIFY_TYPE_DB_LOCKED
      )
    }
    startService(context, intent)
  }

  /**
   * 检查是否需要通知
   * @return true 需要通知
   */
  private fun checkNeedNotify(): Boolean {
    return !(TextUtils.isEmpty(BaseApp.dbName) || BaseApp.KDB == null)
  }

  private fun startService(
    context: Context,
    intent: Intent
  ) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      /*
       * Android 在 8.0 限制了后台服务这些，启动后台服务需要设置通知栏，使服务变成前台服务。
       * 但是在 9.0 上，就会出现Permission Denial: startForeground requires
       */
      context.startForegroundService(intent)
    } else {
      context.startService(intent)
    }
  }

}