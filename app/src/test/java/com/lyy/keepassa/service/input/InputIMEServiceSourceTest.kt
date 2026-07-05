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
