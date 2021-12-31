package com.lyy.keepassa.util.cloud.interceptor

import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.util.cloud.DbSynUtil
import timber.log.Timber

/**
 * cloud file check interceptor
 * @Author laoyuyu
 * @Description
 * @Date 4:54 下午 2021/12/24
 **/
class DbSyncCheckInterceptor : IDbSyncInterceptor {

  override suspend fun intercept(request: DbSyncRequest): DbSyncResponse {
    if (BaseApp.isAFS()) {
      return normal(DbSynUtil.STATE_SUCCEED, "AFS 不需要上传")
    }
    val nextInterceptor = request.nextInterceptor() ?: throw IllegalArgumentException("没有上传一个拦截器")

    val util = request.syncUtil
    val record = request.record
    val cloudFileInfo = util.getFileInfo(record.cloudDiskPath!!)
    Timber.i("获取文件信息成功：${cloudFileInfo.toString()}")
    if (cloudFileInfo == null) {
      Timber.i("云端文件不存在，开始上传文件")
      return nextInterceptor.intercept(
        DbSyncRequest(
          record = record,
          syncUtil = util,
          interceptors = request.interceptors,
          index = request.index + 1
        )
      )
    }
    if (cloudFileInfo.contentHash != null
      && util.checkContentHash(cloudFileInfo.contentHash, record.getDbUri())
    ) {
      return normal(DbSynUtil.STATE_SUCCEED, "云端文件和本地文件的hash一致，忽略该上传")
    }
    Timber.i("云端文件存在，开始同步数据")

    return nextInterceptor.intercept(
      DbSyncRequest(
        record = record,
        syncUtil = util,
        interceptors = request.interceptors,
        index = request.index + 1
      )
    )
  }
}