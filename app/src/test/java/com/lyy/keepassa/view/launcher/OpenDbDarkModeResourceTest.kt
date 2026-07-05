/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.view.launcher

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Node

class OpenDbDarkModeResourceTest {

  private val androidNamespace = "http://schemas.android.com/apk/res/android"

  @Test fun openDbActionsUseLauncherOnBackgroundTextColor() {
    val document = openDbLayout()
    val launcherTextColor = "@color/launcher_on_background_text"

    listOf("cb_key", "key", "open", "change_db").forEach { id ->
      assertEquals(
        "$id must not use @color/color_FFFFFF because that resource is dark in values-night",
        launcherTextColor,
        document.nodeById(id).androidAttribute("textColor")
      )
    }
  }

  private fun openDbLayout(): Document {
    return DocumentBuilderFactory.newInstance().apply {
      isNamespaceAware = true
    }.newDocumentBuilder()
      .parse(File("src/main/res/layout/fragment_open_db.xml"))
  }

  private fun Document.nodeById(id: String): Node {
    val expected = "@+id/$id"
    val nodes = getElementsByTagName("*")
    for (i in 0 until nodes.length) {
      val node = nodes.item(i)
      if (node.androidAttribute("id") == expected) {
        return node
      }
    }
    throw AssertionError("Missing view id: $expected")
  }

  private fun Node.androidAttribute(name: String): String? {
    return attributes?.getNamedItemNS(androidNamespace, name)?.nodeValue
  }
}
