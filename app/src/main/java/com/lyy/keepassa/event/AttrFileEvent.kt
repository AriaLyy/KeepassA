package com.lyy.keepassa.event

import com.keepassdroid.database.security.ProtectedBinary
import com.lyy.keepassa.entity.CommonState

data class AttrFileEvent(
  val state: CommonState,
  val key: String,
  val file: ProtectedBinary,
)
