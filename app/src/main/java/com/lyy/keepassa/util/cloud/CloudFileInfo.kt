/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.util.cloud

import java.util.Date

/**
 * 云端文件信息
 */
data class CloudFileInfo(
  val fileKey: String,    // webDav/dropbox中为云端路径，onedrive为id
  val fileName: String,     // 文件名
  val serviceModifyDate: Date, // 该文件在云端的修改时间
  val size: Long,         // 文件大小
  val isDir: Boolean,
  val contentHash: String? = null,   // hash
)