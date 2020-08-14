package com.lyy.keepassa.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 云端服务器验证信息，所有字段都是加密的
 */
@Entity
class CloudServiceInfo(
  @PrimaryKey(autoGenerate = true) val uid: Int = 0,
  @ColumnInfo var userName: String? = null,
  @ColumnInfo var password: String? = null,
  // 云端路径
  @ColumnInfo val cloudPath: String
)