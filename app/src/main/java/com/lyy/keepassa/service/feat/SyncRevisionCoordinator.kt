package com.lyy.keepassa.service.feat

internal interface SyncRevisionRepository {
  fun load(databaseId: String): SyncRevisionState
  fun save(databaseId: String, state: SyncRevisionState)
}

internal class SyncRevisionCoordinator(
  private val repository: SyncRevisionRepository
) {
  fun localChanged(databaseId: String): Long {
    val state = repository.load(databaseId).localChanged()
    repository.save(databaseId, state)
    return state.localRevision
  }

  fun captureUploadRevision(databaseId: String): Long = repository.load(databaseId).localRevision

  fun uploadSucceeded(databaseId: String, revision: Long) {
    repository.save(databaseId, repository.load(databaseId).uploadSucceeded(revision))
  }

  fun needsUpload(databaseId: String): Boolean = repository.load(databaseId).needsUpload
}
