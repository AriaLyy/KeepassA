package com.lyy.keepassa.service.feat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncRevisionStateTest {
  @Test
  fun uploadSuccessOnlyAcknowledgesTheRevisionThatWasUploaded() {
    var state = SyncRevisionState.initial()
    state = state.localChanged()
    val uploadingRevision = state.localRevision

    state = state.localChanged()
    state = state.uploadSucceeded(uploadingRevision)

    assertEquals(2L, state.localRevision)
    assertEquals(1L, state.uploadedRevision)
    assertTrue(state.needsUpload)
  }

  @Test
  fun latestRevisionUploadClearsPendingSyncWithoutChangingLocalRevision() {
    var state = SyncRevisionState.initial().localChanged()

    state = state.uploadSucceeded(state.localRevision)

    assertEquals(1L, state.localRevision)
    assertEquals(1L, state.uploadedRevision)
    assertFalse(state.needsUpload)
  }
}
