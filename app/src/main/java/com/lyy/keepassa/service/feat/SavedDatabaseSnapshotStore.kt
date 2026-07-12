package com.lyy.keepassa.service.feat

import com.lyy.keepassa.util.cloud.UploadSnapshot
import com.lyy.keepassa.util.cloud.UploadSnapshotStore
import java.io.File
import java.util.UUID

internal data class SavedDatabaseSnapshot(
  val file: File,
  val size: Long,
  val sha256: String,
  val revision: Long
)

internal class SavedDatabaseSnapshotStore(root: File) {
  private val delegate = UploadSnapshotStore(root)

  fun create(source: File, revision: Long): SavedDatabaseSnapshot {
    val snapshot = delegate.create(source, "$revision-${UUID.randomUUID()}")
    return SavedDatabaseSnapshot(snapshot.file, snapshot.size, snapshot.sha256, revision)
  }

  fun delete(snapshot: SavedDatabaseSnapshot) {
    delegate.delete(UploadSnapshot(snapshot.file, snapshot.size, snapshot.sha256))
  }
}
