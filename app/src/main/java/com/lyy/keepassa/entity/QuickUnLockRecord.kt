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
import java.io.Serializable

/**
 * 快速解锁信息实体
 */
@Entity
data class QuickUnLockRecord(
  @PrimaryKey(autoGenerate = true)
  val uid: Int = 0,
  val dbUri: String,
  var dbPass: String,
  var keyPath: String?,
  var isUseKey: Boolean = true,
  var isFullUnlock: Boolean = false,
  @ColumnInfo(name = "passIv", typeAffinity = ColumnInfo.BLOB)
  var passIv: ByteArray
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as QuickUnLockRecord

    if (uid != other.uid) return false
    if (dbUri != other.dbUri) return false
    if (dbPass != other.dbPass) return false
    if (keyPath != other.keyPath) return false
    if (isUseKey != other.isUseKey) return false
    if (isFullUnlock != other.isFullUnlock) return false
    if (!passIv.contentEquals(other.passIv)) return false

    return true
  }

  override fun hashCode(): Int {
    var result = uid
    result = 31 * result + dbUri.hashCode()
    result = 31 * result + dbPass.hashCode()
    result = 31 * result + (keyPath?.hashCode() ?: 0)
    result = 31 * result + isUseKey.hashCode()
    result = 31 * result + isFullUnlock.hashCode()
    result = 31 * result + passIv.contentHashCode()
    return result
  }

}