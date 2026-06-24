/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.util

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import com.arialyy.frame.base.FrameApp
import com.arialyy.frame.util.ResUtil
import com.blankj.utilcode.util.PermissionUtils
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.view.launcher.LauncherActivity
import com.lyy.keepassa.view.main.MainActivity
import com.lyy.keepassa.view.main.QuickUnlockActivity
import timber.log.Timber

/**
 * 通知工具
 */
object NotificationUtil {
  private var notificationManager: NotificationManager
  private var CHANNEL_NAME_OPEN_DB: String = ""
  private val CHANNEL_ID_OPEN_DB = "CHANNEL_OPEN_DB"
  private const val TYPE_OPEN_DB = 1
  private const val TYPE_LOCK_DB = 2
  private const val TYPE_QUICK_UNLOCK_DB = 3

  // 数据库已解锁的的通知的id
  private val DB_UNLOCK_ID = 10001

  // 数据库启用快速解锁的通知的id
  private val DB_START_QUICK_UNLOCK = 10002

  init {
    CHANNEL_NAME_OPEN_DB = ResUtil.getString(R.string.notify_channel_db_open)
    notificationManager =
      FrameApp.context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      // 设置通知归属的渠道
      // 设置渠道，8.0上必须需要设置渠道，否则通知无法显示，参数分别为：渠道id，渠道名，优先级
      // 优先级 https://developer.android.com/guide/topics/ui/notifiers/notifications?hl=zh-cn#importance
      val channel = NotificationChannel(
        CHANNEL_ID_OPEN_DB,
        CHANNEL_NAME_OPEN_DB,
        NotificationManager.IMPORTANCE_LOW
      )
      notificationManager.createNotificationChannel(channel)
    }
  }

  /**
   * 打开数据库通知
   */
  fun startDbOpenNotify(context: Context) {
    startService(context, TYPE_OPEN_DB)
  }

  /**
   * 数据库已锁定，启动快速解锁
   */
  fun startQuickUnlockNotify(context: Context) {
    startService(context, TYPE_QUICK_UNLOCK_DB)
  }

  /**
   * 数据库已锁定通知
   */
  fun startDbLocked(context: Context) {
    startService(context, TYPE_LOCK_DB)
  }

  private fun startService(
    context: Context,
    type: Int
  ) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !PermissionUtils.isGranted(Manifest.permission.POST_NOTIFICATIONS)) {
      Timber.e("notify permission denied")
      return
    }

    val notify: Notification
    var notifyId = DB_UNLOCK_ID
    when (type) {

      TYPE_QUICK_UNLOCK_DB -> {
        notifyId = DB_START_QUICK_UNLOCK
        notify = createQuickUnlockNotify(context)
      }

      TYPE_LOCK_DB -> {
        notify = createDbLockedNotify(context)
      }

      else -> {
        notify = createDbUnlockNotify(context)
      }
    }

    notificationManager.notify(notifyId, notify)
  }

  /**
   * 创建数据库已解锁的通知
   */
  private fun createDbUnlockNotify(context: Context): Notification {
    return createDbNotify(context, ResUtil.getString(R.string.unlocked), createMainPending(context))
  }

  /**
   * 数据库启用快速解锁
   */
  private fun createQuickUnlockNotify(context: Context): Notification {
    return createDbNotify(
      context,
      ResUtil.getString(R.string.notify_quick_unlock_start),
      QuickUnlockActivity.createQuickUnlockPending(context)
    )
  }

  /**
   * 数据库已锁定
   */
  private fun createDbLockedNotify(context: Context): Notification {
    return createDbNotify(
      context,
      ResUtil.getString(R.string.notify_db_locked),
      LauncherActivity.createLauncherPending(context)
    )
  }

  private fun createDbNotify(
    context: Context,
    contentMsg: CharSequence,
    pendingIntent: PendingIntent
  ): Notification {

    val iconId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      R.drawable.ic_launcher_foreground
    } else {
      R.mipmap.ic_launcher
    }
    val builder: Notification.Builder = Notification.Builder(context)
      .setContentTitle(ResUtil.getString(R.string.app_name))
      .setContentText("${BaseApp.dbName}: $contentMsg")
      .setLargeIcon(IconUtil.getBitmapFromDrawable(context, iconId, -1))
      .setSmallIcon(R.drawable.ic_security_24px) // 状态栏图标
      .setContentIntent(pendingIntent)
      .setColor(Color.TRANSPARENT) // 大图标右下角的小图标

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      builder.setChannelId(CHANNEL_ID_OPEN_DB)
    }
    return builder.build()
  }

  /**
   * 主页
   */
  private fun createMainPending(context: Context): PendingIntent {
    return Intent(context, MainActivity::class.java).let { notificationIntent ->
      PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
    }
  }
}