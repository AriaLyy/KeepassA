package com.lyy.keepassa.util.cloud.merge.pending

internal object PendingMergeDatabaseIdentity {
  fun resolve(activeDatabaseUri: String?, requestLocalUri: String): String {
    return activeDatabaseUri?.takeIf { it.isNotBlank() } ?: requestLocalUri
  }
}
