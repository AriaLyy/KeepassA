/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.event

import android.net.Uri
import com.lyy.keepassa.view.DbPathType

/**
 * 选择数据库事件
 */
data class DbPathEvent(
  /**
   * 数据库名
   */
  var dbName: String,
  /**
   * 本地数据库uri
   */
  var fileUri: Uri? = null,
  var dbPathType: DbPathType = DbPathType.AFS, // uri类型，afs，google drive
  /**
   * 云端路径
   */
  var cloudDiskPath: String? = null
)