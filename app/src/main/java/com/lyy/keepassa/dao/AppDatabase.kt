package com.lyy.keepassa.dao

import androidx.room.Database
import androidx.room.RoomDatabase
import com.lyy.keepassa.entity.CloudServiceInfo
import com.lyy.keepassa.entity.DbRecord
import com.lyy.keepassa.entity.EntryRecord
import com.lyy.keepassa.entity.QuickUnLockRecord
import com.lyy.keepassa.entity.SearchRecord

@Database(
    entities = [DbRecord::class, EntryRecord::class,
      SearchRecord::class, CloudServiceInfo::class,
      QuickUnLockRecord::class
    ],
    version = 3
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