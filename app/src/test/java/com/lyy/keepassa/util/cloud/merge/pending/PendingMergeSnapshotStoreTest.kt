package com.lyy.keepassa.util.cloud.merge.pending

import java.nio.file.Files
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingMergeSnapshotStoreTest {

  @Test
  fun saveCopiesEncryptedDatabaseIntoPersistentTaskDirectory() {
    val filesDir = Files.createTempDirectory("pending-merge-files").toFile()
    val source = Files.createTempFile("cloud", ".kdbx").toFile().apply {
      writeBytes(byteArrayOf(1, 2, 3, 4))
    }

    val snapshot = PendingMergeSnapshotStore(filesDir).save("task-1", source)

    assertEquals(
      filesDir.resolve("pending_merge/task-1/cloud.kdbx").canonicalFile,
      snapshot.canonicalFile
    )
    assertArrayEquals(source.readBytes(), snapshot.readBytes())
  }

  @Test
  fun removeDeletesOnlyTheRequestedTaskDirectory() {
    val filesDir = Files.createTempDirectory("pending-merge-files").toFile()
    val source = Files.createTempFile("cloud", ".kdbx").toFile().apply { writeText("encrypted") }
    val store = PendingMergeSnapshotStore(filesDir)
    val first = store.save("task-1", source)
    val second = store.save("task-2", source)

    assertTrue(store.remove("task-1"))

    assertFalse(first.exists())
    assertTrue(second.exists())
  }
}
