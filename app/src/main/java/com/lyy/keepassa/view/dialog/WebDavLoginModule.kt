/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.dialog

import android.content.Context
import androidx.lifecycle.liveData
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.BaseModule
import com.lyy.keepassa.entity.CloudServiceInfo
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.QuickUnLockUtil
import com.lyy.keepassa.util.cloud.WebDavUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import timber.log.Timber

class WebDavLoginModule : BaseModule() {

  var curWebDavServer: String? = null

  fun isNextcloud() = curWebDavServer == WebDavUtil.SUPPORTED_WEBDAV_URLS[3]

  fun isOtherServer() =
    curWebDavServer == WebDavUtil.SUPPORTED_WEBDAV_URLS[WebDavUtil.SUPPORTED_WEBDAV_URLS.size - 1]

  fun isJGY() = curWebDavServer == WebDavUtil.SUPPORTED_WEBDAV_URLS[0]

  fun convertHost(hostName: String, port: String, userName: String): String {
    val hasHttp = hostName.startsWith("http", true)
    val isHttps = hostName.startsWith("https", true) || port == "443"
    var temp = hostName
    if (hasHttp) {
      val index = hostName.indexOf("://")
      if (index != -1) {
        temp = hostName.substring(index + 2, hostName.length)
      }
    }
    return "http${if (isHttps) "s" else ""}://${temp}${if (port.isEmpty()) "" else ":${port}"}/remote.php/dav/files/${userName}/"
  }

  fun checkLogin(
    uri: String,
    userName: String,
    pass: String,
    isPreemptive:Boolean
  ) = flow {
    val b = withContext(Dispatchers.IO) {
      return@withContext WebDavUtil.checkLogin(uri, userName, pass, isPreemptive)
    }
    emit(b)
  }

  /**
   * 检查登录状态
   * 如果是创建数据库，不考虑文件是否存在
   * 如果是打开云端数据，如果文件不存在，则表示登录失败
   */
  fun checkLogin(
    context: Context,
    uri: String,
    userName: String,
    pass: String
  ) = liveData {
    val success = withContext(Dispatchers.IO) {
      var isSuccess = false
      try {
        WebDavUtil.login(uri, userName, pass)
        val b = WebDavUtil.getFileInfo(uri) != null
        if (!b) {
          HitUtil.toaskShort(context.getString(R.string.db_file_no_exist))
          return@withContext false
        }
        // 保存记录
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
        isSuccess = true
      } catch (e: Exception) {
        Timber.e(e)
      }
      return@withContext isSuccess
    }
    emit(success)
  }

}