/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.main.chain

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.Html
import android.widget.Button
import androidx.core.app.NotificationManagerCompat
import com.arialyy.frame.router.Routerfit
import com.arialyy.frame.util.ResUtil
import com.blankj.utilcode.util.PermissionUtils
import com.blankj.utilcode.util.PermissionUtils.SimpleCallback
import com.lyy.keepassa.R
import com.lyy.keepassa.base.KeyConstance
import com.lyy.keepassa.router.DialogRouter
import com.lyy.keepassa.util.NotificationUtil
import com.lyy.keepassa.util.PermissionCooldown
import com.lyy.keepassa.view.dialog.OnMsgBtClickListener
import com.lyy.keepassa.view.main.MainActivity
import timber.log.Timber

/**
 * @Author laoyuyu
 * @Description
 * @Date 2023/4/22
 **/
class NotifyPermissionsChain : IMainDialogInterceptor {

  private val cooldown = PermissionCooldown(KeyConstance.KEY_NOTIFY_PERMISSION_REJECTED_AT)

  override fun intercept(chain: DialogChain): MainDialogResponse {
    Timber.d("NotifyPermissionsChain")
    val ac = chain.activity

    // 通知总开关 + (Android 13+) 运行时权限均 OK 才算真正可用
    if (isNotifyReallyEnabled(ac)) {
      NotificationUtil.startDbOpenNotify(ac)
      return chain.proceed(ac)
    }

    // 用户点过拒绝且在 7 天冷却期内,直接放行,不再弹窗打扰
    if (cooldown.isInCooldown()) {
      return chain.proceed(ac)
    }

    // Android 13+ 走运行时权限弹窗;更低版本没有 POST_NOTIFICATIONS,只能引导去系统设置
    val useRuntimeRequest = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    Routerfit.create(DialogRouter::class.java).showMsgDialog(
      msgContent = Html.fromHtml(ResUtil.getString(R.string.hint_notify_permission)),
      showCancelBt = true,
      cancelText = ResUtil.getString(R.string.cancel),
      enterText = ResUtil.getString(if (useRuntimeRequest) R.string.auth else R.string.open_setting),
      btnClickListener = object : OnMsgBtClickListener {

        override fun onEnter(v: Button) {
          if (useRuntimeRequest) {
            requestNotifyPermission(ac)
          } else {
            openNotifySetting(ac)
          }
        }

        override fun onCancel(v: Button) {
          cooldown.recordRejection()
        }
      }
    )

    return MainDialogResponse(MainDialogResponse.RESPONSE_OK)
  }

  private fun isNotifyReallyEnabled(context: Context): Boolean {
    if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      PermissionUtils.isGranted(Manifest.permission.POST_NOTIFICATIONS)
    } else {
      true
    }
  }

  private fun requestNotifyPermission(activity: MainActivity) {
    PermissionUtils.permission(Manifest.permission.POST_NOTIFICATIONS).callback(object :
      SimpleCallback {
      override fun onGranted() {
        NotificationUtil.startDbOpenNotify(activity)
      }

      override fun onDenied() {
        // 用户勾选了"不再询问"等永久拒绝场景时,rationale 返回 false,此时兜底跳通知设置页
        if (!activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
          openNotifySetting(activity)
        }
      }
    }).request()
  }

  private fun openNotifySetting(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
      }
    } else {
      Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
      }
    }
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
  }
}
