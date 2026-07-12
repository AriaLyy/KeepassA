package com.lyy.keepassa.util.cloud.interceptor

import android.content.Context
import android.content.ContextWrapper
import java.io.File
import java.io.IOException
import java.io.Closeable
import java.util.UUID
import com.keepassdroid.Database
import com.keepassdroid.database.PwDatabase

internal object CloudAttachmentDirectory {
  fun create(cacheDir: File): File {
    val directory = File(
      File(cacheDir, ROOT_DIRECTORY),
      UUID.randomUUID().toString()
    )
    if (!directory.mkdirs() && !directory.isDirectory) {
      throw IOException("Unable to create cloud attachment directory: ${directory.absolutePath}")
    }
    return directory
  }

  private const val ROOT_DIRECTORY = "cloud_merge_attachments"
}

internal class CloudDatabaseOpenContext private constructor(
  base: Context,
  private val cloudFilesDir: File
) : ContextWrapper(base) {

  override fun getFilesDir(): File = cloudFilesDir

  fun attachmentDirectory(): File = cloudFilesDir

  companion object {
    fun create(base: Context): CloudDatabaseOpenContext {
      return CloudDatabaseOpenContext(
        base = base,
        cloudFilesDir = CloudAttachmentDirectory.create(base.cacheDir)
      )
    }
  }
}

internal class OpenedCloudDatabase(
  private val owner: Database,
  private val context: CloudDatabaseOpenContext
) : Closeable {
  val database: PwDatabase get() = checkNotNull(owner.pm)

  override fun close() {
    runCatching { owner.clear(context) }
    context.attachmentDirectory().deleteRecursively()
  }
}
