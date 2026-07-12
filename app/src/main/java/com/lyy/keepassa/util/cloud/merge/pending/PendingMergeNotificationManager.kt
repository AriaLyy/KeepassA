package com.lyy.keepassa.util.cloud.merge.pending

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.util.isCanOpenQuickLock
import com.lyy.keepassa.view.launcher.LauncherActivity
import com.lyy.keepassa.view.main.QuickUnlockActivity

object PendingMergeNotificationManager {
  const val EXTRA_PENDING_MERGE_TASK_ID = "pending_merge_task_id"
  const val ACTION_RESOLVE_PENDING_MERGE = "com.lyy.keepassa.action.RESOLVE_PENDING_MERGE"
  const val CHANNEL_ID_PENDING_MERGE = "CHANNEL_PENDING_MERGE"
  const val REQUEST_CODE_PENDING_MERGE = 41
  const val NOTIFICATION_ID_PENDING_MERGE = 1041

  fun notify(taskId: String): Boolean {
    val context = BaseApp.APP
    if (!canPostNotifications(context)) {
      return false
    }
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      manager.createNotificationChannel(
        NotificationChannel(
          CHANNEL_ID_PENDING_MERGE,
          context.getString(R.string.pending_merge_notification_channel),
          NotificationManager.IMPORTANCE_DEFAULT
        )
      )
    }
    val notification = Notification.Builder(context)
      .setContentTitle(context.getString(R.string.pending_merge_notification_title))
      .setContentText(context.getString(R.string.pending_merge_notification_message))
      .setSmallIcon(R.drawable.ic_security_24px)
      .setContentIntent(createPendingIntent(context, taskId))
      .setAutoCancel(true)
      .apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          setChannelId(CHANNEL_ID_PENDING_MERGE)
        }
      }
      .build()
    manager.notify(NOTIFICATION_ID_PENDING_MERGE, notification)
    return true
  }

  private fun createPendingIntent(context: Context, taskId: String): PendingIntent {
    val target = PendingMergeNotificationTargetPolicy.choose(
      hasOpenDatabase = BaseApp.KDB != null,
      canQuickUnlock = BaseApp.APP.isCanOpenQuickLock()
    )
    val activity = when (target) {
      PendingMergeNotificationTarget.QUICK_UNLOCK -> QuickUnlockActivity::class.java
      PendingMergeNotificationTarget.FULL_UNLOCK -> LauncherActivity::class.java
    }
    val intent = Intent(context, activity).apply {
      action = ACTION_RESOLVE_PENDING_MERGE
      putExtra(EXTRA_PENDING_MERGE_TASK_ID, taskId)
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    return PendingIntent.getActivity(
      context,
      REQUEST_CODE_PENDING_MERGE,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
  }

  private fun canPostNotifications(context: Context): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
          context,
          Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
  }
}
