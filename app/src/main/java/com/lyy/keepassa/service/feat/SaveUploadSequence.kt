package com.lyy.keepassa.service.feat

import com.lyy.keepassa.util.cloud.SynStateCode
import com.lyy.keepassa.util.cloud.interceptor.DbSyncResponse

internal object SaveUploadSequence {
  private val stateCode = object : SynStateCode {}

  suspend fun run(
    save: suspend () -> Int,
    upload: suspend () -> DbSyncResponse
  ): DbSyncResponse {
    val saveCode = save()
    if (saveCode != stateCode.STATE_SUCCEED) {
      return DbSyncResponse(saveCode, "save database failed")
    }
    return upload()
  }
}
