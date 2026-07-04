/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.privacy

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashlyticsConsentConfigTest {

  @Test fun manifest_disablesCrashlyticsCollectionByDefault() {
    val manifest = File("src/main/AndroidManifest.xml")
    val document = DocumentBuilderFactory.newInstance().apply {
      isNamespaceAware = true
    }.newDocumentBuilder().parse(manifest)
    val metadata = document.getElementsByTagName("meta-data")
    val androidNamespace = "http://schemas.android.com/apk/res/android"

    var crashlyticsCollectionValue: String? = null
    for (i in 0 until metadata.length) {
      val item = metadata.item(i)
      if (item.attributes.getNamedItemNS(androidNamespace, "name")?.nodeValue == CRASHLYTICS_COLLECTION_KEY) {
        crashlyticsCollectionValue = item.attributes.getNamedItemNS(androidNamespace, "value")?.nodeValue
      }
    }

    assertEquals("false", crashlyticsCollectionValue)
  }

  @Test fun thirdSdkInitialization_enablesCrashlyticsCollectionAfterConsent() {
    val service = File("src/main/java/com/lyy/keepassa/service/feat/KpaSdkService.kt").readText()

    assertTrue(service.contains("CrashlyticsConsent.enableCollection()"))
  }

  private companion object {
    const val CRASHLYTICS_COLLECTION_KEY = "firebase_crashlytics_collection_enabled"
  }
}
