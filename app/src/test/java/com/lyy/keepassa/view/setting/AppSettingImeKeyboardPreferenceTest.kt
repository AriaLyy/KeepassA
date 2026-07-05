package com.lyy.keepassa.view.setting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class AppSettingImeKeyboardPreferenceTest {

  @Test fun hapticPreference_isDirectlyBelowSecureKeyboardInSafetyGroup() {
    val document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
      .parse(File("src/main/res/xml/app_setting.xml"))
    val safetyGroup = document.getElementsByTagName("PreferenceCategory").item(0) as Element
    val directKeys = safetyGroup.childNodes.asSequence()
      .filterIsInstance<Element>()
      .mapNotNull { it.getAttribute("app:key").takeIf(String::isNotBlank) }
      .toList()

    val imeIndex = directKeys.indexOf("@string/set_key_open_kpa_ime")
    val hapticIndex = directKeys.indexOf("@string/set_key_ime_keyboard_haptic_feedback")

    assertTrue(imeIndex >= 0)
    assertEquals(imeIndex + 1, hapticIndex)
  }

  @Test fun hapticPreference_defaultsEnabledAndIsNotInAutofillGroup() {
    val document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
      .parse(File("src/main/res/xml/app_setting.xml"))
    val haptic = document.getElementsByTagName("SwitchPreference").asSequence()
      .filterIsInstance<Element>()
      .first { it.getAttribute("app:key") == "@string/set_key_ime_keyboard_haptic_feedback" }

    assertEquals("true", haptic.getAttribute("android:defaultValue"))

    val autoFillGroup = document.getElementsByTagName("PreferenceCategory").asSequence()
      .filterIsInstance<Element>()
      .first { it.getAttribute("app:key") == "@string/set_key_auto_fill_category" }
    val autoFillKeys = autoFillGroup.childNodes.asSequence()
      .filterIsInstance<Element>()
      .mapNotNull { it.getAttribute("app:key").takeIf(String::isNotBlank) }
      .toList()

    assertFalse(autoFillKeys.contains("@string/set_key_ime_keyboard_haptic_feedback"))
  }

  private fun org.w3c.dom.NodeList.asSequence(): Sequence<org.w3c.dom.Node> =
    (0 until length).asSequence().map { item(it) }
}
