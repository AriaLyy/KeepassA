package com.lyy.keepassa.service.feat

import android.content.Context
import java.security.MessageDigest

internal class SharedPreferencesSyncRevisionRepository(context: Context) : SyncRevisionRepository {
  private val preferences = context.getSharedPreferences("database_sync_revision", Context.MODE_PRIVATE)

  override fun load(databaseId: String): SyncRevisionState {
    val key = key(databaseId)
    return SyncRevisionState(
      localRevision = preferences.getLong("${key}_local", 0),
      uploadedRevision = preferences.getLong("${key}_uploaded", 0)
    )
  }

  override fun save(databaseId: String, state: SyncRevisionState) {
    val key = key(databaseId)
    preferences.edit()
      .putLong("${key}_local", state.localRevision)
      .putLong("${key}_uploaded", state.uploadedRevision)
      .apply()
  }

  private fun key(databaseId: String): String {
    return MessageDigest.getInstance("SHA-256")
      .digest(databaseId.toByteArray())
      .joinToString("") { "%02x".format(it) }
  }
}
