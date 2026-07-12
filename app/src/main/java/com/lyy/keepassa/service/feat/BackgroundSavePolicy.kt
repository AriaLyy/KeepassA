package com.lyy.keepassa.service.feat

internal object BackgroundSavePolicy {
  fun shouldSave(hasDirtyGroups: Boolean): Boolean = hasDirtyGroups
}
