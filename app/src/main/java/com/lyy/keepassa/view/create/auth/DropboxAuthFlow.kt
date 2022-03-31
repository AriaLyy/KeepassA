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
import android.widget.Button
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.OnLifecycleEvent
import com.arialyy.frame.router.Routerfit
import com.dropbox.core.android.Auth
import com.lyy.keepassa.R
import com.lyy.keepassa.event.DbPathEvent
import com.lyy.keepassa.router.DialogRouter
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.cloud.DbSynUtil
import com.lyy.keepassa.util.cloud.DropboxUtil
import com.lyy.keepassa.view.StorageType.DROPBOX
import com.lyy.keepassa.view.create.CreateDbFirstFragment
import com.lyy.keepassa.view.dialog.OnMsgBtClickListener
import timber.log.Timber

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
    Timber.d("onResume")
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
    Routerfit.create(DialogRouter::class.java)
      .showMsgDialog(
        msgContent = Html.fromHtml(context.getString(R.string.dropbox_msg)),
        showCancelBt = false,
        btnClickListener = object : OnMsgBtClickListener {
          override fun onCover(v: Button) {
          }

          override fun onEnter(v: Button) {
            Auth.startOAuth2Authentication(context, DropboxUtil.APP_KEY)
          }

          override fun onCancel(v: Button) {
          }

        }
      )
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
  override fun onDestroy() {
    Timber.d("onDestroy")
  }

  override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?
  ) {
  }
}