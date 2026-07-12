package com.lyy.keepassa.view.detail

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupDetailCreateGroupParentContractTest {
  @Test
  fun groupDetailCreatesChildGroupUnderCurrentGroup() {
    val source = File("src/main/java/com/lyy/keepassa/view/detail/GroupDetailActivity.kt").readText()
    val initFab = source.substringAfter("private fun initFab()").substringBefore("private fun initMenu()")

    assertTrue(initFab.contains("module.curGroupV4?.let { parent ->"))
    assertTrue(initFab.contains("showCreateGroupDialog(parent)"))
    assertFalse(initFab.contains("showCreateGroupDialog(BaseApp.KDB!!.pm.rootGroup"))
  }
}
