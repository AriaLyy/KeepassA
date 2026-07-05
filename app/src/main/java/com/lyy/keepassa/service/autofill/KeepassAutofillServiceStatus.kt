/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.lyy.keepassa.service.autofill

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.autofill.AutofillManager
import timber.log.Timber

object KeepassAutofillServiceStatus {
  private const val AUTOFILL_SERVICE_SETTING = "autofill_service"

  fun isEnabled(
    context: Context,
    manager: AutofillManager?
  ): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
    val isAutofillSupported = runCatching {
      manager?.isAutofillSupported == true
    }.getOrElse {
      Timber.e(it, "check autofill support failed")
      false
    }
    val hasEnabledAutofillServices = runCatching {
      manager?.hasEnabledAutofillServices() == true
    }.getOrElse {
      Timber.e(it, "check enabled autofill service failed")
      false
    }
    val managerServicePackageName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      runCatching {
        manager?.autofillServiceComponentName?.packageName
      }.getOrElse {
        Timber.e(it, "read autofill service component failed")
        null
      }
    } else {
      null
    }
    val secureAutofillService = runCatching {
      Settings.Secure.getString(context.contentResolver, AUTOFILL_SERVICE_SETTING)
    }.getOrElse {
      Timber.e(it, "read secure autofill service setting failed")
      null
    }
    return isEnabledFromSignals(
      isAutofillSupported = isAutofillSupported,
      hasEnabledAutofillServices = hasEnabledAutofillServices,
      managerServicePackageName = managerServicePackageName,
      secureAutofillService = secureAutofillService,
      appPackageName = context.packageName,
      serviceClassName = AutoFillService::class.java.name
    )
  }

  fun isEnabledFromSignals(
    isAutofillSupported: Boolean,
    hasEnabledAutofillServices: Boolean,
    managerServicePackageName: String?,
    secureAutofillService: String?,
    appPackageName: String,
    serviceClassName: String
  ): Boolean {
    if (!isAutofillSupported) return false
    if (hasEnabledAutofillServices) return true
    if (managerServicePackageName == appPackageName) return true
    return secureAutofillServiceMatches(
      secureAutofillService = secureAutofillService,
      appPackageName = appPackageName,
      serviceClassName = serviceClassName
    )
  }

  private fun secureAutofillServiceMatches(
    secureAutofillService: String?,
    appPackageName: String,
    serviceClassName: String
  ): Boolean {
    val parts = secureAutofillService?.split("/", limit = 2) ?: return false
    if (parts.size != 2) return false
    val packageName = parts[0]
    val className = if (parts[1].startsWith(".")) {
      packageName + parts[1]
    } else {
      parts[1]
    }
    return packageName == appPackageName && className == serviceClassName
  }
}
