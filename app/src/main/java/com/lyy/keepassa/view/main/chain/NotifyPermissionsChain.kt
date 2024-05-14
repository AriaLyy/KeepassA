/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.main.chain

import android.Manifest
import android.os.Build
import android.text.Html
import android.widget.Button
import com.arialyy.frame.router.Routerfit
import com.arialyy.frame.util.ResUtil
import com.blankj.utilcode.util.PermissionUtils
import com.blankj.utilcode.util.PermissionUtils.SimpleCallback
import com.lyy.keepassa.R
import com.lyy.keepassa.router.DialogRouter
import com.lyy.keepassa.util.NotificationUtil
import com.lyy.keepassa.view.dialog.OnMsgBtClickListener
import timber.log.Timber

/**
 * @Author laoyuyu
 * @Description
 * @Date 2023/4/22
 **/
class NotifyPermissionsChain : IMainDialogInterceptor {
  override fun intercept(chain: DialogChain): MainDialogResponse {
    Timber.d("NotifyPermissionsChain")
    val ac = chain.activity
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
      return chain.proceed(ac)
    }

    if (PermissionUtils.isGranted(Manifest.permission.POST_NOTIFICATIONS)) {
      return chain.proceed(ac)
    }

    Routerfit.create(DialogRouter::class.java).showMsgDialog(
      msgContent = Html.fromHtml(ResUtil.getString(R.string.hint_notify_permission)),
      showCancelBt = true,
      cancelText = ResUtil.getString(R.string.cancel),
      enterText = ResUtil.getString(R.string.open_setting),
      btnClickListener = object : OnMsgBtClickListener {

        override fun onEnter(v: Button) {
          PermissionUtils.permission(Manifest.permission.POST_NOTIFICATIONS).callback(object :
            SimpleCallback {
            override fun onGranted() {
              NotificationUtil.startDbOpenNotify(ac)
            }

            override fun onDenied() {
            }
          }).request()
        }
      }
    )

    return MainDialogResponse(MainDialogResponse.RESPONSE_OK)
  }
}