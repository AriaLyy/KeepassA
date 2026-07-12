package com.lyy.keepassa.service.feat

import java.io.File
import kotlin.io.path.createTempDirectory
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class SavedDatabaseSnapshotStoreTest {
  @Test
  fun snapshotKeepsSavedBytesAndRevisionWhenSourceChangesLater() {
    val root = createTempDirectory("saved-snapshot").toFile()
    val source = File(root, "database.kdbx").apply { writeBytes(byteArrayOf(1, 2, 3)) }
    val store = SavedDatabaseSnapshotStore(File(root, "snapshots"))

    val snapshot = store.create(source, revision = 7)
    source.writeBytes(byteArrayOf(9, 9, 9))

    assertEquals(7L, snapshot.revision)
    assertArrayEquals(byteArrayOf(1, 2, 3), snapshot.file.readBytes())
  }
}
