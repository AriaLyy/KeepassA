/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.service.credential

import android.content.Context
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.BeginGetCredentialResponse
import androidx.credentials.provider.BeginGetPasswordOption
import androidx.credentials.provider.CredentialEntry
import androidx.credentials.provider.PasswordCredentialEntry

internal object CredentialBeginGetResponseFactory {

  fun hasPasswordOption(request: BeginGetCredentialRequest): Boolean {
    return request.beginGetCredentialOptions.any { it is BeginGetPasswordOption }
  }

  fun createResponse(
    context: Context,
    request: BeginGetCredentialRequest,
    target: CredentialLookupTarget
  ): BeginGetCredentialResponse {
    return BeginGetCredentialResponse(
      credentialEntries = createCredentialEntries(context, request, target)
    )
  }

  private fun createCredentialEntries(
    context: Context,
    request: BeginGetCredentialRequest,
    target: CredentialLookupTarget
  ): List<CredentialEntry> {
    val passwordOptions = request.beginGetCredentialOptions
      .filterIsInstance<BeginGetPasswordOption>()
    if (passwordOptions.isEmpty()) {
      return emptyList()
    }
    return CredentialPasswordRepository.find(target)
      .flatMap { entry ->
        passwordOptions
          .filter { option ->
            option.allowedUserIds.isEmpty() || option.allowedUserIds.contains(entry.username)
          }
          .map { option ->
            PasswordCredentialEntry(
              context = context,
              username = entry.username,
              pendingIntent = CredentialProviderPendingIntents.createGetPasswordPendingIntent(
                context = context,
                entryId = entry.uuid,
                target = target
              ),
              beginGetPasswordOption = option,
              displayName = entry.title
            ) as CredentialEntry
          }
      }
  }
}
