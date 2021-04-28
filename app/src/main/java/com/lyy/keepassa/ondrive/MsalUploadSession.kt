/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.ondrive

/**
 * @Author laoyuyu
 * @Description
 * @Date 2021/4/27
 * https://docs.microsoft.com/zh-cn/graph/api/resources/uploadsession?view=graph-rest-1.0
 **/
data class MsalUploadSession(
  val uploadUrl: String,   // 上传路径
  val expirationDateTime: String, // 以 UTC 表示的上载会话过期的日期和时间。在此过期时间之前必须上载完整的文件文件。
  val nextExpectedRanges: List<String> // range 0-
)