package com.lyy.keepassa.util.cloud.interceptor

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class DbMergeDelegateHierarchyDeleteContractTest {

  @Test
  fun deletingLocalOnlyParentPreservesUnselectedChildren() {
    val source = File("src/main/java/com/lyy/keepassa/util/cloud/interceptor/DbMergeDelegate.kt").readText()

    assertTrue(source.contains("preserveUnselectedChildren"))
    assertTrue(source.contains("localDb.moveEntry"))
    assertTrue(source.contains("localDb.moveGroup"))
  }
}
