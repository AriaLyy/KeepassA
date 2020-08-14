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