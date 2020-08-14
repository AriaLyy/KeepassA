package com.lyy.keepassa.event

import android.net.Uri

/**
 * 获取key的事件
 */
data class KeyPathEvent(
  val keyUri: Uri,
  val keyName: String
)