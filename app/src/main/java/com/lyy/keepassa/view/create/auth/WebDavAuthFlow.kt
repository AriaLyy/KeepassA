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
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseActivity
import com.lyy.keepassa.event.DbPathEvent
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.KLog
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.util.cloud.DbSynUtil
import com.lyy.keepassa.util.cloud.WebDavUtil
import com.lyy.keepassa.util.putArgument
import com.lyy.keepassa.view.DbPathType.WEBDAV
import com.lyy.keepassa.view.create.CreateDbFirstFragment
import com.lyy.keepassa.view.dialog.WebDavLoginDialog

/**
 * @Author laoyuyu
 * @Description webdav auth flow
 * @Date 2021/2/25
 **/
class WebDavAuthFlow : IAuthFlow {
  private val TAG = javaClass.simpleName
  private var context: Context? = null
  private lateinit var callback: IAuthCallback
  private var webDavUri: String? = null
  private var nextCallback: OnNextFinishCallback? = null
  private var dbName: String? = null

  override fun initContent(
    context: Context,
    callback: IAuthCallback
  ) {
    this.context = context
    this.callback = callback
  }

  override fun startFlow() {
    changeWebDav(false)
  }

  override fun onResume() {
  }

  /**
   * 选择webdav路径，登录成功后直接进入下一个页面
   */
  private fun changeWebDav(isDoNext: Boolean) {
    val webDavDialog = WebDavLoginDialog().apply {
      putArgument("webDavIsCreateLogin", true)
    }
    webDavDialog.setOnDismissListener {
      if (!WebDavUtil.isLogin()) {
        callback.callback(false)
        return@setOnDismissListener
      }
      webDavUri = webDavDialog.getWebDavUri()
      if (webDavUri.isNullOrEmpty()){
        HitUtil.toaskShort(
            context!!.getString(R.string.hint_please_input, context!!.getString(R.string.hint_webdav_url))
        )
        callback.callback(false)
        return@setOnDismissListener
      }
      KLog.d(TAG, "webDavUri: $webDavUri")
      if (isDoNext){
        sendFinishEvent()
        return@setOnDismissListener
      }
      callback.callback(true)
    }
    webDavDialog.show((context as BaseActivity<*>).supportFragmentManager, "web_dav_login")
  }

  override fun doNext(
    fragment: CreateDbFirstFragment,
    dbName: String,
    callback: OnNextFinishCallback
  ) {
    this.dbName = dbName
    nextCallback = callback
    if (!checkWebDavUri(context!!, webDavUri) || !WebDavUtil.isLogin()) {
      changeWebDav(true)
      return
    }
    sendFinishEvent()
  }

  private fun sendFinishEvent() {
    if (dbName == null) {
      return
    }
    nextCallback?.onFinish(
        DbPathEvent(
            dbName = dbName!!,
            dbPathType = WEBDAV,
            fileUri = DbSynUtil.getCloudDbTempPath(WEBDAV.name, dbName!!),
            cloudDiskPath = "${webDavUri}${dbName}.kdbx"
        )
    )
  }

  /**
   * 检查webdav的uri，如果uri非法提示
   * @return false uri无效
   */
  private fun checkWebDavUri(
    context: Context,
    uri: String?
  ): Boolean {
    if (uri.isNullOrEmpty()) {
      HitUtil.toaskShort(
          context.getString(R.string.hint_please_input, context.getString(R.string.hint_webdav_url))
      )
      return false
    }
    if (!KeepassAUtil.instance.checkUrlIsValid(uri)) {
      HitUtil.toaskShort(
          "${context.getString(R.string.hint_webdav_url)} ${context.getString(R.string.invalid)}"
      )
      return false
    }
    return true
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