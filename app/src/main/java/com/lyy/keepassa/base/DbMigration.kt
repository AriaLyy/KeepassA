package com.lyy.keepassa.base

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DbMigration {

  fun MIGRATION_2_3(): Migration {
    return object : Migration(2, 3) {
      override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE QuickUnLockRecord (uid INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
            "dbUri TEXT NOT NULL, dbPass TEXT NOT NULL, " +
            "keyPath TEXT, isUseKey INTEGER NOT NULL, isFullUnlock INTEGER NOT NULL, passIv BLOB NOT NULL)")
      }
    }
  }

}