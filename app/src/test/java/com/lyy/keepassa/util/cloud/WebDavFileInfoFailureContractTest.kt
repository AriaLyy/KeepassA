package com.lyy.keepassa.util.cloud

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebDavFileInfoFailureContractTest {
  @Test
  fun publicFileInfoQueryOnlyReturnsNullForMissingResources() {
    val source = File("src/main/java/com/lyy/keepassa/util/cloud/WebDavUtil.kt").readText()
    val publicMethod = source.substring(
      source.indexOf("override suspend fun getFileInfo(fileKey: String)"),
      source.indexOf("override suspend fun delFile(fileKey: String)")
    )

    assertTrue(publicMethod.contains("getStrictFileInfo"))
    assertFalse(publicMethod.contains("catch (e: Exception)"))
  }
}
