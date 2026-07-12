package com.lyy.keepassa.service.feat

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class KdbHandlerMutationRevisionContractTest {
  @Test
  fun publicMutationEntrypointsMarkLocalRevision() {
    val source = File("src/main/java/com/lyy/keepassa/service/feat/KdbHandlerService.kt").readText()
    val methods = listOf(
      "fun collection(", "fun updateEntryStatus(", "fun moveEntry(", "fun deleteEntry(",
      "fun deleteGroup(", "fun modifyGroup(", "fun createGroup(", "fun createEntry(",
      "fun addEntryTo("
    )

    methods.forEachIndexed { index, marker ->
      val start = source.indexOf(marker)
      assertTrue("missing method marker $marker", start >= 0)
      val end = methods.drop(index + 1)
        .map { source.indexOf(it, start + marker.length) }
        .filter { it >= 0 }
        .minOrNull() ?: source.length
      assertTrue("$marker must call markLocalChange", source.substring(start, end).contains("markLocalChange()"))
    }
  }
}
