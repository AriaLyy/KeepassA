/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import com.arialyy.frame.base.FrameApp.context
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.BaseService
import com.lyy.keepassa.util.IconUtil
import com.lyy.keepassa.view.launcher.LauncherActivity
import com.lyy.keepassa.view.main.MainActivity
import com.lyy.keepassa.view.main.QuickUnlockActivity

/**
 * 数据库打开通知
 */
class DbOpenNotificationService : BaseService() {

  companion object {
    const val KEY_NOTIFY_TYPE = "KEY_NOTIFY_TYPE"

    // 数据库已解锁
    const val NOTIFY_TYPE_OPEN_DB = 1

    // 数据库已锁定，启动快速解锁
    const val NOTIFY_TYPE_QUICK_UNLOCK_DB = 2

    // 数据库已锁定
    const val NOTIFY_TYPE_DB_LOCKED = 3
  }

  private val CHANNEL_ID_OPEN_DB = "CHANNEL_OPEN_DB"
  private var CHANNEL_NAME_OPEN_DB: String = ""

  // 数据库已解锁的的通知的id
  private val DB_UNLOCK_ID = 10001

  // 数据库启用快速解锁的通知的id
  private val DB_START_QUICK_UNLOCK = 10002
  private lateinit var notificationManager: NotificationManager

  override fun onCreate() {
    super.onCreate()
    CHANNEL_NAME_OPEN_DB = getText(R.string.notify_channel_db_open) as String
    notificationManager =
      context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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

  override fun onBind(intent: Intent?): IBinder? {
    return null
  }

  override fun onStartCommand(
    intent: Intent?,
    flags: Int,
    startId: Int
  ): Int {

//    notificationManager.notify(CHANNEL_OPEN_DB, builder.build());
    val notify: Notification
    var notifyId = DB_UNLOCK_ID
    when (intent?.getIntExtra(KEY_NOTIFY_TYPE, NOTIFY_TYPE_OPEN_DB) ?: DB_START_QUICK_UNLOCK) {
      NOTIFY_TYPE_QUICK_UNLOCK_DB -> {
        notifyId = DB_START_QUICK_UNLOCK
        notify = createQuickUnlockNotify()
      }
      NOTIFY_TYPE_DB_LOCKED -> {
        notify = createDbLockedNotify()
      }
      else -> {
        notify = createDbUnlockNotify()
      }
    }

    startForeground(notifyId, notify)
    return super.onStartCommand(intent, flags, startId)
  }

  /**
   * 创建数据库已解锁的通知
   */
  private fun createDbUnlockNotify(): Notification {
    return createDbNotify(getText(R.string.unlocked), createMainPending())
  }

  /**
   * 数据库启用快速解锁
   */
  private fun createQuickUnlockNotify(): Notification {
    return createDbNotify(
        getText(R.string.notify_quick_unlock_start),
        QuickUnlockActivity.createQuickUnlockPending(this)
    )
  }

  /**
   * 数据库已锁定
   */
  private fun createDbLockedNotify(): Notification {
    return createDbNotify(getText(R.string.notify_db_locked), LauncherActivity.createLauncherPending(this))
  }

  private fun createDbNotify(
    contentMsg: CharSequence,
    pendingIntent: PendingIntent
  ): Notification {

    val iconId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      R.drawable.ic_launcher_foreground
    } else {
      R.mipmap.ic_launcher
    }
    val builder: Notification.Builder = Notification.Builder(this)
        .setContentTitle(getText(R.string.app_name))
        .setContentText("${BaseApp.dbName}: $contentMsg")
        .setLargeIcon(IconUtil.getBitmapFromDrawable(this, iconId, -1))
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
  private fun createMainPending(): PendingIntent {
    return Intent(this, MainActivity::class.java).let { notificationIntent ->
      PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
    }
  }

}