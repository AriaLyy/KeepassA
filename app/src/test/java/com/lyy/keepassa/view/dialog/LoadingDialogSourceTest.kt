package com.lyy.keepassa.view.dialog

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoadingDialogSourceTest {

  @Test
  fun dismissUsesStateLossSafeDismissForForegroundSyncCancellation() {
    val source = File("src/main/java/com/lyy/keepassa/view/dialog/LoadingDialog.java").readText()
    val dismissMethod = source.substringAfter("public void dismiss(long delay)")

    assertTrue(dismissMethod.contains("dismissAllowingStateLoss()"))
    assertFalse(dismissMethod.contains("super.dismiss()"))
  }
}
