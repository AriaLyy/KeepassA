package com.lyy.keepassa.util.cloud.interceptor

import android.content.Context
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.entity.DbHistoryRecord
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.cloud.SynStateCode
import timber.log.Timber

/**
 * @Author laoyuyu
 * @Description
 * @Date 5:08 下午 2021/12/24
 **/
class DbSyncUploadInterceptor(
  private val contextProvider: () -> Context = { BaseApp.APP },
  private val uploadFailureNotifier: (DbHistoryRecord) -> Unit = {
    HitUtil.toaskLong(BaseApp.APP.getString(R.string.merge_upload_failed_retry_next_time))
  }
) : IDbSyncInterceptor {
  private val stateCode = object : SynStateCode {}

  override suspend fun intercept(request: DbSyncRequest): DbSyncResponse {
    val util = request.syncUtil
    val record = request.record
    val b = util.uploadFile(contextProvider(), record)
    val msg = "上传文件${if (b) "成功" else "失败"}, fileKey = ${record.cloudDiskPath}"
    Timber.d(msg)
    if (!b) {
      uploadFailureNotifier(record)
    }
    return DbSyncResponse(if (b) stateCode.STATE_SUCCEED else stateCode.STATE_FAIL, msg)
  }
}
