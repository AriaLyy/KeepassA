package com.lyy.keepassa.util.cloud

import java.util.Date

/**
 * 云端文件信息
 */
data class CloudFileInfo(
  val cloudPath: String,    // 所在的云端路径
  val fileName: String,     // 文件名
  val serviceModifyDate: Date, // 该文件在云端的修改时间
  val size: Long,         // 文件大小
  val isDir: Boolean,
  val contentHash: String? = null   // hash
)