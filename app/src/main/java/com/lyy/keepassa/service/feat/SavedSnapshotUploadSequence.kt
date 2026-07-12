package com.lyy.keepassa.service.feat

import com.lyy.keepassa.util.cloud.interceptor.DbSyncResponse

internal object SavedSnapshotUploadSequence {
  suspend fun run(
    snapshot: SavedDatabaseSnapshot,
    upload: suspend (SavedDatabaseSnapshot) -> DbSyncResponse,
    cleanup: (SavedDatabaseSnapshot) -> Unit
  ): DbSyncResponse {
    return try {
      upload(snapshot)
    } finally {
      cleanup(snapshot)
    }
  }
}
