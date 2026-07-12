package com.lyy.keepassa.view.create.entry

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModifyEntryHandlerSyncContractTest {
  @Test
  fun toolbarSavePersistsLocallyAndDefersCloudSyncUntilBackground() {
    val source = File("src/main/java/com/lyy/keepassa/view/create/entry/ModifyEntryHandler.kt").readText()
    val saveMethod = source.substringAfter("override fun saveDb").substringBeforeLast("\n}")

    assertTrue(saveMethod.contains("saveDbByForeground("))
    assertTrue(saveMethod.contains("uploadDb = false"))
    assertFalse(saveMethod.contains("uploadDb = true"))
    assertFalse(saveMethod.contains("saveOnly(true)"))
  }
}
