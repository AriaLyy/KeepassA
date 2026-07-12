package com.lyy.keepassa.util.cloud.merge.pending

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.IOException

interface PendingMergeRepository {
  fun save(task: PendingMergeTask)
  fun list(): List<PendingMergeTask>
  fun findForDatabase(databaseIdentity: String): PendingMergeTask?
  fun remove(taskId: String): Boolean
}

class FilePendingMergeRepository(
  directory: File,
  private val gson: Gson = Gson()
) : PendingMergeRepository {
  private val repositoryFile = File(directory, FILE_NAME)

  init {
    if (!directory.exists() && !directory.mkdirs()) {
      throw IOException("Unable to create pending merge directory: ${directory.absolutePath}")
    }
  }

  @Synchronized
  override fun save(task: PendingMergeTask) {
    val tasks = list().filterNot { it.hasSameTarget(task) }.toMutableList()
    tasks += task
    write(tasks)
  }

  @Synchronized
  override fun list(): List<PendingMergeTask> {
    if (!repositoryFile.isFile) {
      return emptyList()
    }
    return runCatching {
      val type = object : TypeToken<List<PendingMergeTask>>() {}.type
      gson.fromJson<List<PendingMergeTask>>(repositoryFile.readText(), type).orEmpty()
    }.getOrDefault(emptyList())
  }

  override fun findForDatabase(databaseIdentity: String): PendingMergeTask? {
    return list().firstOrNull { it.databaseIdentity == databaseIdentity }
  }

  @Synchronized
  override fun remove(taskId: String): Boolean {
    val tasks = list()
    val remaining = tasks.filterNot { it.id == taskId }
    if (tasks.size == remaining.size) {
      return false
    }
    write(remaining)
    return true
  }

  private fun write(tasks: List<PendingMergeTask>) {
    val temporary = File(repositoryFile.parentFile, "$FILE_NAME.tmp")
    temporary.writeText(gson.toJson(tasks))
    if (repositoryFile.exists() && !repositoryFile.delete()) {
      throw IOException("Unable to replace pending merge repository")
    }
    if (!temporary.renameTo(repositoryFile)) {
      throw IOException("Unable to commit pending merge repository")
    }
  }

  private companion object {
    const val FILE_NAME = "pending_merge_tasks.json"
  }
}
