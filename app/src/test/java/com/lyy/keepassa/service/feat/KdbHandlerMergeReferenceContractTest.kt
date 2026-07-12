package com.lyy.keepassa.service.feat

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class KdbHandlerMergeReferenceContractTest {

  @Test
  fun mutationEntrypointsRegisterEntryHistoryBinariesAndCustomIcons() {
    val source = File("src/main/java/com/lyy/keepassa/service/feat/KdbHandlerService.kt").readText()

    assertTrue(source.contains("registerEntryReferences(entry, target)"))
    assertTrue(source.contains("entry.binaries.values.forEach(database.binPool::poolAdd)"))
    assertTrue(source.contains("entry.history"))
    assertTrue(source.contains("database.customIcons"))
    assertTrue(!source.contains("registerV4MergeReferences"))
  }
}
