package com.lyy.keepassa.util.cloud

import java.io.File
import java.security.MessageDigest

internal class UploadSafetyFuse(private val root: File) {
  fun isTripped(cloudPath: String): Boolean = marker(cloudPath).isFile

  fun trip(cloudPath: String) {
    root.mkdirs()
    marker(cloudPath).writeText(cloudPath, Charsets.UTF_8)
  }

  fun clear(cloudPath: String) {
    marker(cloudPath).delete()
  }

  private fun marker(cloudPath: String): File {
    val digest = MessageDigest.getInstance("SHA-256")
      .digest(cloudPath.toByteArray(Charsets.UTF_8))
      .joinToString("") { "%02x".format(it) }
    return root.resolve("$digest.fuse")
  }
}
