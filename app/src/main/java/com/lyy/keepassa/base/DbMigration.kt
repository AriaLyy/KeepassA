/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.base

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DbMigration {

  fun MIGRATION_2_3(): Migration {
    return object : Migration(2, 3) {
      override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE QuickUnLockRecord (uid INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, dbUri TEXT NOT NULL, dbPass TEXT NOT NULL, keyPath TEXT, isUseKey INTEGER NOT NULL, isFullUnlock INTEGER NOT NULL, passIv BLOB NOT NULL)")

      }
    }
  }

  fun MIGRATION_3_4(): Migration {
    return object : Migration(3, 4) {
      override fun migrate(database: SupportSQLiteDatabase) {
        // modify passIv can set null
        database.execSQL("CREATE TABLE QuickUnLockRecord_TEMP (uid INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, dbUri TEXT NOT NULL, dbPass TEXT NOT NULL, keyPath TEXT, isUseKey INTEGER NOT NULL, isUseFingerprint INTEGER NOT NULL, passIv BLOB)")
        database.execSQL("INSERT INTO QuickUnLockRecord_TEMP(uid, dbUri, dbPass, keyPath, isUseKey, isUseFingerprint, passIv) SELECT uid, dbUri, dbPass, keyPath, isUseKey, isFullUnlock, passIv FROM QuickUnLockRecord")
        database.execSQL("DROP TABLE QuickUnLockRecord")
        database.execSQL("ALTER TABLE QuickUnLockRecord_TEMP RENAME TO QuickUnLockRecord")
        // renameDbRecord
        database.execSQL("ALTER TABLE DbRecord RENAME TO DbHistoryRecord")
      }
    }
  }

}