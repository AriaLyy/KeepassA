/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.service.autofill

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertTrue
import org.junit.Test

class W3cHintsCompatBrowserTest {

  @Test fun kiwiProxyBrowser_isTreatedAsBrowser() {
    assertTrue(W3cHints.isBrowser(KIWI_PROXY_BROWSER_PACKAGE))
  }

  @Test fun autofillCompatibilityConfigs_includeKiwiProxyBrowser() {
    listOf(
      "src/main/res/xml/auto_fill_service_configuration.xml",
      "src/main/res/xml-v28/auto_fill_service_configuration.xml",
      "src/main/res/xml-v30/auto_fill_service_configuration.xml"
    ).forEach { path ->
      assertTrue(
        "$path should include $KIWI_PROXY_BROWSER_PACKAGE",
        compatibilityPackages(path).contains(KIWI_PROXY_BROWSER_PACKAGE)
      )
    }
  }

  private fun compatibilityPackages(path: String): Set<String> {
    val document = DocumentBuilderFactory.newInstance().apply {
      isNamespaceAware = true
    }.newDocumentBuilder().parse(File(path))
    val packages = document.getElementsByTagName("compatibility-package")
    val androidNamespace = "http://schemas.android.com/apk/res/android"
    return buildSet {
      for (i in 0 until packages.length) {
        val item = packages.item(i)
        item.attributes.getNamedItemNS(androidNamespace, "name")?.nodeValue?.let(::add)
      }
    }
  }

  private companion object {
    const val KIWI_PROXY_BROWSER_PACKAGE = "secure.unblock.unlimited.proxy.snap.hotspot.shield"
  }
}
