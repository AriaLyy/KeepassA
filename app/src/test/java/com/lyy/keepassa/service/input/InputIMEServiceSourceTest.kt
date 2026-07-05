package com.lyy.keepassa.service.input

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
}
