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