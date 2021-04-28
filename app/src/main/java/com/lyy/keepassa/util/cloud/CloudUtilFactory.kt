/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.util.cloud

import com.lyy.keepassa.view.DbPathType
import com.lyy.keepassa.view.DbPathType.DROPBOX
import com.lyy.keepassa.view.DbPathType.ONE_DRIVE
import com.lyy.keepassa.view.DbPathType.WEBDAV

/**
 * 云端文件工具工厂
 */
object CloudUtilFactory {

  fun getCloudUtil(dbPathType: DbPathType): ICloudUtil {
    when (dbPathType) {
      DROPBOX -> return DropboxUtil
      WEBDAV -> return WebDavUtil
      ONE_DRIVE -> return OneDriveUtil
    }
    throw IllegalArgumentException("不识别的工具类型：${dbPathType}")
  }

}