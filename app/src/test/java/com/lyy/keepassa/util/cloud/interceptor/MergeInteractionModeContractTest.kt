package com.lyy.keepassa.util.cloud.interceptor

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class MergeInteractionModeContractTest {

  @Test
  fun foregroundAndBackgroundSavePropagateDifferentMergeInteractionModes() {
    val service = File("src/main/java/com/lyy/keepassa/service/feat/KdbHandlerService.kt").readText()
    val sync = File("src/main/java/com/lyy/keepassa/util/cloud/DbSynUtil.kt").readText()
    val request = File("src/main/java/com/lyy/keepassa/util/cloud/interceptor/DbSyncRequest.kt").readText()
    val compare = File("src/main/java/com/lyy/keepassa/util/cloud/interceptor/DbSyncCompareInterceptor.kt").readText()

    assertTrue(service.contains("MergeInteractionMode.BACKGROUND"))
    assertTrue(service.contains("MergeInteractionMode.FOREGROUND"))
    assertTrue(sync.contains("mergeInteractionMode: MergeInteractionMode"))
    assertTrue(request.contains("val mergeInteractionMode: MergeInteractionMode"))
    assertTrue(compare.contains("request.mergeInteractionMode"))
  }
}
