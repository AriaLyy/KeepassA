package com.lyy.keepassa.util.cloud.interceptor

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DbMergeDelegateSaveContractTest {

  @Test
  fun mergedDatabaseIsAwaitedAndSaveFailureStopsSynchronization() {
    val source = File("src/main/java/com/lyy/keepassa/util/cloud/interceptor/DbMergeDelegate.kt").readText()

    assertFalse(source.contains("kdbHandlerService.saveDbByBackground()"))
    assertTrue(source.contains("val saveCode = KpaUtil.kdbHandlerService.saveDbAwait()"))
    assertTrue(source.contains("if (saveCode != DbSynUtil.STATE_SUCCEED)"))
    assertTrue(source.contains("return saveCode"))
  }
}
