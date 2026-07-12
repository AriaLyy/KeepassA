package com.lyy.keepassa.service.feat

import java.io.File
import kotlin.io.path.createTempDirectory
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import kotlinx.coroutines.CancellationException

class LocalDatabaseSaveGuardTest {
  @Test
  fun invalidSavedDatabase_restoresOriginalAndReturnsFailure() {
    val dir = createTempDirectory("save-guard").toFile()
    val database = File(dir, "database.kdbx").apply { writeBytes(byteArrayOf(1, 2, 3)) }
    var uploadAllowed = false

    val saved = LocalDatabaseSaveGuard().save(
      database = database,
      write = {
        database.writeBytes(byteArrayOf(9, 9))
        true
      },
      validate = { false }
    )
    uploadAllowed = saved

    assertFalse(saved)
    assertFalse(uploadAllowed)
    assertArrayEquals(byteArrayOf(1, 2, 3), database.readBytes())
  }

  @Test
  fun cancellationRestoresOriginalAndPropagates() {
    val dir = createTempDirectory("save-guard-cancel").toFile()
    val database = File(dir, "database.kdbx").apply { writeBytes(byteArrayOf(1, 2, 3)) }

    assertThrows(CancellationException::class.java) {
      LocalDatabaseSaveGuard().save(
        database = database,
        write = {
          database.writeBytes(byteArrayOf(9, 9))
          throw CancellationException("cancel save")
        },
        validate = { true }
      )
    }

    assertArrayEquals(byteArrayOf(1, 2, 3), database.readBytes())
    assertFalse(File(dir, "database.kdbx.save-backup").exists())
  }
}
