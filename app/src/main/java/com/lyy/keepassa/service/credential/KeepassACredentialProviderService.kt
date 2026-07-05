/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.service.credential

import android.os.CancellationSignal
import android.os.OutcomeReceiver
import androidx.annotation.RequiresApi
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.provider.BeginCreateCredentialRequest
import androidx.credentials.provider.BeginCreateCredentialResponse
import androidx.credentials.provider.BeginCreatePasswordCredentialRequest
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.BeginGetCredentialResponse
import androidx.credentials.provider.CredentialProviderService
import androidx.credentials.provider.CreateEntry
import androidx.credentials.provider.AuthenticationAction
import androidx.credentials.provider.ProviderClearCredentialStateRequest
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.service.autofill.AutofillBrowserAuthContextStore

@RequiresApi(34)
class KeepassACredentialProviderService : CredentialProviderService() {

  override fun onBeginGetCredentialRequest(
    request: BeginGetCredentialRequest,
    cancellationSignal: CancellationSignal,
    callback: OutcomeReceiver<BeginGetCredentialResponse, GetCredentialException>
  ) {
    val packageName = request.callingAppInfo?.packageName
    val target = targetFrom(packageName)
    if (target == null || cancellationSignal.isCanceled) {
      callback.onResult(BeginGetCredentialResponse())
      return
    }

    if (!CredentialBeginGetResponseFactory.hasPasswordOption(request)) {
      callback.onResult(BeginGetCredentialResponse())
      return
    }

    if (BaseApp.KDB == null || BaseApp.isLocked) {
      callback.onResult(
        BeginGetCredentialResponse(
          authenticationActions = listOf(
            AuthenticationAction(
              title = getString(R.string.credential_provider_unlock_title),
              pendingIntent = CredentialProviderPendingIntents.createUnlockPendingIntent(this)
            )
          )
        )
      )
      return
    }

    callback.onResult(
      CredentialBeginGetResponseFactory.createResponse(
        context = this,
        request = request,
        target = target
      )
    )
  }

  override fun onBeginCreateCredentialRequest(
    request: BeginCreateCredentialRequest,
    cancellationSignal: CancellationSignal,
    callback: OutcomeReceiver<BeginCreateCredentialResponse, CreateCredentialException>
  ) {
    val packageName = request.callingAppInfo?.packageName
    val target = targetFrom(packageName)
    if (
      target == null ||
      request !is BeginCreatePasswordCredentialRequest ||
      cancellationSignal.isCanceled
    ) {
      callback.onResult(BeginCreateCredentialResponse())
      return
    }

    val saveEntry = CreateEntry(
      accountName = getString(R.string.app_name),
      pendingIntent = CredentialProviderPendingIntents.createSavePasswordPendingIntent(
        context = this,
        target = target
      ),
      description = getString(R.string.credential_provider_save_title)
    )
    callback.onResult(BeginCreateCredentialResponse(createEntries = listOf(saveEntry)))
  }

  override fun onClearCredentialStateRequest(
    request: ProviderClearCredentialStateRequest,
    cancellationSignal: CancellationSignal,
    callback: OutcomeReceiver<Void?, ClearCredentialException>
  ) {
    callback.onResult(null)
  }

  private fun targetFrom(packageName: String?): CredentialLookupTarget? {
    val browserDomain = packageName?.let { AutofillBrowserAuthContextStore.find(it)?.domain }
    return CredentialLookupTargetMapper.from(
      packageName = packageName,
      origin = browserDomain?.let { "https://$it" }
    )
  }
}
