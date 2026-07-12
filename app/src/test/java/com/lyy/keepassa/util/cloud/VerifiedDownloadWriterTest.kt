package com.lyy.keepassa.util.cloud

import java.io.ByteArrayInputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class VerifiedDownloadWriterTest {
  @get:Rule
  val tempFolder = TemporaryFolder()

  @Test
  fun shortDownloadIsRejectedWithoutReplacingExistingDatabase() {
    val target = tempFolder.newFile("database.kdbx").apply { writeBytes(byteArrayOf(1, 2, 3)) }
    val temp = tempFolder.root.resolve("database.kdbx.download")

    assertThrows(IncompleteDownloadException::class.java) {
      VerifiedDownloadWriter.write(ByteArrayInputStream(byteArrayOf(9, 8)), temp, expectedSize = 3)
    }

    assertArrayEquals(byteArrayOf(1, 2, 3), target.readBytes())
    assertFalse(temp.exists())
  }

  @Test
  fun completeDownloadProducesVerifiedTemporaryFile() {
    val temp = tempFolder.root.resolve("database.kdbx.download")

    VerifiedDownloadWriter.write(ByteArrayInputStream(byteArrayOf(9, 8, 7)), temp, expectedSize = 3)

    assertArrayEquals(byteArrayOf(9, 8, 7), temp.readBytes())
  }
}
