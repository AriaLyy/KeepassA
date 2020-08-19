/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


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