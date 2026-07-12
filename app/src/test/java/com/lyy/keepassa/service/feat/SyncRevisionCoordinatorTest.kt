package com.lyy.keepassa.service.feat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncRevisionCoordinatorTest {
  @Test
  fun successfulUploadDoesNotAcknowledgeChangesCreatedWhileUploadWasRunning() {
    val repository = InMemorySyncRevisionRepository()
    val coordinator = SyncRevisionCoordinator(repository)
    val databaseId = "database"
    coordinator.localChanged(databaseId)
    val uploadingRevision = coordinator.captureUploadRevision(databaseId)

    coordinator.localChanged(databaseId)
    coordinator.uploadSucceeded(databaseId, uploadingRevision)

    val state = repository.load(databaseId)
    assertEquals(2L, state.localRevision)
    assertEquals(1L, state.uploadedRevision)
    assertTrue(state.needsUpload)
  }
}

private class InMemorySyncRevisionRepository : SyncRevisionRepository {
  private val states = mutableMapOf<String, SyncRevisionState>()

  override fun load(databaseId: String): SyncRevisionState =
    states[databaseId] ?: SyncRevisionState.initial()

  override fun save(databaseId: String, state: SyncRevisionState) {
    states[databaseId] = state
  }
}
