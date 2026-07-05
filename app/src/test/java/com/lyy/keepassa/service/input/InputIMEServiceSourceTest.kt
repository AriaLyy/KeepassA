package com.lyy.keepassa.service.input

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class InputIMEServiceSourceTest {

  @Test fun imeLayout_exposesSearchAndKeyboardContainers() {
    val layout = File("src/main/res/layout/layout_kpa_ime.xml").readText()

    assertTrue(layout.contains("android:id=\"@+id/imeSearchBar\""))
    assertTrue(layout.contains("android:id=\"@+id/tvImeSearchQuery\""))
    assertTrue(layout.contains("android:id=\"@+id/llImeKeyboard\""))
  }

  @Test fun candidateItem_hasSubtitleAndStableWidth() {
    val item = File("src/main/res/layout/item_ime_entry.xml").readText()

    assertTrue(item.contains("android:id=\"@+id/subtitle\""))
    assertTrue(item.contains("android:layout_width=\"160dp\""))
  }

  @Test fun candidatesAdapter_handlesEmptyStateWithoutPwEntryCast() {
    val source = File("src/main/java/com/lyy/keepassa/service/input/CandidatesAdapter.kt").readText()

    assertTrue(source.contains("ITEM_TYPE_EMPTY"))
    assertTrue(source.contains("item.obj as? PwEntry"))
  }

  @Test fun service_usesImeKeyboardComponents() {
    val source = File("src/main/java/com/lyy/keepassa/service/input/InputIMEService.kt").readText()

    assertTrue(source.contains("ImeKeyboardState"))
    assertTrue(source.contains("ImeKeyboardViewBinder"))
    assertTrue(source.contains("ImeEntrySearchEngine"))
    assertTrue(source.contains("ImeSearchSession"))
    assertTrue(source.contains("ImeManualSelectionPolicy"))
    assertTrue(source.contains("ImeBrowserDomainContext"))
  }

  @Test fun service_routesSearchTextWithoutCommitText() {
    val source = File("src/main/java/com/lyy/keepassa/service/input/InputIMEService.kt").readText()

    assertTrue(source.contains("handleSearchTextInput"))
    assertTrue(source.contains("keyboardState.isSearchMode"))
    assertTrue(source.contains("scheduleImeSearch"))
  }

  @Test fun searchField_isFocusableAndServiceMovesCursorIntoIt() {
    val layout = File("src/main/res/layout/layout_kpa_ime.xml").readText()
    val source = File("src/main/java/com/lyy/keepassa/service/input/InputIMEService.kt").readText()

    assertTrue(layout.contains("<androidx.appcompat.widget.AppCompatEditText"))
    assertTrue(layout.contains("android:id=\"@+id/tvImeSearchQuery\""))
    assertTrue(source.contains("focusImeSearchInput"))
    assertTrue(source.contains("requestFocus()"))
    assertTrue(source.contains("setSelection"))
    assertTrue(source.contains("showSoftInputOnFocus = false"))
  }

  @Test fun imeSearchBar_centersHintAndUsesDedicatedClearIcon() {
    val layout = File("src/main/res/layout/layout_kpa_ime.xml").readText()
    val clearIcon = File("src/main/res/drawable/ic_ime_search_clear.xml")

    assertTrue(layout.contains("android:id=\"@+id/tvImeSearchQuery\""))
    assertTrue(layout.contains("android:gravity=\"center_vertical\""))
    assertTrue(layout.contains("app:srcCompat=\"@drawable/ic_ime_search_clear\""))
    assertTrue(clearIcon.exists())
    assertTrue(clearIcon.readText().contains("pathData=\"M19,6.41"))
  }

  @Test fun imeKeyboard_usesNightAwareImeColors() {
    val layout = File("src/main/res/layout/layout_kpa_ime.xml").readText()
    val keyBackground = File("src/main/res/drawable/bg_ime_key.xml").readText()
    val searchBackground = File("src/main/res/drawable/bg_ime_search_bar.xml")
    val dayColors = File("src/main/res/values/colors.xml").readText()
    val nightColors = File("src/main/res/values-night/colors.xml").readText()
    val binder = File("src/main/java/com/lyy/keepassa/service/input/keyboard/ImeKeyboardViewBinder.kt").readText()

    assertTrue(layout.contains("android:background=\"@color/ime_keyboard_background\""))
    assertTrue(layout.contains("android:background=\"@drawable/bg_ime_search_bar\""))
    assertTrue(searchBackground.exists())
    assertTrue(keyBackground.contains("@color/ime_key_background"))
    assertTrue(keyBackground.contains("@color/ime_key_background_pressed"))
    assertTrue(binder.contains("R.color.ime_key_text"))
    listOf(
      "ime_keyboard_background",
      "ime_search_background",
      "ime_key_background",
      "ime_key_background_pressed",
      "ime_key_text",
      "ime_search_clear_icon"
    ).forEach { colorName ->
      assertTrue("missing day color $colorName", dayColors.contains("name=\"$colorName\""))
      assertTrue("missing night color $colorName", nightColors.contains("name=\"$colorName\""))
    }
  }

  @Test fun serviceRerendersKeyboardWithShiftState() {
    val service = File("src/main/java/com/lyy/keepassa/service/input/InputIMEService.kt").readText()
    val binder = File("src/main/java/com/lyy/keepassa/service/input/keyboard/ImeKeyboardViewBinder.kt").readText()

    assertTrue(service.contains("renderImeKeyboard()"))
    assertTrue(service.contains("render(keyboardState.page, keyboardState.shiftState)"))
    assertTrue(service.contains("val shiftBefore = keyboardState.shiftState"))
    assertTrue(binder.contains("fun render(page: ImeKeyboardPage, shiftState: ImeShiftState)"))
    assertTrue(binder.contains("displayShiftedText"))
  }

  @Test fun imeActionRow_containsOnlyCurrentFirstRowActionsInOrder() {
    val layout = File("src/main/res/layout/layout_kpa_ime.xml").readText()

    assertFalse(layout.contains("android:id=\"@+id/btLock\""))
    assertFalse(layout.contains("android:id=\"@+id/btBackspace\""))
    assertFalse(layout.contains("android:id=\"@+id/btEnter\""))

    val account = layout.indexOf("android:id=\"@+id/btAccount\"")
    val pass = layout.indexOf("android:id=\"@+id/btPass\"")
    val totp = layout.indexOf("android:id=\"@+id/btTotp\"")
    val more = layout.indexOf("android:id=\"@+id/btOtherInfo\"")
    val switchIme = layout.indexOf("android:id=\"@+id/btChangeIme\"")
    val close = layout.indexOf("android:id=\"@+id/btClose\"")

    assertTrue(account >= 0)
    assertTrue(account < pass)
    assertTrue(pass < totp)
    assertTrue(totp < more)
    assertTrue(more < switchIme)
    assertTrue(switchIme < close)

    assertTrue(layout.contains("app:layout_constraintTop_toTopOf=\"@+id/btAccount\""))
    assertTrue(layout.contains("app:layout_constraintTop_toBottomOf=\"@+id/btAccount\""))
  }

  @Test fun service_noLongerHandlesRemovedActionButtons() {
    val source = File("src/main/java/com/lyy/keepassa/service/input/InputIMEService.kt").readText()

    assertFalse(source.contains("R.id.btLock"))
    assertFalse(source.contains("R.id.btBackspace"))
    assertFalse(source.contains("R.id.btEnter ->"))
    assertFalse(source.contains("setupBackspaceLongPress"))
  }
}
