package com.lyy.keepassa.util.cloud.merge.pending

import java.io.File
import java.io.IOException

class PendingMergeSnapshotStore(filesDir: File) {
  private val rootDirectory = File(filesDir, ROOT_DIRECTORY)

  fun save(taskId: String, source: File): File {
    require(source.isFile) { "Cloud database snapshot does not exist: ${source.absolutePath}" }
    val taskDirectory = File(rootDirectory, taskId)
    if (!taskDirectory.exists() && !taskDirectory.mkdirs()) {
      throw IOException("Unable to create pending merge snapshot directory")
    }
    val target = File(taskDirectory, SNAPSHOT_FILE_NAME)
    val temporary = File(taskDirectory, "$SNAPSHOT_FILE_NAME.tmp")
    source.inputStream().use { input ->
      temporary.outputStream().use { output -> input.copyTo(output) }
    }
    if (target.exists() && !target.delete()) {
      throw IOException("Unable to replace pending merge snapshot")
    }
    if (!temporary.renameTo(target)) {
      throw IOException("Unable to commit pending merge snapshot")
    }
    return target
  }

  fun remove(taskId: String): Boolean {
    val taskDirectory = File(rootDirectory, taskId)
    if (!taskDirectory.exists()) {
      return false
    }
    return taskDirectory.deleteRecursively()
  }

  companion object {
    const val ROOT_DIRECTORY = "pending_merge"
    const val SNAPSHOT_FILE_NAME = "cloud.kdbx"
  }
}
