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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoFillSaveEntryBinderTest {

  @Test
  fun applyPackageAssociation_writesAndroidAppUrlForAutofillSave() {
    val strings = hashMapOf<String, ProtectedString>()

    val changed = AutoFillSaveEntryBinder.applyPackageAssociation(
      strings,
      AutoFillParam(apkPkgName = "com.lyy.autofill.savedemo", isSave = true)
    )

    assertTrue(changed)
    assertEquals("androidapp://com.lyy.autofill.savedemo", strings["KP2A_URL_1"].toString())
  }

  @Test
  fun applyPackageAssociation_usesNextFreeSlotWithoutOverwritingExistingUrls() {
    val strings = hashMapOf(
      "KP2A_URL_1" to ProtectedString(false, "androidapp://existing.app")
    )

    val changed = AutoFillSaveEntryBinder.applyPackageAssociation(
      strings,
      AutoFillParam(apkPkgName = "com.lyy.autofill.savedemo", isSave = true)
    )

    assertTrue(changed)
    assertEquals("androidapp://existing.app", strings["KP2A_URL_1"].toString())
    assertEquals("androidapp://com.lyy.autofill.savedemo", strings["KP2A_URL_2"].toString())
  }

  @Test
  fun applyPackageAssociation_skipsDuplicateAndroidAppUrl() {
    val strings = hashMapOf(
      "KP2A_URL_1" to ProtectedString(false, "androidapp://com.lyy.autofill.savedemo")
    )

    val changed = AutoFillSaveEntryBinder.applyPackageAssociation(
      strings,
      AutoFillParam(apkPkgName = "com.lyy.autofill.savedemo", isSave = true)
    )

    assertFalse(changed)
    assertEquals(1, strings.size)
  }

  @Test
  fun applyPackageAssociation_ignoresNonSaveAutofillParam() {
    val strings = hashMapOf<String, ProtectedString>()

    val changed = AutoFillSaveEntryBinder.applyPackageAssociation(
      strings,
      AutoFillParam(apkPkgName = "com.lyy.autofill.savedemo", isSave = false)
    )

    assertFalse(changed)
    assertTrue(strings.isEmpty())
  }

  @Test
  fun applyPackageAssociation_skipsPackageAssociationWhenWebUrlExists() {
    val strings = hashMapOf<String, ProtectedString>()

    val changed = AutoFillSaveEntryBinder.applyPackageAssociation(
      strings,
      AutoFillParam(
        apkPkgName = "com.webview.host",
        domain = "https://login.example.com",
        isSave = true
      )
    )

    assertFalse(changed)
    assertTrue(strings.isEmpty())
  }

  @Test
  fun getWebUrl_returnsTrimmedDomainForWebViewSave() {
    val webUrl = AutoFillSaveEntryBinder.getWebUrl(
      AutoFillParam(
        apkPkgName = "com.webview.host",
        domain = "  https://login.example.com  ",
        isSave = true
      )
    )

    assertEquals("https://login.example.com", webUrl)
  }

  @Test
  fun getWebUrl_returnsDomainEvenWhenAutofillSearchCreatesANewEntry() {
    val webUrl = AutoFillSaveEntryBinder.getWebUrl(
      AutoFillParam(
        apkPkgName = "com.webview.host",
        domain = "login.example.com",
        isSave = false
      )
    )

    assertEquals("login.example.com", webUrl)
  }

  @Test
  fun prepareCustomFieldsForCreateUi_writesPackageAssociationForNativeAppSave() {
    val strings = hashMapOf<String, ProtectedString>()

    val shouldShowCustomFields = AutoFillSaveEntryBinder.prepareCustomFieldsForCreateUi(
      strings,
      AutoFillParam(apkPkgName = "com.lyy.autofill.savedemo", isSave = true)
    )

    assertTrue(shouldShowCustomFields)
    assertEquals("androidapp://com.lyy.autofill.savedemo", strings["KP2A_URL_1"].toString())
  }

  @Test
  fun prepareCustomFieldsForCreateUi_doesNotWritePackageAssociationForWebViewSave() {
    val strings = hashMapOf<String, ProtectedString>()

    val shouldShowCustomFields = AutoFillSaveEntryBinder.prepareCustomFieldsForCreateUi(
      strings,
      AutoFillParam(
        apkPkgName = "com.webview.host",
        domain = "https://login.example.com",
        isSave = true
      )
    )

    assertFalse(shouldShowCustomFields)
    assertTrue(strings.isEmpty())
  }
}
