package com.lyy.keepassa.service.feat

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class KdbOpenAssertionContractTest {
  @Test
  fun malformedDatabaseAssertionIsHandledAtOpenBoundary() {
    val source = File("src/main/java/com/lyy/keepassa/service/feat/KdbOpenService.kt").readText()
    val method = source.substring(source.indexOf("private suspend fun openDbFile"))

    assertTrue(method.contains("catch (e: AssertionError)"))
  }
}
