/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.view.credential

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContract
import androidx.credentials.GetCredentialResponse
import androidx.credentials.PasswordCredential
import androidx.credentials.provider.PendingIntentHandler
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.service.autofill.AutofillBrowserAuthContextStore
import com.lyy.keepassa.service.credential.CredentialBeginGetResponseFactory
import com.lyy.keepassa.service.credential.CredentialLookupTargetMapper
import com.lyy.keepassa.service.credential.CredentialPasswordRepository
import com.lyy.keepassa.service.credential.CredentialPasswordResultMapper
import com.lyy.keepassa.service.credential.CredentialProviderPendingIntents
import com.lyy.keepassa.service.credential.CredentialUnlockIntentPolicy
import com.lyy.keepassa.util.getRealPass
import com.lyy.keepassa.util.getRealUserName
import com.lyy.keepassa.util.isCanOpenQuickLock
import com.lyy.keepassa.view.launcher.LauncherActivity
import com.lyy.keepassa.view.main.QuickUnlockActivity

class CredentialGetActivity : ComponentActivity() {

  private val unlockLauncher = registerForActivityResult(
    object : ActivityResultContract<Unit, Boolean>() {
      override fun createIntent(context: Context, input: Unit): Intent {
        return if (
          CredentialUnlockIntentPolicy.shouldUseQuickUnlock(
            hasOpenDatabase = BaseApp.KDB != null,
            canOpenQuickUnlock = BaseApp.APP.isCanOpenQuickLock()
          )
        ) {
          QuickUnlockActivity.createQuickUnlockResultIntent(context)
        } else {
          LauncherActivity.createUnlockResultIntent(context)
        }
      }

      override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
        return resultCode == RESULT_OK
      }
    }
  ) { unlocked ->
    if (unlocked) {
      completeGet()
    } else {
      cancel()
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (BaseApp.KDB == null || BaseApp.isLocked) {
      unlockLauncher.launch(Unit)
      return
    }
    completeGet()
  }

  private fun completeGet() {
    if (intent.getStringExtra(CredentialProviderPendingIntents.EXTRA_ENTRY_ID) == null) {
      completeBeginGetAfterUnlock()
      return
    }
    completeSelectedGet()
  }

  private fun completeSelectedGet() {
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

  private fun completeBeginGetAfterUnlock() {
    val request = PendingIntentHandler.retrieveBeginGetCredentialRequest(intent)
    val packageName = request?.callingAppInfo?.packageName
    val browserDomain = packageName?.let { AutofillBrowserAuthContextStore.find(it)?.domain }
    val target = CredentialLookupTargetMapper.from(
      packageName = packageName,
      origin = browserDomain?.let { "https://$it" }
    )
    if (request == null || target == null || BaseApp.KDB == null || BaseApp.isLocked) {
      cancel()
      return
    }

    val response = CredentialBeginGetResponseFactory.createResponse(
      context = this,
      request = request,
      target = target
    )
    if (response.credentialEntries.isEmpty()) {
      cancel()
      return
    }
    val result = Intent()
    PendingIntentHandler.setBeginGetCredentialResponse(result, response)
    setResult(RESULT_OK, result)
    finish()
  }

  private fun cancel() {
    setResult(RESULT_CANCELED)
    finish()
  }
}
