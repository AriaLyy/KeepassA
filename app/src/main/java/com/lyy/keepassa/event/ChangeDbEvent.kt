/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.event

import android.net.Uri
import com.lyy.keepassa.view.StorageType

/**
 * 切换数据库的事件
 */
data class ChangeDbEvent(
  /**
   * 数据库名
   */
  var dbName: String,
  /**
   * 本地文件路径
   */
  var localFileUri: Uri,
  /**
   * 云端文件路径
   */
  var cloudPath: String? = null,
  var uriType: StorageType = StorageType.AFS, // uri类型，afs，google drive,
  var keyUri: Uri? = null
)