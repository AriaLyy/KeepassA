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
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.widget.Button
import com.arialyy.frame.router.Routerfit
import com.arialyy.frame.util.ResUtil
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.entity.CloudServiceInfo
import com.lyy.keepassa.event.DbPathEvent
import com.lyy.keepassa.router.DialogRouter
import com.lyy.keepassa.util.QuickUnLockUtil
import com.lyy.keepassa.util.cloud.DbSynUtil
import com.lyy.keepassa.util.cloud.WebDavUtil
import com.lyy.keepassa.view.StorageType.WEBDAV
import com.lyy.keepassa.view.create.CreateDbFirstFragment
import com.lyy.keepassa.view.dialog.OnMsgBtClickListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * @Author laoyuyu
 * @Description webdav auth flow
 * @Date 2021/2/25
 **/
class WebDavAuthFlow : IAuthFlow {
  private var context: Context? = null
  private lateinit var callback: IAuthCallback
  private var webDavUri: String? = null
  private var nextCallback: OnNextFinishCallback? = null
  private var dbName: String? = null
  private val scope = MainScope()
  private var isLogin = false
  private val loginDialog by lazy {
    Routerfit.create(DialogRouter::class.java).getWebDavLoginDialog()
  }
  private val fileSelectDialog by lazy {
    Routerfit.create(DialogRouter::class.java).getCloudFileListDialog(WEBDAV, true)
  }

  override fun initContent(
    context: Context,
    callback: IAuthCallback
  ) {
    this.context = context
    this.callback = callback
  }

  override fun startFlow() {
    // changeWebDav(false)
    scope.launch {
      loginDialog.webDavLoginFlow.collectLatest {
        isLogin = it.loginSuccess
        if (it.loginSuccess) {
          fileSelectDialog.show()
        }
      }
    }
    loginDialog.show()

    scope.launch {
      fileSelectDialog.cloudFileSelectFlow.collectLatest {
        if (it.storageType == WEBDAV) {
          webDavUri = it.fileFullPath
          callback.callback(true)
        }
      }
    }
  }

  override fun onResume() {
  }

  override fun doNext(
    fragment: CreateDbFirstFragment,
    dbName: String,
    callback: OnNextFinishCallback
  ) {
    this.dbName = "${dbName}.kdbx"
    if (!isLogin) {
      loginDialog.show()
      return
    }
    if (webDavUri == null) {
      fileSelectDialog.show()
      return
    }

    nextCallback = callback

    scope.launch {
      val fileExist = withContext(Dispatchers.IO) {
        return@withContext WebDavUtil.fileExists("${webDavUri!!}${this@WebDavAuthFlow.dbName}")
      }
      if (fileExist) {
        val content =
          ResUtil.getString(
            R.string.hint_cloud_file_already_exist,
            this@WebDavAuthFlow.dbName!!
          )
        val color = ResUtil.getColor(R.color.red)
        val ss = SpannableString(content)
        ss.setSpan(
          ForegroundColorSpan(color), 0, this@WebDavAuthFlow.dbName!!.length,
          Spannable.SPAN_INCLUSIVE_EXCLUSIVE
        )
        Routerfit.create(DialogRouter::class.java).showMsgDialog(
          msgTitle = ResUtil.getString(R.string.hint),
          msgContent = ss,
          btnClickListener = object : OnMsgBtClickListener{
            override fun onCover(v: Button) {
            }

            override fun onEnter(v: Button) {
              sendFinishEvent()
            }

            override fun onCancel(v: Button) {
            }
          }
        )
        return@launch
      }
      sendFinishEvent()
    }
  }

  private fun sendFinishEvent() {
    if (dbName == null) {
      return
    }
    scope.launch {
      saveWebDavServiceInfo("${webDavUri!!}${dbName}", WebDavUtil.userName, WebDavUtil.password)
      nextCallback?.onFinish(
        DbPathEvent(
          dbName = dbName!!,
          storageType = WEBDAV,
          fileUri = DbSynUtil.getCloudDbTempPath(WEBDAV.name, dbName!!),
          cloudDiskPath = "${webDavUri}${dbName}"
        )
      )
    }
  }

  private suspend fun saveWebDavServiceInfo(
    uri: String,
    userName: String,
    pass: String
  ) {
    withContext(Dispatchers.IO) {
      Timber.d("开始保存webDav登陆记录，uri = $uri")
      val dao = BaseApp.appDatabase.cloudServiceInfoDao()
      var data = dao.queryServiceInfo(uri)
      if (data == null) {
        data = CloudServiceInfo(
          userName = QuickUnLockUtil.encryptStr(userName),
          password = QuickUnLockUtil.encryptStr(pass),
          cloudPath = uri
        )
        dao.saveServiceInfo(data)
      } else {
        data.userName = QuickUnLockUtil.encryptStr(userName)
        data.password = QuickUnLockUtil.encryptStr(pass)
        dao.updateServiceInfo(data)
      }
    }
  }

  override fun onDestroy() {
    context = null
  }

  override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?
  ) {
  }
}