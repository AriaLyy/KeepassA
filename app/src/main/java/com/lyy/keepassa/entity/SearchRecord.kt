package com.lyy.keepassa.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class SearchRecord {
  @PrimaryKey(autoGenerate = true) var uid: Int = 0

  @ColumnInfo var title: String = ""
  @ColumnInfo var time: Long = 0
}