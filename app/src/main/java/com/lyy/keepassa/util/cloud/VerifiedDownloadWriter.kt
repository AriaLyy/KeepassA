package com.lyy.keepassa.util.cloud

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

internal class IncompleteDownloadException(message: String) : IOException(message)

internal object VerifiedDownloadWriter {
  fun write(input: InputStream, tempFile: File, expectedSize: Long) {
    require(expectedSize >= 0) { "Expected download size must not be negative" }
    tempFile.parentFile?.mkdirs()
    tempFile.delete()
    try {
      FileOutputStream(tempFile).use { output ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (total < expectedSize) {
          val count = input.read(buffer, 0, minOf(buffer.size.toLong(), expectedSize - total).toInt())
          if (count < 0) {
            throw IncompleteDownloadException(
              "Download ended early, expected=$expectedSize, actual=$total"
            )
          }
          output.write(buffer, 0, count)
          total += count
        }
        if (input.read() != -1) {
          throw IncompleteDownloadException("Download exceeds expected size=$expectedSize")
        }
        output.fd.sync()
      }
    } catch (error: Throwable) {
      tempFile.delete()
      throw error
    }
  }
}
