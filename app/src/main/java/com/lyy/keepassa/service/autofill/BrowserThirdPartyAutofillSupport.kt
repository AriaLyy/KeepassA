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
import android.os.Build
import com.lyy.keepassa.base.KeyConstance
import timber.log.Timber

enum class BrowserThirdPartyAutofillState {
  ENABLED,
  DISABLED,
  NOT_INSTALLED,
  UNKNOWN
}

data class BrowserThirdPartyAutofillIntegration(
  val packageName: String,
  val displayName: String,
  val cooldownStorageKey: String,
  val settingsActivityClassName: String = BrowserThirdPartyAutofillSupport.OPTIONS_LAUNCHER_CLASS
) {
  val providerAuthority: String =
    "$packageName${BrowserThirdPartyAutofillSupport.PROVIDER_SUFFIX}"

  val settingsPreferenceKey: String =
    if (packageName == BrowserThirdPartyAutofillSupport.CHROME_PACKAGE) {
      BrowserThirdPartyAutofillSupport.CHROME_SETTINGS_PREFERENCE_KEY
    } else {
      "${BrowserThirdPartyAutofillSupport.BROWSER_SETTINGS_PREFERENCE_KEY_PREFIX}$packageName"
    }
}

object BrowserThirdPartyAutofillPromptPolicy {
  fun shouldPrompt(
    isKeepassAutofillEnabled: Boolean,
    browserState: BrowserThirdPartyAutofillState,
    isInCooldown: Boolean,
    sdkInt: Int
  ): Boolean {
    if (sdkInt < Build.VERSION_CODES.O) return false
    if (!isKeepassAutofillEnabled) return false
    if (isInCooldown) return false
    return browserState == BrowserThirdPartyAutofillState.DISABLED
  }
}

object BrowserThirdPartyAutofillSupport {
  const val CHROME_PACKAGE = "com.android.chrome"
  const val PROVIDER_SUFFIX = ".AutofillThirdPartyModeContentProvider"
  const val OPTIONS_LAUNCHER_CLASS =
    "org.chromium.chrome.browser.AutofillOptionsLauncher"
  const val CHROME_SETTINGS_PREFERENCE_KEY = "set_key_browser_autofill_settings"
  const val BROWSER_SETTINGS_PREFERENCE_KEY_PREFIX =
    "set_key_browser_autofill_settings_"

  private const val THIRD_PARTY_MODE_PATH = "autofill_third_party_mode"
  private const val THIRD_PARTY_MODE_COLUMN = "autofill_third_party_state"

  val integrations: List<BrowserThirdPartyAutofillIntegration> = listOf(
    BrowserThirdPartyAutofillIntegration(
      packageName = CHROME_PACKAGE,
      displayName = "Chrome",
      cooldownStorageKey = KeyConstance.KEY_CHROME_AUTOFILL_PERMISSION_REJECTED_AT
    ),
    BrowserThirdPartyAutofillIntegration(
      packageName = "org.adblockplus.browser",
      displayName = "Adblock Browser",
      cooldownStorageKey =
      "${KeyConstance.KEY_BROWSER_THIRD_PARTY_AUTOFILL_PERMISSION_REJECTED_AT_PREFIX}org.adblockplus.browser"
    ),
    BrowserThirdPartyAutofillIntegration(
      packageName = "com.hsv.freeadblockerbrowser",
      displayName = "Free Adblocker Browser",
      cooldownStorageKey =
      "${KeyConstance.KEY_BROWSER_THIRD_PARTY_AUTOFILL_PERMISSION_REJECTED_AT_PREFIX}com.hsv.freeadblockerbrowser"
    ),
    BrowserThirdPartyAutofillIntegration(
      packageName = "com.vivaldi.browser",
      displayName = "Vivaldi",
      cooldownStorageKey =
      "${KeyConstance.KEY_BROWSER_THIRD_PARTY_AUTOFILL_PERMISSION_REJECTED_AT_PREFIX}com.vivaldi.browser"
    )
  )

  fun providerUriString(packageName: String): String {
    return "content://$packageName$PROVIDER_SUFFIX/$THIRD_PARTY_MODE_PATH"
  }

  fun stateFromProviderValue(value: Int): BrowserThirdPartyAutofillState {
    return when (value) {
      0 -> BrowserThirdPartyAutofillState.DISABLED
      1 -> BrowserThirdPartyAutofillState.ENABLED
      else -> BrowserThirdPartyAutofillState.UNKNOWN
    }
  }

  fun thirdPartyModeState(
    context: Context,
    integration: BrowserThirdPartyAutofillIntegration
  ): BrowserThirdPartyAutofillState {
    return thirdPartyModeState(context, integration.packageName)
  }

  fun thirdPartyModeState(
    context: Context,
    packageName: String
  ): BrowserThirdPartyAutofillState {
    if (!isPackageInstalled(context, packageName)) {
      return BrowserThirdPartyAutofillState.NOT_INSTALLED
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
        if (!cursor.moveToFirst()) return BrowserThirdPartyAutofillState.UNKNOWN
        val index = cursor.getColumnIndex(THIRD_PARTY_MODE_COLUMN)
        if (index < 0) return BrowserThirdPartyAutofillState.UNKNOWN
        stateFromProviderValue(cursor.getInt(index))
      } ?: BrowserThirdPartyAutofillState.UNKNOWN
    } catch (e: Throwable) {
      Timber.e(e, "query browser third-party autofill mode failed: %s", packageName)
      BrowserThirdPartyAutofillState.UNKNOWN
    }
  }

  fun settingsIntent(integration: BrowserThirdPartyAutofillIntegration): Intent {
    return settingsIntent(integration.packageName, integration.settingsActivityClassName)
  }

  fun settingsIntent(
    packageName: String,
    settingsActivityClassName: String = OPTIONS_LAUNCHER_CLASS
  ): Intent {
    return Intent(Intent.ACTION_APPLICATION_PREFERENCES).apply {
      addCategory(Intent.CATEGORY_APP_BROWSER)
      setClassName(packageName, settingsActivityClassName)
    }
  }

  fun openSettings(
    context: Context,
    integration: BrowserThirdPartyAutofillIntegration
  ): Boolean {
    return openSettings(context, integration.packageName, integration.settingsActivityClassName)
  }

  fun openSettings(
    context: Context,
    packageName: String,
    settingsActivityClassName: String = OPTIONS_LAUNCHER_CLASS
  ): Boolean {
    val intent = settingsIntent(packageName, settingsActivityClassName)
    if (context !is Activity) {
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return runCatching {
      context.startActivity(intent)
      true
    }.getOrElse {
      Timber.e(it, "open browser autofill options failed: %s", packageName)
      false
    }
  }

  @Suppress("DEPRECATION")
  fun isPackageInstalled(context: Context, packageName: String): Boolean {
    return try {
      context.packageManager.getPackageInfo(packageName, 0)
      true
    } catch (e: PackageManager.NameNotFoundException) {
      false
    } catch (e: Throwable) {
      Timber.e(e, "check browser package failed: %s", packageName)
      false
    }
  }
}
