/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.view.setting

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import timber.log.Timber

data class CredentialManagerSettingsCandidate(
  val action: String,
  val isDirectCredentialProvider: Boolean
)

enum class CredentialManagerSettingsOpenResult {
  DIRECT,
  FALLBACK,
  FAILED
}

object CredentialManagerSettingsShortcut {
  private const val API_35 = 35
  const val CREDENTIAL_PROVIDER_SETTINGS_ACTION = "android.settings.CREDENTIAL_PROVIDER"

  fun candidateActions(
    sdkInt: Int = Build.VERSION.SDK_INT
  ): List<CredentialManagerSettingsCandidate> {
    val credentialProviderAction = if (sdkInt >= API_35) {
      Settings.ACTION_CREDENTIAL_PROVIDER
    } else {
      CREDENTIAL_PROVIDER_SETTINGS_ACTION
    }
    return listOf(
      CredentialManagerSettingsCandidate(
        action = credentialProviderAction,
        isDirectCredentialProvider = true
      ),
      CredentialManagerSettingsCandidate(
        action = Settings.ACTION_SECURITY_SETTINGS,
        isDirectCredentialProvider = false
      ),
      CredentialManagerSettingsCandidate(
        action = Settings.ACTION_SETTINGS,
        isDirectCredentialProvider = false
      )
    ).distinctBy { it.action }
  }

  @Suppress("DEPRECATION")
  fun open(context: Context): CredentialManagerSettingsOpenResult {
    for (candidate in candidateActions()) {
      val intent = Intent(candidate.action)
      if (context !is Activity) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      if (intent.resolveActivity(context.packageManager) == null) {
        continue
      }
      val opened = runCatching {
        context.startActivity(intent)
        true
      }.getOrElse {
        Timber.e(it, "open credential manager settings failed: %s", candidate.action)
        false
      }
      if (opened) {
        return if (candidate.isDirectCredentialProvider) {
          CredentialManagerSettingsOpenResult.DIRECT
        } else {
          CredentialManagerSettingsOpenResult.FALLBACK
        }
      }
    }
    return CredentialManagerSettingsOpenResult.FAILED
  }
}
