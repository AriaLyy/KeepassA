package com.lyy.keepassa.service.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImeManualSelectionPolicyTest {

  @Test fun manualSelection_survivesSamePackageInputSwitch() {
    val policy = ImeManualSelectionPolicy<String>()

    policy.rememberManualSelection("com.android.chrome", "entry")
    policy.onStartInput("com.android.chrome")

    assertTrue(policy.hasManualSelectionFor("com.android.chrome"))
    assertEquals("entry", policy.currentSelection)
  }

  @Test fun packageChange_clearsManualSelection() {
    val policy = ImeManualSelectionPolicy<String>()

    policy.rememberManualSelection("com.android.chrome", "entry")
    policy.onStartInput("com.vivaldi.browser")

    assertFalse(policy.hasManualSelectionFor("com.android.chrome"))
    assertNull(policy.currentSelection)
  }

  @Test fun shouldUseAutomaticCandidates_isFalseAfterManualSelectionInSamePackage() {
    val policy = ImeManualSelectionPolicy<String>()

    policy.rememberManualSelection("com.android.chrome", "entry")

    assertFalse(policy.shouldUseAutomaticCandidates("com.android.chrome"))
  }

  @Test fun newSearch_clearsManualSelection() {
    val policy = ImeManualSelectionPolicy<String>()

    policy.rememberManualSelection("com.android.chrome", "entry")
    policy.onNewSearch()

    assertNull(policy.currentSelection)
  }

  @Test fun lockOrDestroy_clearsManualSelection() {
    val policy = ImeManualSelectionPolicy<String>()

    policy.rememberManualSelection("com.android.chrome", "entry")
    policy.clear()

    assertNull(policy.currentSelection)
  }
}
