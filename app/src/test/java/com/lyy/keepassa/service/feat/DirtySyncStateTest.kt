package com.lyy.keepassa.service.feat

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DirtySyncStateTest {
  @Test
  fun localSaveSuccess_doesNotClearPendingCloudChanges() {
    assertFalse(DirtySyncState.shouldClearAfterLocalSave())
  }

  @Test
  fun onlySuccessfulUpload_clearsPendingCloudChanges() {
    assertTrue(DirtySyncState.shouldClearAfterUpload(uploadSucceeded = true))
    assertFalse(DirtySyncState.shouldClearAfterUpload(uploadSucceeded = false))
  }
}
