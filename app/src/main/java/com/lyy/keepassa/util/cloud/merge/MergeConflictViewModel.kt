package com.lyy.keepassa.util.cloud.merge

import androidx.lifecycle.ViewModel

class MergeConflictViewModel : ViewModel() {
  lateinit var session: MergeConflictSession
    private set

  val decisions = linkedMapOf<Int, MutableMap<FieldKey, Decision>>()
  val deleteLocalOnlyIndexes = linkedSetOf<Int>()
  var itemIndex = 0
  var completed = false

  fun bind(session: MergeConflictSession) {
    if (!::session.isInitialized) {
      this.session = session
    }
  }

  fun isBound(): Boolean = ::session.isInitialized
}
