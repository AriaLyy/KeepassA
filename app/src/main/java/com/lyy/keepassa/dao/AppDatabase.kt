/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.dao

import androidx.room.Database
import androidx.room.RoomDatabase
import com.lyy.keepassa.entity.CloudServiceInfo
import com.lyy.keepassa.entity.DbHistoryRecord
import com.lyy.keepassa.entity.EntryRecord
import com.lyy.keepassa.entity.QuickUnLockRecord
import com.lyy.keepassa.entity.SearchRecord

@Database(
    entities = [DbHistoryRecord::class, EntryRecord::class,
      SearchRecord::class, CloudServiceInfo::class,
      QuickUnLockRecord::class
    ],
    version = 4
)
abstract class AppDatabase : RoomDatabase() {
  companion object {
    const val DB_NAME = "keepassA.db"
  }

  abstract fun cloudServiceInfoDao(): CloudServiceInfoDao

  abstract fun dbRecordDao(): DbRecordDao

  abstract fun entryRecordDao(): EntryRecordDao

  abstract fun searchRecordDao(): SearchDao

  abstract fun quickUnlockDao(): QuickUnlockDao
}