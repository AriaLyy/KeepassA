package com.lyy.keepassa.service.feat

import java.io.File
import kotlin.io.path.createTempDirectory
import org.junit.Assert.assertFalse
import org.junit.Test

class TemporaryValidationWorkspaceTest {
  @Test
  fun deletesWorkspaceWhenValidationThrows() {
    val root = createTempDirectory("validation-workspace").toFile()
    var workspace: File? = null

    runCatching {
      TemporaryValidationWorkspace(root).use { directory ->
        workspace = directory
        File(directory, "0").writeBytes(byteArrayOf(1, 2, 3))
        error("invalid database")
      }
    }

    assertFalse(workspace!!.exists())
  }
}
