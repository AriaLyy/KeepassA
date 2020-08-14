package com.lyy.keepassa.event

import android.net.Uri
import com.lyy.keepassa.view.DbPathType

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
  var uriType: DbPathType = DbPathType.AFS, // uri类型，afs，google drive,
  var keyUri: Uri? = null
)