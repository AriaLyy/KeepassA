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
import com.lyy.keepassa.entity.SearchRecord

@Dao
interface SearchDao {

  @Insert
  suspend fun saveRecord(record: SearchRecord)

  @Query("SELECT * FROM SearchRecord WHERE title=:title")
  suspend fun getRecord(title: String): SearchRecord?

  @Query("SELECT * FROM SearchRecord ORDER BY time DESC LIMIT 5")
  suspend fun getSearchRecord(): List<SearchRecord>

  @Delete
  suspend fun delRecord(record: SearchRecord)

  @Update
  suspend fun updateRecord(record: SearchRecord)

}