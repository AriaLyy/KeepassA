package com.lyy.keepassa.util.cloud.interceptor

import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadedCloudFileResolverTest {

  @Test
  fun resolvesFileUriAndLegacySlashFilePrefixToFilesystemPath() {
    assertEquals(
      "/data/user/0/app/cache/cloud.kdbx",
      DownloadedCloudFileResolver.resolve("file:///data/user/0/app/cache/cloud.kdbx").path.replace('\\', '/')
    )
    assertEquals(
      "/data/user/0/app/cache/cloud.kdbx",
      DownloadedCloudFileResolver.resolve("/file:/data/user/0/app/cache/cloud.kdbx").path.replace('\\', '/')
    )
  }
}
