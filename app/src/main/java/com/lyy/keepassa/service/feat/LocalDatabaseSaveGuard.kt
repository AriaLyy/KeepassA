package com.lyy.keepassa.service.feat

import java.io.File
import kotlinx.coroutines.CancellationException

internal class LocalDatabaseSaveGuard {
  fun save(database: File, write: () -> Boolean, validate: () -> Boolean): Boolean {
    val backup = File(database.parentFile, "${database.name}.save-backup")
    if (database.exists()) database.copyTo(backup, overwrite = true)
    return try {
      val valid = write() && validate()
      if (!valid && backup.exists()) backup.copyTo(database, overwrite = true)
      valid
    } catch (error: CancellationException) {
      if (backup.exists()) backup.copyTo(database, overwrite = true)
      throw error
    } catch (error: Exception) {
      if (backup.exists()) backup.copyTo(database, overwrite = true)
      false
    } finally {
      backup.delete()
    }
  }
}
