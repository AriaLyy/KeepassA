package com.lyy.keepassa.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.lyy.keepassa.entity.CloudServiceInfo

@Dao
interface CloudServiceInfoDao {

  @Update
  suspend fun update(serviceInfo: CloudServiceInfo)

  @Query("SELECT * FROM CloudServiceInfo WHERE cloudPath=:uri")
  suspend fun queryServiceInfo(uri: String): CloudServiceInfo?

  @Insert
  suspend fun saveServiceInfo(serviceInfo: CloudServiceInfo)

  @Update
  suspend fun updateServiceInfo(serviceInfo: CloudServiceInfo)
}