/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.view.create.entry

import com.keepassdroid.database.security.ProtectedString
import com.lyy.keepassa.entity.AutoFillParam

internal object AutoFillSaveEntryBinder {
  private const val ANDROID_APP_URL_PREFIX = "androidapp://"
  private const val KP2A_URL_PREFIX = "KP2A_URL_"

  fun getWebUrl(autoFillParam: AutoFillParam?): String? {
    return autoFillParam?.domain?.trim()?.takeIf { it.isNotEmpty() }
  }

  fun prepareCustomFieldsForCreateUi(
    strings: MutableMap<String, ProtectedString>,
    autoFillParam: AutoFillParam?
  ): Boolean {
    val targetUrl = getPackageAssociationUrl(autoFillParam) ?: return false
    applyPackageAssociation(strings, autoFillParam)
    return strings.values.any { it.toString().equals(targetUrl, ignoreCase = true) }
  }

  fun applyPackageAssociation(
    strings: MutableMap<String, ProtectedString>,
    autoFillParam: AutoFillParam?
  ): Boolean {
    if (autoFillParam?.isSave != true || getWebUrl(autoFillParam) != null) {
      return false
    }
    return applyPackageAssociation(strings, autoFillParam.apkPkgName)
  }

  fun applyPackageAssociation(
    strings: MutableMap<String, ProtectedString>,
    apkPkgName: String?
  ): Boolean {
    val normalizedPackageName = apkPkgName?.trim()?.takeIf { it.isNotEmpty() } ?: return false
    val targetUrl = "$ANDROID_APP_URL_PREFIX$normalizedPackageName"
    if (strings.values.any { it.toString().equals(targetUrl, ignoreCase = true) }) {
      return false
    }

    for (i in 1 until 100) {
      val key = "$KP2A_URL_PREFIX$i"
      if (strings[key] == null) {
        strings[key] = ProtectedString(false, targetUrl)
        return true
      }
    }
    return false
  }

  private fun getPackageAssociationUrl(autoFillParam: AutoFillParam?): String? {
    if (autoFillParam?.isSave != true || getWebUrl(autoFillParam) != null) {
      return null
    }
    val normalizedPackageName = autoFillParam.apkPkgName.trim().takeIf { it.isNotEmpty() }
      ?: return null
    return "$ANDROID_APP_URL_PREFIX$normalizedPackageName"
  }
}
