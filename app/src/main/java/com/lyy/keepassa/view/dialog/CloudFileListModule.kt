package com.lyy.keepassa.view.dialog

import androidx.lifecycle.liveData
import com.lyy.keepassa.base.BaseModule
import com.lyy.keepassa.util.cloud.CloudFileInfo
import com.lyy.keepassa.util.cloud.CloudUtilFactory
import com.lyy.keepassa.view.DbPathType
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
  fun getCloudRootPath(dbPathType: DbPathType): String {
    return CloudUtilFactory.getCloudUtil(dbPathType)
        .getRootPath()
  }

  /**
   * 获取云文件指定路径的文件列表
   */
  fun getFileList(
    dbPathType: DbPathType,
    path: String
  ) = liveData {
    val temp = cache[path]
    if (temp != null && temp.isNotEmpty()) {
      emit(temp)
    } else {
      val data = withContext(Dispatchers.IO) {
        try {
          val utile = CloudUtilFactory.getCloudUtil(dbPathType)
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