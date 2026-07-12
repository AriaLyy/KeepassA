package com.lyy.keepassa.service.feat

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KdbHandlerSaveUploadContractTest {

  @Test
  fun foregroundSaveUploadsRevisionBoundSnapshotWithoutHoldingOuterMutex() {
    val source = File("src/main/java/com/lyy/keepassa/service/feat/KdbHandlerService.kt").readText()
    val foreground = source
      .substringAfter("fun saveDbByForeground(")
      .substringBefore("override fun init(")

    assertTrue(source.contains("suspend fun saveDbAwait(): Int"))
    assertTrue(source.contains("saveDbTransactionAwait(createSnapshot = true)"))
    assertTrue(source.contains("record.copy(localDbUri = Uri.fromFile(frozen.file).toString())"))
    assertTrue(foreground.contains("saveAndUploadSnapshot("))
    assertFalse(foreground.contains("mutex.withLock"))
  }
}
