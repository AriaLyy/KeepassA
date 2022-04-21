/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.dialog

import android.content.Context
import android.net.Uri
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

class WebDavLoginModule : BaseModule() {


  fun checkLogin(
    uri: String,
    userName: String,
    pass: String
  ) = flow {
    val b = withContext(Dispatchers.IO) {
      return@withContext WebDavUtil.checkLogin(uri, userName, pass)
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
        e.printStackTrace()
      }
      return@withContext isSuccess
    }
    emit(success)
  }

  /**
   * 1、如果是坚果云，不允许使用 dav/
   * 2、检查文件夹是否存在，没有文件夹，创建文件夹，创建失败，则认为授权失败
   */
  fun handleCreateLoginFlow(
    context: Context,
    uri: String,
    userName: String,
    pass: String
  ) = liveData<Boolean> {
    val success = withContext(Dispatchers.IO) {
      var b = WebDavUtil.checkLogin(uri, userName, pass)
      if (!b) {
        return@withContext false
      }
      // 检查文件夹是否存在，不存在，创建文件夹
      val uriBean = Uri.parse(uri)
      val path = uriBean.path
      WebDavUtil.createDir(uri)


      return@withContext b
    }

    emit(success)
  }
}