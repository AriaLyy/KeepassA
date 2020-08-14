package com.lyy.keepassa.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class EntryRecord(
  @PrimaryKey(autoGenerate = true) val uid: Int = 0,
    // 数据库的本地文件uri
  var dbFileUri: String,
  @ColumnInfo var userName: String,
  @ColumnInfo var title: String,
  @ColumnInfo val uuid: ByteArray,
  @ColumnInfo var time: Long
)