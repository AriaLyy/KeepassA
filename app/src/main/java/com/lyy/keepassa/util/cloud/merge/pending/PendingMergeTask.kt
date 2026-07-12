package com.lyy.keepassa.util.cloud.merge.pending

data class PendingMergeTask(
  val id: String,
  val databaseIdentity: String,
  val localDatabaseUri: String,
  val cloudStorageType: String,
  val cloudDatabasePath: String,
  val cloudSnapshotPath: String,
  val cloudModifiedTime: Long?,
  val cloudContentHash: String?,
  val createdAt: Long,
  val state: PendingMergeState
) {
  fun hasSameTarget(other: PendingMergeTask): Boolean {
    return databaseIdentity == other.databaseIdentity &&
        cloudStorageType == other.cloudStorageType &&
        cloudDatabasePath == other.cloudDatabasePath
  }
}

enum class PendingMergeState {
  WAITING_FOR_UNLOCK,
  READY_TO_PRESENT,
  RESOLVING,
  DEFERRED_BY_USER
}
