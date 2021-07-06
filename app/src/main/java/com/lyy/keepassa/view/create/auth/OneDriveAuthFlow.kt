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
import com.lyy.keepassa.event.DbPathEvent
import com.lyy.keepassa.util.cloud.DbSynUtil
import com.lyy.keepassa.util.cloud.OneDriveUtil
import com.lyy.keepassa.view.StorageType.ONE_DRIVE
import com.lyy.keepassa.view.create.CreateDbFirstFragment
import com.lyy.keepassa.view.dialog.LoadingDialog
import timber.log.Timber

/**
 * @Author laoyuyu
 * @Description
 * @Date 2021/4/26
 **/
class OneDriveAuthFlow : IAuthFlow {
  private val TAG = javaClass.simpleName
  private lateinit var context: Context
  private lateinit var callback: IAuthCallback
  private var loginCallback: OneDriveUtil.OnLoginCallback? = null
  private var isAuthid = false
  private var loadingDialog: LoadingDialog? = null

  override fun initContent(
    context: Context,
    callback: IAuthCallback
  ) {
    this.context = context
    this.callback = callback
  }

  override fun startFlow() {
    auth()
  }

  override fun onResume() {
  }

  override fun doNext(
    fragment: CreateDbFirstFragment,
    dbName: String,
    callback: OnNextFinishCallback
  ) {
    if (!isAuthid) {
      auth()
      return
    }
    val name = "$dbName.kdbx"
    callback.onFinish(
        DbPathEvent(
            dbName = name,
            fileUri = DbSynUtil.getCloudDbTempPath(ONE_DRIVE.name, name),
            storageType = ONE_DRIVE,
            cloudDiskPath = "/$name"
        )
    )
  }

  private fun auth() {
    if (isAuthid){
      Timber.d( "已经完成授权")
      return
    }
    loadingDialog = LoadingDialog(context)
    loadingDialog?.show()
    OneDriveUtil.initOneDrive {
      if (it) {
        OneDriveUtil.loadAccount()
        return@initOneDrive
      }
      this.callback.callback(false)
      loadingDialog?.dismiss()
    }
    OneDriveUtil.loginCallback = object : OneDriveUtil.OnLoginCallback {
      override fun callback(success: Boolean) {
        isAuthid = success
        this@OneDriveAuthFlow.callback.callback(success)
        loadingDialog?.dismiss()
      }
    }
  }

  override fun onDestroy() {
    loginCallback = null
  }

  override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?
  ) {
  }
}