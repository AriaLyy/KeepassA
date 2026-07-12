package com.lyy.keepassa.service.feat

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackgroundEntrySaveContractTest {
  @Test
  fun leavingEntryEditorTriggersDeferredBackgroundUpload() {
    val source = File("src/main/java/com/lyy/keepassa/service/feat/KpaSdkService.kt").readText()
    val callback = source.substringAfter("override fun onBackground").substringBefore("XLogFeature.flush()")

    assertTrue(callback.contains("saveDbByBackground(true)"))
    assertFalse(callback.contains("CreateEntryActivity"))
    assertFalse(callback.contains("return"))
  }
}
