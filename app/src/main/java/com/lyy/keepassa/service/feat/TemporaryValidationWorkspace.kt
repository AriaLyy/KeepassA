package com.lyy.keepassa.service.feat

import java.io.File
import java.util.UUID

internal class TemporaryValidationWorkspace(private val root: File) {
  inline fun <T> use(block: (File) -> T): T {
    val directory = File(root, UUID.randomUUID().toString())
    check(directory.mkdirs() || directory.isDirectory)
    return try {
      block(directory)
    } finally {
      directory.deleteRecursively()
    }
  }
}
