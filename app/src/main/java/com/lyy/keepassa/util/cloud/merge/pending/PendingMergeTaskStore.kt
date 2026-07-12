package com.lyy.keepassa.util.cloud.merge.pending

import java.io.File

data class PendingMergeTaskDraft(
  val id: String,
  val databaseIdentity: String,
  val localDatabaseUri: String,
  val cloudStorageType: String,
  val cloudDatabasePath: String,
  val cloudModifiedTime: Long?,
  val cloudContentHash: String?,
  val createdAt: Long,
  val state: PendingMergeState
)

class PendingMergeTaskStore(
  private val repository: PendingMergeRepository,
  private val snapshots: PendingMergeSnapshotStore
) {
  fun create(draft: PendingMergeTaskDraft, cloudDatabase: File): PendingMergeTask {
    val snapshot = snapshots.save(draft.id, cloudDatabase)
    val task = PendingMergeTask(
      id = draft.id,
      databaseIdentity = draft.databaseIdentity,
      localDatabaseUri = draft.localDatabaseUri,
      cloudStorageType = draft.cloudStorageType,
      cloudDatabasePath = draft.cloudDatabasePath,
      cloudSnapshotPath = snapshot.absolutePath,
      cloudModifiedTime = draft.cloudModifiedTime,
      cloudContentHash = draft.cloudContentHash,
      createdAt = draft.createdAt,
      state = draft.state
    )
    try {
      repository.save(task)
    } catch (error: Throwable) {
      snapshots.remove(draft.id)
      throw error
    }
    return task
  }
}
