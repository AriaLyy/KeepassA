/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.service.credential

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.lyy.keepassa.view.credential.CredentialGetActivity
import com.lyy.keepassa.view.credential.CredentialSaveActivity
import java.util.UUID

internal object CredentialProviderPendingIntents {
  const val EXTRA_ENTRY_ID = "credential_entry_id"
  const val EXTRA_PACKAGE_NAME = "credential_package_name"
  const val EXTRA_DOMAIN = "credential_domain"

  private const val REQ_GET_PASSWORD = 4101
  private const val REQ_SAVE_PASSWORD = 4102
  private const val REQ_UNLOCK = 4103
  private const val PENDING_INTENT_FLAGS =
    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

  fun getPasswordExtras(
    entryId: UUID,
    target: CredentialLookupTarget
  ): Map<String, String> = buildMap {
    put(EXTRA_ENTRY_ID, entryId.toString())
    put(EXTRA_PACKAGE_NAME, target.packageName)
    target.domain?.let { put(EXTRA_DOMAIN, it) }
  }

  fun createGetPasswordPendingIntent(
    context: Context,
    entryId: UUID,
    target: CredentialLookupTarget
  ): PendingIntent {
    val intent = Intent(context, CredentialGetActivity::class.java)
    getPasswordExtras(entryId, target).forEach { (key, value) ->
      intent.putExtra(key, value)
    }
    return PendingIntent.getActivity(
      context,
      REQ_GET_PASSWORD + entryId.hashCode(),
      intent,
      PENDING_INTENT_FLAGS
    )
  }

  fun createSavePasswordPendingIntent(
    context: Context,
    target: CredentialLookupTarget
  ): PendingIntent {
    val intent = Intent(context, CredentialSaveActivity::class.java)
      .putExtra(EXTRA_PACKAGE_NAME, target.packageName)
    target.domain?.let { intent.putExtra(EXTRA_DOMAIN, it) }
    return PendingIntent.getActivity(
      context,
      REQ_SAVE_PASSWORD + target.packageName.hashCode(),
      intent,
      PENDING_INTENT_FLAGS
    )
  }

  fun createUnlockPendingIntent(context: Context): PendingIntent {
    return PendingIntent.getActivity(
      context,
      REQ_UNLOCK,
      Intent(context, CredentialGetActivity::class.java),
      PENDING_INTENT_FLAGS
    )
  }
}
