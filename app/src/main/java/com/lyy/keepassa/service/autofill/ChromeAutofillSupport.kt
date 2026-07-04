/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.service.autofill

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import timber.log.Timber

enum class ChromeThirdPartyAutofillState {
  ENABLED,
  DISABLED,
  NOT_INSTALLED,
  UNKNOWN
}

object ChromeAutofillPromptPolicy {
  fun shouldPrompt(
    isKeepassAutofillEnabled: Boolean,
    chromeState: ChromeThirdPartyAutofillState,
    isInCooldown: Boolean,
    sdkInt: Int
  ): Boolean {
    if (sdkInt < android.os.Build.VERSION_CODES.O) return false
    if (!isKeepassAutofillEnabled) return false
    if (isInCooldown) return false
    return chromeState == ChromeThirdPartyAutofillState.DISABLED
  }
}

object ChromeAutofillSupport {
  const val CHROME_PACKAGE = "com.android.chrome"
  private const val PROVIDER_SUFFIX = ".AutofillThirdPartyModeContentProvider"
  private const val THIRD_PARTY_MODE_PATH = "autofill_third_party_mode"
  private const val THIRD_PARTY_MODE_COLUMN = "autofill_third_party_state"
  private const val OPTIONS_LAUNCHER_CLASS =
    "org.chromium.chrome.browser.AutofillOptionsLauncher"

  fun providerUriString(packageName: String = CHROME_PACKAGE): String {
    return "content://$packageName$PROVIDER_SUFFIX/$THIRD_PARTY_MODE_PATH"
  }

  fun stateFromProviderValue(value: Int): ChromeThirdPartyAutofillState {
    return when (value) {
      0 -> ChromeThirdPartyAutofillState.DISABLED
      1 -> ChromeThirdPartyAutofillState.ENABLED
      else -> ChromeThirdPartyAutofillState.UNKNOWN
    }
  }

  fun thirdPartyModeState(
    context: Context,
    packageName: String = CHROME_PACKAGE
  ): ChromeThirdPartyAutofillState {
    if (!isPackageInstalled(context, packageName)) {
      return ChromeThirdPartyAutofillState.NOT_INSTALLED
    }

    val uri = Uri.parse(providerUriString(packageName))
    return try {
      context.contentResolver.query(
        uri,
        arrayOf(THIRD_PARTY_MODE_COLUMN),
        null,
        null,
        null
      )?.use { cursor ->
        if (!cursor.moveToFirst()) return ChromeThirdPartyAutofillState.UNKNOWN
        val index = cursor.getColumnIndex(THIRD_PARTY_MODE_COLUMN)
        if (index < 0) return ChromeThirdPartyAutofillState.UNKNOWN
        stateFromProviderValue(cursor.getInt(index))
      } ?: ChromeThirdPartyAutofillState.UNKNOWN
    } catch (e: Throwable) {
      Timber.e(e, "query Chrome third-party autofill mode failed")
      ChromeThirdPartyAutofillState.UNKNOWN
    }
  }

  fun settingsIntent(packageName: String = CHROME_PACKAGE): Intent {
    return Intent(Intent.ACTION_APPLICATION_PREFERENCES).apply {
      addCategory(Intent.CATEGORY_APP_BROWSER)
      setClassName(packageName, OPTIONS_LAUNCHER_CLASS)
    }
  }

  fun openSettings(
    context: Context,
    packageName: String = CHROME_PACKAGE
  ): Boolean {
    val intent = settingsIntent(packageName)
    if (context !is Activity) {
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return runCatching {
      context.startActivity(intent)
      true
    }.getOrElse {
      Timber.e(it, "open Chrome autofill options failed")
      false
    }
  }

  @Suppress("DEPRECATION")
  private fun isPackageInstalled(context: Context, packageName: String): Boolean {
    return try {
      context.packageManager.getPackageInfo(packageName, 0)
      true
    } catch (e: PackageManager.NameNotFoundException) {
      false
    } catch (e: Throwable) {
      Timber.e(e, "check Chrome package failed")
      false
    }
  }
}
