/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.ondrive

import androidx.annotation.Keep

/**
 * @Author laoyuyu
 * @Description
 * @Date 2021/2/7
 * https://docs.microsoft.com/zh-cn/graph/api/resources/driveitem?view=graph-rest-1.0
 **/
@Keep
data class MsalSourceItem(
  val id: String,
  val createdDateTime: String,
  val lastModifiedDateTime: String,
  val cTag: String,
  val eTag: String,
  val webUrl: String?, // 下载地址
  val name: String,
  val size: Long,
  val file: MsalFileInfo?,
  val folder: MsalFolderInfo?
) {
  fun isFolder() = folder != null
}

@Keep
data class MsalFolderInfo(
  val childCount: Long
)

@Keep
data class MsalFileInfo(
  val mimeType: String,
  val hashes: MsalFileHashes
)

@Keep
data class MsalFileHashes(
  val quickXorHash: String,
  val sha1Hash: String,
  val sha256Hash: String
)