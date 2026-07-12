package com.lyy.keepassa.util.cloud.merge.pending

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PendingMergeTaskStoreTest {

  @Test
  fun createPersistsSnapshotBeforeTaskMetadata() {
    val filesDir = Files.createTempDirectory("pending-merge-store").toFile()
    val source = Files.createTempFile("cloud", ".kdbx").toFile().apply { writeText("encrypted") }
    val repository = RecordingRepository()
    val store = PendingMergeTaskStore(repository, PendingMergeSnapshotStore(filesDir))

    val task = store.create(draft(), source)

    assertEquals(listOf("save:${task.cloudSnapshotPath}"), repository.events)
    assertEquals("encrypted", File(task.cloudSnapshotPath).readText())
  }

  @Test
  fun createRemovesSnapshotWhenMetadataPersistenceFails() {
    val filesDir = Files.createTempDirectory("pending-merge-store").toFile()
    val source = Files.createTempFile("cloud", ".kdbx").toFile().apply { writeText("encrypted") }
    val repository = RecordingRepository(failOnSave = true)
    val store = PendingMergeTaskStore(repository, PendingMergeSnapshotStore(filesDir))

    runCatching { store.create(draft(), source) }

    assertFalse(filesDir.resolve("pending_merge/task-1").exists())
  }

  private fun draft() = PendingMergeTaskDraft(
    id = "task-1",
    databaseIdentity = "db-1",
    localDatabaseUri = "content://local/db.kdbx",
    cloudStorageType = "WEBDAV",
    cloudDatabasePath = "/remote/db.kdbx",
    cloudModifiedTime = 10L,
    cloudContentHash = "hash-1",
    createdAt = 20L,
    state = PendingMergeState.WAITING_FOR_UNLOCK
  )

  private class RecordingRepository(
    private val failOnSave: Boolean = false
  ) : PendingMergeRepository {
    val events = mutableListOf<String>()
    override fun save(task: PendingMergeTask) {
      if (failOnSave) error("metadata failed")
      events += "save:${task.cloudSnapshotPath}"
    }
    override fun list() = emptyList<PendingMergeTask>()
    override fun findForDatabase(databaseIdentity: String) = null
    override fun remove(taskId: String) = false
  }
}
