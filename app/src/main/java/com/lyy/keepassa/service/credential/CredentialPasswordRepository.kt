/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.service.credential

import com.keepassdroid.database.PwEntry
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.service.autofill.AutofillEntryLookup
import java.util.UUID

internal object CredentialPasswordRepository {

  fun find(target: CredentialLookupTarget): List<PwEntry> {
    return AutofillEntryLookup.find(target.packageName, target.domain).orEmpty()
  }

  fun findById(entryId: String): PwEntry? {
    val uuid = runCatching { UUID.fromString(entryId) }.getOrNull() ?: return null
    return BaseApp.KDB?.pm?.entries?.get(uuid)
  }
}
