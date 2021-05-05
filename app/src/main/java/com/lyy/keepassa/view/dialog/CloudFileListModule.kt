/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.dialog

import androidx.lifecycle.liveData
import com.lyy.keepassa.base.BaseModule
import com.lyy.keepassa.util.cloud.CloudFileInfo
import com.lyy.keepassa.util.cloud.CloudUtilFactory
import com.lyy.keepassa.view.StorageType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 云文件列表module
 */
class CloudFileListModule : BaseModule() {
  private val cache = HashMap<String, List<CloudFileInfo>?>()

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
          val utile = CloudUtilFactory.getCloudUtil(storageType)
          val list = utile.getFileList(path)
          cache[path] = list
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