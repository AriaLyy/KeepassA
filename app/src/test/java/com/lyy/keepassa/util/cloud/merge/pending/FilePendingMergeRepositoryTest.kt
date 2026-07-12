package com.lyy.keepassa.util.cloud.merge.pending

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FilePendingMergeRepositoryTest {

  @Test
  fun savedTaskCanBeReadByANewRepositoryInstance() {
    val directory = Files.createTempDirectory("pending-merge-repository").toFile()
    val task = task(id = "task-1", snapshot = "first.kdbx")

    FilePendingMergeRepository(directory).save(task)

    assertEquals(task, FilePendingMergeRepository(directory).findForDatabase("db-1"))
    assertNull(FilePendingMergeRepository(directory).findForDatabase("other-db"))
  }

  @Test
  fun savingSameDatabaseAndCloudPathReplacesExistingTask() {
    val directory = Files.createTempDirectory("pending-merge-repository").toFile()
    val repository = FilePendingMergeRepository(directory)
    repository.save(task(id = "task-1", snapshot = "first.kdbx"))

    val replacement = task(id = "task-2", snapshot = "second.kdbx")
    repository.save(replacement)

    assertEquals(listOf(replacement), FilePendingMergeRepository(directory).list())
  }

  private fun task(id: String, snapshot: String): PendingMergeTask {
    return PendingMergeTask(
      id = id,
      databaseIdentity = "db-1",
      localDatabaseUri = "content://local/db.kdbx",
      cloudStorageType = "WEBDAV",
      cloudDatabasePath = "/remote/db.kdbx",
      cloudSnapshotPath = snapshot,
      cloudModifiedTime = 10L,
      cloudContentHash = "hash-1",
      createdAt = 20L,
      state = PendingMergeState.WAITING_FOR_UNLOCK
    )
  }
}
