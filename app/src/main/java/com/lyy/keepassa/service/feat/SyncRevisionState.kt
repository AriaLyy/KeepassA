package com.lyy.keepassa.service.feat

internal data class SyncRevisionState(
  val localRevision: Long,
  val uploadedRevision: Long
) {
  val needsUpload: Boolean get() = localRevision > uploadedRevision

  fun localChanged(): SyncRevisionState = copy(localRevision = localRevision + 1)

  fun uploadSucceeded(revision: Long): SyncRevisionState {
    return copy(uploadedRevision = maxOf(uploadedRevision, minOf(revision, localRevision)))
  }

  companion object {
    fun initial(): SyncRevisionState = SyncRevisionState(0, 0)
  }
}
