package com.lyy.keepassa.util.cloud

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class WebDavHttpClientTimeoutContractTest {
  @Test
  fun webDavClientUsesLongUploadAndResponseTimeoutsAndEvictsTimedOutStreams() {
    val source = File("src/main/java/com/lyy/keepassa/util/cloud/WebDavUtil.kt").readText()

    assertTrue(source.contains("connectTimeout(30, TimeUnit.SECONDS)"))
    assertTrue(source.contains("readTimeout(120, TimeUnit.SECONDS)"))
    assertTrue(source.contains("writeTimeout(180, TimeUnit.SECONDS)"))
    assertTrue(source.contains("e is SocketTimeoutException"))
  }
}
