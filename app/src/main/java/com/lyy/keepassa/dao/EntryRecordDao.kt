package com.lyy.keepassa.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.lyy.keepassa.entity.EntryRecord

@Dao
interface EntryRecordDao {

  @Query("SELECT COUNT(uid) FROM EntryRecord WHERE dbFileUri = :dbFileUri")
  suspend fun hasRecord(dbFileUri: String): Int

  @Query("SELECT * FROM EntryRecord WHERE uuid = :uuid AND dbFileUri = :dbFileUri")
  suspend fun getRecord(
    uuid: ByteArray,
    dbFileUri: String
  ): EntryRecord?

  /**
   * 只获取50条历史记录
   */
  @Query("SELECT * FROM EntryRecord WHERE dbFileUri = :dbFileUri ORDER BY time DESC LIMIT 50")
  suspend fun getRecord(dbFileUri: String): List<EntryRecord>

  @Insert
  suspend fun saveRecord(record: EntryRecord)

  @Update
  suspend fun updateRecord(record: EntryRecord)

  @Delete
  suspend fun delReocrd(record: EntryRecord)

}