package com.lyy.keepassa.util.cloud

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class UploadSnapshotStoreTest {
  @get:Rule
  val tempFolder = TemporaryFolder()

  @Test
  fun snapshotIsImmutableAndCarriesSizeAndSha256() {
    val source = tempFolder.newFile("database.kdbx").apply {
      writeBytes(byteArrayOf(1, 2, 3, 4))
    }
    val root = tempFolder.newFolder("snapshots")

    val snapshot = UploadSnapshotStore(root).create(source, "task-1")
    source.writeBytes(byteArrayOf(9, 9))

    assertArrayEquals(byteArrayOf(1, 2, 3, 4), snapshot.file.readBytes())
    assertEquals(4L, snapshot.size)
    assertEquals(
      "9f64a747e1b97f131fabb6b447296c9b6f0201e79fb3c5356e6c77e89b6a806a",
      snapshot.sha256
    )
    assertFalse(root.resolve("task-1/database.kdbx.tmp").exists())
  }
}
