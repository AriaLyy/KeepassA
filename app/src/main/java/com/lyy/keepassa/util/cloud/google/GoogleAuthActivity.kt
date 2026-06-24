/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.util.cloud.google

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.drive.DriveScopes
import com.lyy.keepassa.util.cloud.GoogleDriveUtil
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * @Author laoyuyu
 * @Description
 * @Date 2024/5/17
 **/
class GoogleAuthActivity : AppCompatActivity() {

  companion object {
    const val REQUEST_CODE_SIGN_IN = 1991
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    requestSignIn()
  }

  private fun requestSignIn() {
    Timber.d("Requesting sign-in")

    val signInOptions =
      Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestScopes(Scope(DriveScopes.DRIVE_APPDATA), Scope(DriveScopes.DRIVE_FILE))
        .build()
    val client: GoogleSignInClient = GoogleSignIn.getClient(this, signInOptions)

    // The result of the sign-in Intent is handled in onActivityResult.
    startActivityForResult(client.signInIntent, REQUEST_CODE_SIGN_IN)
  }

  /**
   * Handles the `result` of a completed sign-in activity initiated from [ ][.requestSignIn].
   */
  private fun handleSignInResult(result: Intent) {
    GoogleSignIn.getSignedInAccountFromIntent(result)
      .addOnSuccessListener { googleAccount: GoogleSignInAccount ->
        Timber.d("Signed in as " + googleAccount.email)
        // Use the authenticated account to sign in to the Drive service.
        val credential =
          GoogleAccountCredential.usingOAuth2(
            this, setOf(DriveScopes.DRIVE_APPDATA, DriveScopes.DRIVE_FILE)
          )
        credential.setSelectedAccount(googleAccount.account)

        lifecycleScope.launch {
          GoogleDriveUtil.GOOGLE_AUTH_FLOW.emit(credential)
        }
        finish()
      }
      .addOnCanceledListener {
        Timber.w("cancel sign")
        finish()
      }
      .addOnFailureListener { exception ->
        Timber.e("Unable to sign in.", exception)
        lifecycleScope.launch {
          GoogleDriveUtil.AUTH_STATE_FLOW.emit(false)
        }
        finish()
      }
  }

  @Deprecated("Deprecated in Java")
  override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
    super.onActivityResult(requestCode, resultCode, resultData)
    Timber.d("onActivityResult, requestCode: $resultCode, resultCode: $resultCode")
    if (requestCode == REQUEST_CODE_SIGN_IN && resultCode == RESULT_OK && resultData != null) {
      Timber.d("auth success")
      handleSignInResult(resultData)
      return
    }
    if (requestCode == RESULT_CANCELED){
      finish()
      return
    }
    lifecycleScope.launch {
      GoogleDriveUtil.AUTH_STATE_FLOW.emit(false)
    }
    finish()
  }
}