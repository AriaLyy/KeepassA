/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.view.credential

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContract
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CreatePasswordResponse
import androidx.credentials.provider.PendingIntentHandler
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.entity.AutoFillParam
import com.lyy.keepassa.service.autofill.AutofillBrowserAuthContextStore
import com.lyy.keepassa.service.credential.CredentialSaveRequestMapper
import com.lyy.keepassa.view.create.entry.CreateEntryActivity
import com.lyy.keepassa.view.launcher.LauncherActivity

class CredentialSaveActivity : ComponentActivity() {

  private val createEntryLauncher = registerForActivityResult(
    object : ActivityResultContract<AutoFillParam, Boolean>() {
      override fun createIntent(context: Context, input: AutoFillParam): Intent {
        return Intent(context, CreateEntryActivity::class.java).apply {
          putExtra(LauncherActivity.KEY_AUTO_FILL_PARAM, input)
          putExtra(CreateEntryActivity.EXTRA_FINISH_WITH_RESULT, true)
        }
      }

      override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
        return resultCode == Activity.RESULT_OK
      }
    }
  ) { saved ->
    if (saved) {
      val result = Intent()
      PendingIntentHandler.setCreateCredentialResponse(result, CreatePasswordResponse())
      setResult(RESULT_OK, result)
    } else {
      setResult(RESULT_CANCELED)
    }
    finish()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    continueCreate()
  }

  private fun continueCreate() {
    val providerRequest = PendingIntentHandler.retrieveProviderCreateCredentialRequest(intent)
    val passwordRequest = providerRequest?.callingRequest as? CreatePasswordRequest
    if (
      providerRequest == null ||
      passwordRequest == null ||
      BaseApp.KDB == null ||
      BaseApp.isLocked
    ) {
      cancel()
      return
    }

    val packageName = providerRequest.callingAppInfo.packageName
    val browserDomain = AutofillBrowserAuthContextStore.find(packageName)?.domain
    val draft = CredentialSaveRequestMapper.from(
      packageName = packageName,
      origin = browserDomain?.let { "https://$it" },
      username = passwordRequest.id,
      password = passwordRequest.password
    )
    if (draft == null) {
      cancel()
      return
    }

    createEntryLauncher.launch(
      AutoFillParam(
        apkPkgName = draft.packageName,
        domain = draft.domain,
        isSave = true,
        saveUserName = draft.username,
        savePass = draft.password
      )
    )
  }

  private fun cancel() {
    setResult(RESULT_CANCELED)
    finish()
  }
}
