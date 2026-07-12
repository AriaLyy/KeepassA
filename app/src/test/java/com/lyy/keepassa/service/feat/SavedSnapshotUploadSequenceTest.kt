package com.lyy.keepassa.service.feat

import com.lyy.keepassa.util.cloud.interceptor.DbSyncResponse
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SavedSnapshotUploadSequenceTest {
  @Test
  fun uploadsFrozenSnapshotAndDeletesItAfterSourceChanges() = runBlocking {
    val root = createTempDirectory("snapshot-upload").toFile()
    val source = File(root, "database.kdbx").apply { writeBytes(byteArrayOf(1, 2, 3)) }
    val store = SavedDatabaseSnapshotStore(File(root, "snapshots"))
    val snapshot = store.create(source, revision = 4)
    source.writeBytes(byteArrayOf(9, 9, 9))
    var uploaded = byteArrayOf()

    val response = SavedSnapshotUploadSequence.run(
      snapshot = snapshot,
      upload = { frozen ->
        uploaded = frozen.file.readBytes()
        DbSyncResponse(0, "ok")
      },
      cleanup = store::delete
    )

    assertEquals(listOf<Byte>(1, 2, 3), uploaded.toList())
    assertEquals(0, response.code)
    assertFalse(snapshot.file.exists())
  }

  @Test
  fun deletesSnapshotWhenUploadThrows() = runBlocking {
    val root = createTempDirectory("snapshot-upload-error").toFile()
    val source = File(root, "database.kdbx").apply { writeBytes(byteArrayOf(1)) }
    val store = SavedDatabaseSnapshotStore(File(root, "snapshots"))
    val snapshot = store.create(source, revision = 5)

    runCatching {
      SavedSnapshotUploadSequence.run(
        snapshot = snapshot,
        upload = { error("network failure") },
        cleanup = store::delete
      )
    }

    assertFalse(snapshot.file.exists())
  }
}
