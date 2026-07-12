package com.lyy.keepassa.util.cloud.interceptor

import java.io.File
import java.net.URI

object DownloadedCloudFileResolver {
  fun resolve(value: String): File {
    val normalized = if (value.startsWith("/file:")) value.removePrefix("/file:") else value
    return if (normalized.startsWith("file:", ignoreCase = true)) {
      File(URI(normalized))
    } else {
      File(normalized)
    }
  }
}
