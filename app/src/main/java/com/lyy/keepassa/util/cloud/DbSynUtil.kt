/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.util.cloud

import android.content.Context
import android.net.Uri
import android.text.TextUtils
import com.arialyy.frame.util.SharePreUtil
import com.arialyy.frame.util.StringUtil
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.Constance
import com.lyy.keepassa.entity.DbHistoryRecord
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.util.cloud.interceptor.DbSyncCheckInterceptor
import com.lyy.keepassa.util.cloud.interceptor.DbSyncCompareInterceptor
import com.lyy.keepassa.util.cloud.interceptor.DbSyncRequest
import com.lyy.keepassa.util.cloud.interceptor.DbSyncResponse
import com.lyy.keepassa.util.cloud.interceptor.DbSyncUploadInterceptor
import com.lyy.keepassa.util.cloud.interceptor.IDbSyncInterceptor
import com.lyy.keepassa.view.StorageType
import com.lyy.keepassa.view.StorageType.AFS
import timber.log.Timber
import java.io.File
import java.util.Date

/**
 * 数据库同步工具
 */
object DbSynUtil : SynStateCode {

  private val TAG = StringUtil.getClassName(this)
  private const val KEY_SERVICE_MODIFY_TIME = "KEY_SERVICE_MODIFY_TIME"

  /**
   * 打开数据库时，记录的云盘时间，上传完成需要重新更新
   */
  var serviceModifyTime: Date = Date(System.currentTimeMillis())

  init {
    serviceModifyTime =
      Date(SharePreUtil.getLong(Constance.PRE_FILE_NAME, BaseApp.APP, KEY_SERVICE_MODIFY_TIME))
  }

  private val interceptors = arrayListOf<IDbSyncInterceptor>().apply {
    add(DbSyncCheckInterceptor())
    add(DbSyncCompareInterceptor())
    add(DbSyncUploadInterceptor())
  }

  /**
   * 或去该记录在云端的修改时间
   */
  suspend fun getFileServiceModifyTime(record: DbHistoryRecord): Date {
    return CloudUtilFactory.getCloudUtil(record.getDbPathType())
      .getFileServiceModifyTime(record.cloudDiskPath!!)
  }

  /**
   * 更新服务器端文件的修改时间
   */
  suspend fun updateServiceModifyTime(
    record: DbHistoryRecord
  ) {
    serviceModifyTime = CloudUtilFactory.getCloudUtil(record.getDbPathType())
      .getFileServiceModifyTime(record.cloudDiskPath!!)
    SharePreUtil.putLong(
      Constance.PRE_FILE_NAME,
      BaseApp.APP, KEY_SERVICE_MODIFY_TIME, serviceModifyTime.time
    )
    Timber.d(
      TAG, "更新云端文件修改时间：${KeepassAUtil.instance.formatTime(serviceModifyTime)}"
    )
  }

  /**
   * 从云端下载的文件缓存路径
   * @param cloudTypeName 云端网盘名
   */
  fun getCloudDbTempPath(
    cloudTypeName: String,
    dbName: String
  ): Uri {
    val file = File("${BaseApp.APP.cacheDir.path}/$cloudTypeName/${dbName}")
    if (file.parentFile != null && !file.parentFile!!.exists()) {
      file.parentFile!!.mkdirs()
    }
    return Uri.fromFile(file)
  }

  /**
   * 上传同步
   */
  suspend fun uploadSyn(record: DbHistoryRecord): DbSyncResponse {
    val storageType = record.getDbPathType()
    if (storageType == AFS) {
      return DbSyncResponse(STATE_SUCCEED, "")
    }
    val util = CloudUtilFactory.getCloudUtil(storageType)
    return interceptors[0].intercept(DbSyncRequest(record, util, interceptors))
  }

  /**
   * 只用于下载，如果文件存在，先会删除文件，再执行下载
   */
  suspend fun downloadOnly(
    context: Context,
    dbRecord: DbHistoryRecord,
    filePath: Uri
  ): String? {
    Timber.i("开始下载文件，云端路径：${dbRecord.cloudDiskPath}，文件保存路径：${filePath}")
    val util = CloudUtilFactory.getCloudUtil(StorageType.valueOf(dbRecord.type))
    val path = util.downloadFile(context, dbRecord, filePath)
    if (!TextUtils.isEmpty(path)) {
      updateServiceModifyTime(dbRecord)
    }
    return path
  }

  fun toask(
    msg: String,
    success: Boolean,
    des: String
  ) {
    BaseApp.handler.post {
      HitUtil.toaskShort(
        "$msg ${
          if (success) BaseApp.APP.getString(R.string.success) else BaseApp.APP.getString(
            R.string.fail
          )
        } $des"
      )
    }
  }
}