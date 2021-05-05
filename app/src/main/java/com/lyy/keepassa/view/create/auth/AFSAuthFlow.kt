/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.create.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.keepassdroid.utils.UriUtil
import com.lyy.keepassa.event.DbPathEvent
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.util.takePermission
import com.lyy.keepassa.view.StorageType.AFS
import com.lyy.keepassa.view.create.CreateDbFirstFragment

/**
 * @Author laoyuyu
 * @Description
 * @Date 2021/2/25
 **/
class AFSAuthFlow : IAuthFlow {
  private val PATH_REQUEST_CODE = 0xA1
  private var context: Context? = null
  private lateinit var authCallback: IAuthCallback
  private lateinit var nextCallback: OnNextFinishCallback
  private var dbUri: Uri? = null

  override fun initContent(
    context: Context,
    callback: IAuthCallback
  ) {
    this.context = context
    this.authCallback = callback
  }

  override fun startFlow() {
    authCallback.callback(true)
  }

  override fun onResume() {
  }

  override fun doNext(
    fragment: CreateDbFirstFragment,
    dbName: String,
    callback: OnNextFinishCallback
  ) {
    nextCallback = callback
    if (dbUri == null) {
      KeepassAUtil.instance.createFile(
          fragment, "*/*", "$dbName.kdbx", PATH_REQUEST_CODE
      )
      return
    }
    nextCallback.onFinish(
        DbPathEvent(
            dbName = UriUtil.getFileNameFromUri(context, dbUri),
            fileUri = dbUri,
            storageType = AFS
        )
    )
  }

  override fun onDestroy() {
  }

  override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?
  ) {
    dbUri = data?.data
    if (resultCode == Activity.RESULT_OK
        && requestCode == PATH_REQUEST_CODE
        && data != null
        && data.data != null
        && context != null
    ) {

      // 申请长期的uri权限
      // 防止一个不可思议的空指针，data.data 有可能还是为空
      data.data?.apply {
        takePermission()
        nextCallback.onFinish(
            DbPathEvent(
                dbName = UriUtil.getFileNameFromUri(context, this),
                fileUri = this,
                storageType = AFS
            )
        )
      }
    }
  }
}