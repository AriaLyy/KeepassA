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
import com.lyy.keepassa.entity.QuickUnLockRecord

@Dao
interface QuickUnlockDao {

  @Query("SELECT * FROM QuickUnLockRecord WHERE dbUri=:dbUri")
  suspend fun findRecord(dbUri: String): QuickUnLockRecord?

  @Query("SELECT * FROM QuickUnLockRecord")
  suspend fun getAllRecord(): List<QuickUnLockRecord>?

  @Insert
  suspend fun saveRecord(record: QuickUnLockRecord)

  @Update
  suspend fun updateRecord(record: QuickUnLockRecord)

  @Delete
  suspend fun deleteRecord(record: QuickUnLockRecord)
}