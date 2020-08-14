package com.lyy.keepassa.util.cloud

import com.lyy.keepassa.view.DbPathType
import com.lyy.keepassa.view.DbPathType.DROPBOX
import com.lyy.keepassa.view.DbPathType.WEBDAV

/**
 * 云端文件工具工厂
 */
object CloudUtilFactory {

  fun getCloudUtil(dbPathType: DbPathType): ICloudUtil {
    when (dbPathType) {
      DROPBOX -> return DropboxUtil
      WEBDAV -> return WebDavUtil
    }
    throw IllegalArgumentException("不识别的工具类型：${dbPathType}")
  }

}