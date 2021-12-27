package com.lyy.keepassa.util.cloud.interceptor

import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.util.cloud.DbSynUtil
import timber.log.Timber

/**
 * @Author laoyuyu
 * @Description
 * @Date 5:08 下午 2021/12/24
 **/
class DbSyncUploadInterceptor : IDbSyncInterceptor {
  override suspend fun intercept(request: DbSyncRequest): DbSyncResponse {
    val util = request.syncUtil
    val record = request.record
    val b = util.uploadFile(BaseApp.APP, record)
    val msg = "上传文件${if (b) "成功" else "失败"}, fileKey = ${record.cloudDiskPath}"
    Timber.d(msg)
    return DbSyncResponse(if (b) DbSynUtil.STATE_SUCCEED else DbSynUtil.STATE_FAIL, msg)
  }
}