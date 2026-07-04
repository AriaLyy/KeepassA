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

class PrivacyAgreementDialogBehaviorTest {

  @Test fun privacyAgreementDialogCannotBeDismissedByOutsideTouchOrBackKey() {
    val launcherModule = File("src/main/java/com/lyy/keepassa/view/launcher/LauncherModule.kt")
      .readText()
    val showPrivacyAgreement = launcherModule
      .substringAfter("fun showPrivacyAgreement(context: Context)")
      .substringBefore("\n  }")

    assertTrue(showPrivacyAgreement.contains("cancelable = false"))
    assertTrue(showPrivacyAgreement.contains("interceptBackKey = true"))
  }

  @Test fun msgDialogSupportsCancelableRouterArgument() {
    val dialogRouter = File("src/main/java/com/lyy/keepassa/router/DialogRouter.kt").readText()
    val msgDialog = File("src/main/java/com/lyy/keepassa/view/dialog/MsgDialog.kt").readText()

    assertTrue(dialogRouter.contains("@RouterArgName(name = \"cancelable\") cancelable: Boolean = true"))
    assertTrue(msgDialog.contains("@Autowired(name = \"cancelable\")"))
    assertTrue(msgDialog.contains("var cancelable: Boolean = true"))
    assertTrue(msgDialog.contains("isCancelable = cancelable"))
    assertTrue(msgDialog.contains("dialog?.setCanceledOnTouchOutside(cancelable)"))
  }

  @Test fun msgDialogContentScrollContainerOwnsMaxHeight() {
    val document = DocumentBuilderFactory.newInstance().apply {
      isNamespaceAware = true
    }.newDocumentBuilder()
      .parse(File("src/main/res/layout/dialog_msg.xml"))
    val scroll = document.getElementsByTagName("com.lyy.keepassa.widget.MaxHeightNestedScrollView")
    val textViews = document.getElementsByTagName("TextView")
    val androidNamespace = "http://schemas.android.com/apk/res/android"

    assertTrue("dialog_msg.xml should use MaxHeightNestedScrollView", scroll.length == 1)
    assertTrue(
      "scroll container should cap height so long privacy text can scroll",
      scroll.item(0).attributes.getNamedItemNS(androidNamespace, "maxHeight")?.nodeValue == "400dp"
    )

    for (i in 0 until textViews.length) {
      val item = textViews.item(i)
      assertFalse(
        "content TextView must not own maxHeight or long privacy text will be clipped",
        item.attributes.getNamedItemNS(androidNamespace, "maxHeight") != null
      )
    }
  }
}
