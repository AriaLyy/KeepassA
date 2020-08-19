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
import com.lyy.keepassa.entity.DbRecord

@Dao
interface DbRecordDao {

  @Query("SELECT * FROM DbRecord ORDER BY time DESC LIMIT 0, 1")
  suspend fun getLastRecord(): DbRecord

  @Query("SELECT * FROM DbRecord ORDER BY time DESC")
  suspend fun getAllRecord(): List<DbRecord>

  @Query("SELECT * FROM DbRecord WHERE localDbUri=:localDbUri")
  suspend fun findRecord(localDbUri: String): DbRecord?

  @Query("SELECT * FROM DbRecord WHERE cloudDiskPath=:cloudPath")
  suspend fun findRecordByCloudPath(cloudPath: String): DbRecord?

  @Insert
  suspend fun saveRecord(record: DbRecord)

  @Update
  suspend fun updateRecord(record: DbRecord)

  @Delete
  suspend fun deleteRecord(record: DbRecord)
}