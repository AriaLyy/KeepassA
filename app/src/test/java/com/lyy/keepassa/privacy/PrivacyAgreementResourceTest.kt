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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacyAgreementResourceTest {

  @Test fun privacyAgreement_hasDefaultSimplifiedAndTraditionalContent() {
    listOf(
      "src/main/res/values/strings.xml",
      "src/main/res/values-zh-rCN/strings.xml",
      "src/main/res/values-zh-rTW/strings.xml"
    ).forEach { path ->
      val content = stringResource(path, "privacy_agreement_content_html")

      assertTrue("$path should mention KeePass-compatible usage", content.contains("KeePass"))
      assertTrue("$path should disclose Firebase Crashlytics", content.contains("Firebase Crashlytics"))
      assertFalse("$path must not mention Bugly", content.contains("Bugly", ignoreCase = true))
    }
  }

  @Test fun privacyAgreement_hasLocalizedActionLabels() {
    assertTrue(stringResource("src/main/res/values/strings.xml", "privacy_agreement_accept").contains("Agree"))
    assertTrue(stringResource("src/main/res/values/strings.xml", "privacy_agreement_exit").contains("Exit"))

    assertTrue(stringResource("src/main/res/values-zh-rCN/strings.xml", "privacy_agreement_accept").contains("同意"))
    assertTrue(stringResource("src/main/res/values-zh-rCN/strings.xml", "privacy_agreement_exit").contains("退出应用"))

    assertTrue(stringResource("src/main/res/values-zh-rTW/strings.xml", "privacy_agreement_accept").contains("同意"))
    assertTrue(stringResource("src/main/res/values-zh-rTW/strings.xml", "privacy_agreement_exit").contains("退出應用"))
  }

  @Test fun launcherModule_readsPrivacyAgreementFromResources() {
    val launcherModule = File("src/main/java/com/lyy/keepassa/view/launcher/LauncherModule.kt").readText()

    assertTrue(launcherModule.contains("R.string.privacy_agreement_content_html"))
    assertTrue(launcherModule.contains("R.string.privacy_agreement_accept"))
    assertTrue(launcherModule.contains("R.string.privacy_agreement_exit"))
    assertFalse(launcherModule.contains("Bugly"))
    assertFalse(launcherModule.contains("是否同意本协议"))
  }

  private fun stringResource(path: String, name: String): String {
    val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(path))
    val strings = document.getElementsByTagName("string")
    for (i in 0 until strings.length) {
      val item = strings.item(i)
      if (item.attributes.getNamedItem("name")?.nodeValue == name) {
        return item.textContent
      }
    }
    error("Missing string resource $name in $path")
  }
}
