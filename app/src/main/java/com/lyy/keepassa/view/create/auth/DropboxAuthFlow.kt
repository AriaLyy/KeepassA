/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.create.auth

import android.content.Context
import android.content.Intent
import android.text.Html
import android.text.TextUtils
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.OnLifecycleEvent
import com.dropbox.core.android.Auth
import com.lyy.keepassa.R
import com.lyy.keepassa.event.DbPathEvent
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.KLog
import com.lyy.keepassa.util.cloud.DbSynUtil
import com.lyy.keepassa.util.cloud.DropboxUtil
import com.lyy.keepassa.view.StorageType.DROPBOX
import com.lyy.keepassa.view.create.CreateDbFirstFragment
import com.lyy.keepassa.view.dialog.MsgDialog
import com.lyy.keepassa.view.dialog.MsgDialog.OnBtClickListener

/**
 * @Author laoyuyu
 * @Description default save db to root path (eg: "/")
 * @Date 2021/2/25
 **/
class DropboxAuthFlow : IAuthFlow {
  private val TAG = javaClass.simpleName
  private lateinit var context: Context
  private var isNeedAuth = false
  private lateinit var callback: IAuthCallback

  override fun initContent(
    context: Context,
    callback: IAuthCallback
  ) {
    this.context = context
    this.callback = callback
  }

  override fun onResume() {
    KLog.d(TAG, "onResume")
    if (!isNeedAuth || DropboxUtil.isAuthorized()) {
      return
    }
    val token = Auth.getOAuth2Token()
    if (!TextUtils.isEmpty(token)) {
      DropboxUtil.saveToken(token)
      HitUtil.toaskShort("dropbox ${context.getString(R.string.auth)}${context.getString(R.string.success)}")
      callback.callback(true)
      return
    }
    HitUtil.toaskShort("dropbox ${context.getString(R.string.auth)}${context.getString(R.string.fail)}")
    callback.callback(false)
  }

  override fun doNext(
    fragment: CreateDbFirstFragment,
    dbName: String,
    callback: OnNextFinishCallback
  ) {
    if (!DropboxUtil.isAuthorized()) {
      authDropbox()
      return
    }
    val name = "$dbName.kdbx"
    callback.onFinish(
        DbPathEvent(
            dbName = name,
            fileUri = DbSynUtil.getCloudDbTempPath(DROPBOX.name, name),
            storageType = DROPBOX,
            cloudDiskPath = "/$name"
        )
    )
  }

  override fun startFlow() {
    isNeedAuth = true
    if (!DropboxUtil.isAuthorized()) {
      isNeedAuth = true
      authDropbox()
      return
    }
  }

  /**
   * 选择dropbox路径
   * 只有dropbox为授权才显示该对话框
   */
  private fun authDropbox() {
    val msgDialog = MsgDialog.generate {
      msgTitle = this@DropboxAuthFlow.context.getString(R.string.hint)
      msgContent = Html.fromHtml(this@DropboxAuthFlow.context.getString(R.string.dropbox_msg))
      showCancelBt = false
      build()
    }
    msgDialog.setOnBtClickListener(object : OnBtClickListener {
      override fun onBtClick(
        type: Int,
        view: View
      ) {
        Auth.startOAuth2Authentication(context, DropboxUtil.APP_KEY)
      }
    })
    msgDialog.show()
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
  override fun onDestroy() {
    KLog.d(TAG, "onDestroy")
  }

  override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?
  ) {
  }
}