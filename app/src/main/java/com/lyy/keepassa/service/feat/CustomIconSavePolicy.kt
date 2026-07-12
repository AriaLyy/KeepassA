package com.lyy.keepassa.service.feat

import com.keepassdroid.database.PwDatabaseV4
import com.keepassdroid.database.PwIconCustom

internal object CustomIconSavePolicy {
  fun isSerializable(icon: PwIconCustom?): Boolean {
    return icon != null &&
        icon.uuid != PwDatabaseV4.UUID_ZERO &&
        icon.imageData?.isNotEmpty() == true
  }
}
