package com.lyy.keepassa.util.cloud

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest

internal data class UploadSnapshot(
  val file: File,
  val size: Long,
  val sha256: String
)

internal class UploadSnapshotStore(private val root: File) {
  fun create(source: File, taskId: String): UploadSnapshot {
    require(source.isFile) { "Upload source does not exist: $source" }
    val taskDir = root.resolve(taskId)
    if (!taskDir.mkdirs() && !taskDir.isDirectory) {
      throw IOException("Cannot create upload snapshot directory: $taskDir")
    }
    val temp = taskDir.resolve("database.kdbx.tmp")
    val target = taskDir.resolve("database.kdbx")
    temp.delete()
    target.delete()
    val digest = MessageDigest.getInstance("SHA-256")
    try {
      FileInputStream(source).use { input ->
        FileOutputStream(temp).use { output ->
          val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
          while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            output.write(buffer, 0, count)
            digest.update(buffer, 0, count)
          }
          output.fd.sync()
        }
      }
      if (!temp.renameTo(target)) {
        throw IOException("Cannot commit upload snapshot: $target")
      }
      return UploadSnapshot(target, target.length(), digest.digest().toHex())
    } catch (error: Throwable) {
      temp.delete()
      target.delete()
      taskDir.delete()
      throw error
    }
  }

  fun delete(snapshot: UploadSnapshot) {
    val taskDir = snapshot.file.parentFile
    snapshot.file.delete()
    taskDir?.delete()
  }

  private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
