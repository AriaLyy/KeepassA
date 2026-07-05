/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.view.credential

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.credentials.GetCredentialResponse
import androidx.credentials.PasswordCredential
import androidx.credentials.provider.PendingIntentHandler
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.service.credential.CredentialPasswordRepository
import com.lyy.keepassa.service.credential.CredentialPasswordResultMapper
import com.lyy.keepassa.service.credential.CredentialProviderPendingIntents
import com.lyy.keepassa.util.getRealPass
import com.lyy.keepassa.util.getRealUserName

class CredentialGetActivity : Activity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    completeGet()
  }

  private fun completeGet() {
    val providerRequest = PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)
    if (providerRequest == null || BaseApp.KDB == null || BaseApp.isLocked) {
      cancel()
      return
    }

    val entryId = intent.getStringExtra(CredentialProviderPendingIntents.EXTRA_ENTRY_ID)
    val entry = entryId?.let { CredentialPasswordRepository.findById(it) }
    val passwordResult = CredentialPasswordResultMapper.from(
      username = entry?.getRealUserName(),
      password = entry?.getRealPass()
    )
    if (passwordResult == null) {
      cancel()
      return
    }

    val result = Intent()
    PendingIntentHandler.setGetCredentialResponse(
      result,
      GetCredentialResponse(
        PasswordCredential(
          id = passwordResult.username,
          password = passwordResult.password
        )
      )
    )
    setResult(RESULT_OK, result)
    finish()
  }

  private fun cancel() {
    setResult(RESULT_CANCELED)
    finish()
  }
}
