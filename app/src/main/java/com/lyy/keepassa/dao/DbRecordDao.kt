/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.lyy.keepassa.entity.DbHistoryRecord

@Dao
interface DbRecordDao {

  @Query("SELECT * FROM DbHistoryRecord ORDER BY time DESC LIMIT 0, 1")
  suspend fun getLastRecord(): DbHistoryRecord

  @Query("SELECT * FROM DbHistoryRecord ORDER BY time DESC")
  suspend fun getAllRecord(): List<DbHistoryRecord>

  @Query("SELECT * FROM DbHistoryRecord WHERE localDbUri=:localDbUri")
  suspend fun findRecord(localDbUri: String): DbHistoryRecord?

  @Query("SELECT * FROM DbHistoryRecord WHERE cloudDiskPath=:cloudPath")
  suspend fun findRecordByCloudPath(cloudPath: String): DbHistoryRecord?

  @Insert
  suspend fun saveRecord(record: DbHistoryRecord)

  @Update
  suspend fun updateRecord(record: DbHistoryRecord)

  @Delete
  suspend fun deleteRecord(record: DbHistoryRecord)
}