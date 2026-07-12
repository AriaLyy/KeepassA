package com.lyy.keepassa.util.cloud.merge.pending

import org.junit.Assert.assertEquals
import org.junit.Test

class PendingMergeDatabaseIdentityTest {
  @Test
  fun uploadSnapshotRequestKeepsFormalDatabaseIdentity() {
    val formal = "file:///cache/WEBDAV/database.kdbx"
    val uploadSnapshot = "file:///cache/saved_database_snapshots/7/database.kdbx"

    assertEquals(formal, PendingMergeDatabaseIdentity.resolve(formal, uploadSnapshot))
  }
}
