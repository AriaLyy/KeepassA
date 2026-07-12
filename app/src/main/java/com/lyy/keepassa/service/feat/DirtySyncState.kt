package com.lyy.keepassa.service.feat

internal object DirtySyncState {
  fun shouldClearAfterLocalSave(): Boolean = false

  fun shouldClearAfterUpload(uploadSucceeded: Boolean): Boolean = uploadSucceeded
}
