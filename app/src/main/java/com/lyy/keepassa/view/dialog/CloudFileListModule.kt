/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.dialog

import androidx.lifecycle.liveData
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.BaseModule
import com.lyy.keepassa.entity.CloudServiceInfo
import com.lyy.keepassa.util.QuickUnLockUtil
import com.lyy.keepassa.util.cloud.CloudFileInfo
import com.lyy.keepassa.util.cloud.CloudUtilFactory
import com.lyy.keepassa.util.cloud.WebDavUtil
import com.lyy.keepassa.view.StorageType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

/**
 * 云文件列表module
 */
class CloudFileListModule : BaseModule() {
  private val cache = HashMap<String, List<CloudFileInfo>?>()

  val upEntry = CloudFileInfo("", "..", Date(), 0, true)

  suspend fun saveWebHistory(uri: String) {
    withContext(Dispatchers.IO) {
      // 保存记录
      val dao = BaseApp.appDatabase.cloudServiceInfoDao()
      var data = dao.queryServiceInfo(uri)
      if (data == null) {
        data = CloudServiceInfo(
          userName = QuickUnLockUtil.encryptStr(WebDavUtil.userName),
          password = QuickUnLockUtil.encryptStr(WebDavUtil.pass),
          cloudPath = uri
        )
        dao.saveServiceInfo(data)
        return@withContext
      }
      data.userName = QuickUnLockUtil.encryptStr(WebDavUtil.userName)
      data.password = QuickUnLockUtil.encryptStr(WebDavUtil.pass)
      dao.updateServiceInfo(data)
    }
  }

  /**
   * 获取云盘根路径
   */
  fun getCloudRootPath(storageType: StorageType): String {
    return CloudUtilFactory.getCloudUtil(storageType)
      .getRootPath()
  }

  /**
   * 获取云文件指定路径的文件列表
   */
  fun getFileList(
    storageType: StorageType,
    path: String
  ) = liveData {
    val temp = cache[path]
    if (temp != null && temp.isNotEmpty()) {
      emit(temp)
    } else {
      val data = withContext(Dispatchers.IO) {
        try {
          val util = CloudUtilFactory.getCloudUtil(storageType)
          val list = util.getFileList(path)
          val dirList = mutableListOf<CloudFileInfo>()
          val fileList = mutableListOf<CloudFileInfo>()
          val tempList = mutableListOf<CloudFileInfo>()
          list?.forEach {
            if (it.isDir) {
              dirList.add(it)
            } else {
              fileList.add(it)
            }
          }
          tempList.addAll(dirList)
          tempList.addAll(fileList)
          cache[path] = tempList
          return@withContext list
        } catch (e: Exception) {
          e.printStackTrace()
        }
        null
      }
      emit(data)
    }
  }
}