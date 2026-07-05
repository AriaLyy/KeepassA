# KeePassA IME Keyboard Search Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a lightweight KeePassA soft keyboard and in-IME entry search that works in browsers without relying on the current page URL.

**Architecture:** Keep `InputIMEService` as the lifecycle and UI orchestrator while moving keyboard state, key layout, search ranking, manual selection, haptic settings, and browser-domain freshness into focused Kotlin components. Use custom XML containers and a small keyboard view binder rather than `KeyboardView` or a third-party keyboard engine.

**Tech Stack:** Android `InputMethodService`, Kotlin, XML layouts, AndroidX Preference, JUnit4 JVM tests, existing KeePassA database model and utilities.

---

## Working Tree Rules

- Do not use `git add -A`.
- Stage only files created or modified by the current task.
- Preserve unrelated dirty files already present in the workspace.
- Run focused tests before every task commit.
- Keep commits small and task-scoped.

## File Structure

Create these focused IME components:

- `app/src/main/java/com/lyy/keepassa/service/input/keyboard/ImeKeyAction.kt`
  - Sealed key action model.
- `app/src/main/java/com/lyy/keepassa/service/input/keyboard/ImeKeyboardState.kt`
  - Ordinary/search mode, alphabet/symbol page, Shift, and in-memory query state.
- `app/src/main/java/com/lyy/keepassa/service/input/keyboard/ImeKeyboardLayout.kt`
  - QWERTY and number/symbol key row definitions.
- `app/src/main/java/com/lyy/keepassa/service/input/keyboard/ImeKeyboardPreferences.kt`
  - Reads the haptic-feedback setting.
- `app/src/main/java/com/lyy/keepassa/service/input/keyboard/ImeKeyboardViewBinder.kt`
  - Renders key definitions into the IME keyboard container and forwards key actions.
- `app/src/main/java/com/lyy/keepassa/service/input/search/ImeEntrySearchEngine.kt`
  - IME-safe entry search and ranking.
- `app/src/main/java/com/lyy/keepassa/service/input/search/ImeSearchSession.kt`
  - Search-mode query/result/selection state and debounce constant.
- `app/src/main/java/com/lyy/keepassa/service/input/ImeManualSelectionPolicy.kt`
  - Prevents automatic candidates from overriding manual selection in the same package session.
- `app/src/main/java/com/lyy/keepassa/service/autofill/ImeBrowserDomainContext.kt`
  - Package-bound, TTL-bound domain context published by autofill parsing and consumed by IME.

Modify these existing files:

- `app/src/main/java/com/lyy/keepassa/service/input/InputIMEService.kt`
  - Wire keyboard, search mode, manual selection, browser-domain context, and haptic feedback.
- `app/src/main/java/com/lyy/keepassa/service/input/CandidatesAdapter.kt`
  - Display optional subtitle and a lightweight empty state.
- `app/src/main/res/layout/layout_kpa_ime.xml`
  - Add visible search area and keyboard container.
- `app/src/main/res/layout/item_ime_entry.xml`
  - Add fixed width and subtitle line.
- `app/src/main/res/xml/app_setting.xml`
  - Add keyboard haptic feedback switch under the secure keyboard item.
- `app/src/main/res/values/strings.xml`
  - Add English strings and setting key.
- `app/src/main/res/values-zh-rCN/strings.xml`
  - Add Chinese strings for the new visible UI.
- `app/src/main/java/com/lyy/keepassa/service/autofill/StructureParser.kt`
  - Publish domain context when a reliable browser domain is parsed.

Create these tests:

- `app/src/test/java/com/lyy/keepassa/service/input/keyboard/ImeKeyboardStateTest.kt`
- `app/src/test/java/com/lyy/keepassa/service/input/keyboard/ImeKeyboardLayoutTest.kt`
- `app/src/test/java/com/lyy/keepassa/service/input/search/ImeEntrySearchEngineTest.kt`
- `app/src/test/java/com/lyy/keepassa/service/input/search/ImeSearchSessionTest.kt`
- `app/src/test/java/com/lyy/keepassa/service/input/ImeManualSelectionPolicyTest.kt`
- `app/src/test/java/com/lyy/keepassa/service/autofill/ImeBrowserDomainContextTest.kt`
- `app/src/test/java/com/lyy/keepassa/service/input/InputIMEServiceSourceTest.kt`
- `app/src/test/java/com/lyy/keepassa/view/setting/AppSettingImeKeyboardPreferenceTest.kt`

## Task 1: Keyboard State, Actions, and Layout

**Files:**
- Create: `app/src/main/java/com/lyy/keepassa/service/input/keyboard/ImeKeyAction.kt`
- Create: `app/src/main/java/com/lyy/keepassa/service/input/keyboard/ImeKeyboardState.kt`
- Create: `app/src/main/java/com/lyy/keepassa/service/input/keyboard/ImeKeyboardLayout.kt`
- Test: `app/src/test/java/com/lyy/keepassa/service/input/keyboard/ImeKeyboardStateTest.kt`
- Test: `app/src/test/java/com/lyy/keepassa/service/input/keyboard/ImeKeyboardLayoutTest.kt`

- [ ] **Step 1: Write the failing keyboard state tests**

Create `app/src/test/java/com/lyy/keepassa/service/input/keyboard/ImeKeyboardStateTest.kt`:

```kotlin
package com.lyy.keepassa.service.input.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImeKeyboardStateTest {

  @Test fun defaultState_isOrdinaryLowercaseAlphabet() {
    val state = ImeKeyboardState()

    assertFalse(state.isSearchMode)
    assertEquals(ImeKeyboardPage.ALPHABET, state.page)
    assertEquals(ImeShiftState.LOWERCASE, state.shiftState)
    assertEquals("", state.searchQuery)
  }

  @Test fun searchQuery_isOnlyChangedInSearchMode() {
    val state = ImeKeyboardState()

    assertFalse(state.appendSearchText("g"))
    state.enterSearchMode()
    assertTrue(state.appendSearchText("g"))
    assertTrue(state.appendSearchText("it"))

    assertEquals("git", state.searchQuery)
  }

  @Test fun clearAndExitSearchMode_clearsQuery() {
    val state = ImeKeyboardState()
    state.enterSearchMode()
    state.appendSearchText("github")

    state.exitSearchMode()

    assertFalse(state.isSearchMode)
    assertEquals("", state.searchQuery)
  }

  @Test fun backspaceInSearchMode_removesLastChar() {
    val state = ImeKeyboardState()
    state.enterSearchMode()
    state.appendSearchText("abc")

    assertTrue(state.backspaceSearchQuery())

    assertEquals("ab", state.searchQuery)
  }

  @Test fun singleShift_uppercasesOneCharacterThenResets() {
    val state = ImeKeyboardState()

    state.tapShift(nowMillis = 1_000)
    val first = state.applyShiftTo("a")
    val second = state.applyShiftTo("b")

    assertEquals("A", first)
    assertEquals("b", second)
    assertEquals(ImeShiftState.LOWERCASE, state.shiftState)
  }

  @Test fun doubleShift_locksUppercase() {
    val state = ImeKeyboardState()

    state.tapShift(nowMillis = 1_000)
    state.tapShift(nowMillis = 1_250)

    assertEquals(ImeShiftState.UPPERCASE_LOCKED, state.shiftState)
    assertEquals("A", state.applyShiftTo("a"))
    assertEquals("B", state.applyShiftTo("b"))
  }

  @Test fun longPressShift_locksUppercase() {
    val state = ImeKeyboardState()

    state.longPressShift()

    assertEquals(ImeShiftState.UPPERCASE_LOCKED, state.shiftState)
  }

  @Test fun switchPage_togglesAlphabetAndSymbols() {
    val state = ImeKeyboardState()

    state.switchToSymbols()
    assertEquals(ImeKeyboardPage.SYMBOLS, state.page)

    state.switchToAlphabet()
    assertEquals(ImeKeyboardPage.ALPHABET, state.page)
  }
}
```

- [ ] **Step 2: Write the failing keyboard layout tests**

Create `app/src/test/java/com/lyy/keepassa/service/input/keyboard/ImeKeyboardLayoutTest.kt`:

```kotlin
package com.lyy.keepassa.service.input.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImeKeyboardLayoutTest {

  @Test fun alphabetRows_matchQwertyDesign() {
    val rows = ImeKeyboardLayout.alphabetRows()

    assertEquals("qwertyuiop", rows[0].characters())
    assertEquals("asdfghjkl", rows[1].characters())
    assertEquals("zxcvbnm", rows[2].filterIsInstance<ImeKeyAction.CommitText>().joinToString("") { it.text })
  }

  @Test fun alphabetRows_includeRequiredFunctionKeys() {
    val flattened = ImeKeyboardLayout.alphabetRows().flatten()

    assertTrue(flattened.contains(ImeKeyAction.Shift))
    assertTrue(flattened.contains(ImeKeyAction.Backspace))
    assertTrue(flattened.contains(ImeKeyAction.SwitchToSymbols))
    assertTrue(flattened.contains(ImeKeyAction.Space))
    assertTrue(flattened.contains(ImeKeyAction.Enter))
  }

  @Test fun symbolRows_includeLoginAndSearchCharacters() {
    val chars = ImeKeyboardLayout.symbolRows()
      .flatten()
      .filterIsInstance<ImeKeyAction.CommitText>()
      .joinToString("") { it.text }

    listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
      "@", ".", "_", "-", "+", "/", ":", "?", "&", "=", "#", "%", "!", "*", "(", ")"
    ).forEach { expected ->
      assertTrue("missing $expected in $chars", chars.contains(expected))
    }
  }

  @Test fun symbolRows_includeRequiredFunctionKeys() {
    val flattened = ImeKeyboardLayout.symbolRows().flatten()

    assertTrue(flattened.contains(ImeKeyAction.SwitchToAlphabet))
    assertTrue(flattened.contains(ImeKeyAction.Backspace))
    assertTrue(flattened.contains(ImeKeyAction.Space))
    assertTrue(flattened.contains(ImeKeyAction.Enter))
  }

  private fun List<ImeKeyAction>.characters(): String =
    filterIsInstance<ImeKeyAction.CommitText>().joinToString("") { it.text }
}
```

- [ ] **Step 3: Run tests to verify they fail**

Run:

```powershell
.\gradlew.bat :app:testDevDebugUnitTest --tests com.lyy.keepassa.service.input.keyboard.ImeKeyboardStateTest --tests com.lyy.keepassa.service.input.keyboard.ImeKeyboardLayoutTest
```

Expected: `BUILD FAILED` with unresolved references for `ImeKeyboardState`, `ImeKeyboardPage`, `ImeShiftState`, `ImeKeyboardLayout`, or `ImeKeyAction`.

- [ ] **Step 4: Add the minimal keyboard action model**

Create `app/src/main/java/com/lyy/keepassa/service/input/keyboard/ImeKeyAction.kt`:

```kotlin
package com.lyy.keepassa.service.input.keyboard

sealed class ImeKeyAction {
  data class CommitText(val text: String) : ImeKeyAction()
  object Space : ImeKeyAction()
  object Backspace : ImeKeyAction()
  object Enter : ImeKeyAction()
  object Shift : ImeKeyAction()
  object SwitchToSymbols : ImeKeyAction()
  object SwitchToAlphabet : ImeKeyAction()
  object EnterSearchMode : ImeKeyAction()
  object ClearSearch : ImeKeyAction()
  object ExitSearchMode : ImeKeyAction()
}
```

- [ ] **Step 5: Add the keyboard state implementation**

Create `app/src/main/java/com/lyy/keepassa/service/input/keyboard/ImeKeyboardState.kt`:

```kotlin
package com.lyy.keepassa.service.input.keyboard

enum class ImeKeyboardPage {
  ALPHABET,
  SYMBOLS
}

enum class ImeShiftState {
  LOWERCASE,
  UPPERCASE_NEXT,
  UPPERCASE_LOCKED
}

class ImeKeyboardState {
  companion object {
    const val SHIFT_DOUBLE_TAP_WINDOW_MS = 400L
  }

  var isSearchMode: Boolean = false
    private set

  var page: ImeKeyboardPage = ImeKeyboardPage.ALPHABET
    private set

  var shiftState: ImeShiftState = ImeShiftState.LOWERCASE
    private set

  var searchQuery: String = ""
    private set

  private var lastShiftTapMillis: Long = Long.MIN_VALUE

  fun enterSearchMode() {
    isSearchMode = true
    searchQuery = ""
  }

  fun exitSearchMode() {
    isSearchMode = false
    searchQuery = ""
  }

  fun clearSearchQuery() {
    searchQuery = ""
  }

  fun appendSearchText(text: String): Boolean {
    if (!isSearchMode) return false
    searchQuery += text
    return true
  }

  fun backspaceSearchQuery(): Boolean {
    if (!isSearchMode || searchQuery.isEmpty()) return false
    searchQuery = searchQuery.dropLast(1)
    return true
  }

  fun switchToSymbols() {
    page = ImeKeyboardPage.SYMBOLS
  }

  fun switchToAlphabet() {
    page = ImeKeyboardPage.ALPHABET
  }

  fun tapShift(nowMillis: Long) {
    shiftState = if (nowMillis - lastShiftTapMillis <= SHIFT_DOUBLE_TAP_WINDOW_MS) {
      ImeShiftState.UPPERCASE_LOCKED
    } else {
      when (shiftState) {
        ImeShiftState.LOWERCASE -> ImeShiftState.UPPERCASE_NEXT
        ImeShiftState.UPPERCASE_NEXT -> ImeShiftState.UPPERCASE_LOCKED
        ImeShiftState.UPPERCASE_LOCKED -> ImeShiftState.LOWERCASE
      }
    }
    lastShiftTapMillis = nowMillis
  }

  fun longPressShift() {
    shiftState = ImeShiftState.UPPERCASE_LOCKED
  }

  fun applyShiftTo(text: String): String {
    val result = when (shiftState) {
      ImeShiftState.LOWERCASE -> text
      ImeShiftState.UPPERCASE_NEXT,
      ImeShiftState.UPPERCASE_LOCKED -> text.uppercase()
    }
    if (shiftState == ImeShiftState.UPPERCASE_NEXT) {
      shiftState = ImeShiftState.LOWERCASE
    }
    return result
  }
}
```

- [ ] **Step 6: Add the keyboard layout implementation**

Create `app/src/main/java/com/lyy/keepassa/service/input/keyboard/ImeKeyboardLayout.kt`:

```kotlin
package com.lyy.keepassa.service.input.keyboard

object ImeKeyboardLayout {

  fun alphabetRows(): List<List<ImeKeyAction>> = listOf(
    "qwertyuiop".toCommitTextActions(),
    "asdfghjkl".toCommitTextActions(),
    listOf(ImeKeyAction.Shift) + "zxcvbnm".toCommitTextActions() + ImeKeyAction.Backspace,
    listOf(
      ImeKeyAction.SwitchToSymbols,
      ImeKeyAction.CommitText(","),
      ImeKeyAction.Space,
      ImeKeyAction.CommitText("."),
      ImeKeyAction.Enter
    )
  )

  fun symbolRows(): List<List<ImeKeyAction>> = listOf(
    "1234567890".toCommitTextActions(),
    listOf("@", ".", "_", "-", "+", "/", ":", "?", "&").map { ImeKeyAction.CommitText(it) },
    listOf("=", "#", "%", "!", "*", "(", ")").map { ImeKeyAction.CommitText(it) },
    listOf(
      ImeKeyAction.SwitchToAlphabet,
      ImeKeyAction.Space,
      ImeKeyAction.Backspace,
      ImeKeyAction.Enter
    )
  )

  private fun String.toCommitTextActions(): List<ImeKeyAction.CommitText> =
    map { ImeKeyAction.CommitText(it.toString()) }
}
```

- [ ] **Step 7: Run tests to verify they pass**

Run:

```powershell
.\gradlew.bat :app:testDevDebugUnitTest --tests com.lyy.keepassa.service.input.keyboard.ImeKeyboardStateTest --tests com.lyy.keepassa.service.input.keyboard.ImeKeyboardLayoutTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Commit keyboard state and layout**

Run:

```powershell
git add -- app/src/main/java/com/lyy/keepassa/service/input/keyboard/ImeKeyAction.kt app/src/main/java/com/lyy/keepassa/service/input/keyboard/ImeKeyboardState.kt app/src/main/java/com/lyy/keepassa/service/input/keyboard/ImeKeyboardLayout.kt app/src/test/java/com/lyy/keepassa/service/input/keyboard/ImeKeyboardStateTest.kt app/src/test/java/com/lyy/keepassa/service/input/keyboard/ImeKeyboardLayoutTest.kt
git commit -m "feat: add ime keyboard state"
```

Expected: one commit containing only the files listed in this task.

## Task 2: Browser Domain Context for IME

**Files:**
- Create: `app/src/main/java/com/lyy/keepassa/service/autofill/ImeBrowserDomainContext.kt`
- Modify: `app/src/main/java/com/lyy/keepassa/service/autofill/StructureParser.kt`
- Test: `app/src/test/java/com/lyy/keepassa/service/autofill/ImeBrowserDomainContextTest.kt`

- [ ] **Step 1: Write the failing domain context tests**

Create `app/src/test/java/com/lyy/keepassa/service/autofill/ImeBrowserDomainContextTest.kt`:

```kotlin
package com.lyy.keepassa.service.autofill

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImeBrowserDomainContextTest {

  private var now = 10_000L

  @After fun tearDown() {
    ImeBrowserDomainContext.resetForTest()
  }

  @Test fun resolve_samePackageWithinTtl_returnsDomain() {
    ImeBrowserDomainContext.setClockForTest { now }
    ImeBrowserDomainContext.remember(
      browserPackage = "com.android.chrome",
      domain = "example.com",
      source = ImeBrowserDomainContext.Source.WEB_DOMAIN
    )

    assertEquals("example.com", ImeBrowserDomainContext.resolve("com.android.chrome"))
  }

  @Test fun resolve_differentPackage_returnsNull() {
    ImeBrowserDomainContext.setClockForTest { now }
    ImeBrowserDomainContext.remember(
      browserPackage = "com.android.chrome",
      domain = "example.com",
      source = ImeBrowserDomainContext.Source.ADDRESS_BAR
    )

    assertNull(ImeBrowserDomainContext.resolve("com.vivaldi.browser"))
  }

  @Test fun resolve_expiredContext_returnsNull() {
    ImeBrowserDomainContext.setClockForTest { now }
    ImeBrowserDomainContext.remember(
      browserPackage = "com.android.chrome",
      domain = "example.com",
      source = ImeBrowserDomainContext.Source.WEB_DOMAIN
    )

    now += ImeBrowserDomainContext.TTL_MS + 1

    assertNull(ImeBrowserDomainContext.resolve("com.android.chrome"))
  }

  @Test fun remember_blankDomain_clearsContext() {
    ImeBrowserDomainContext.setClockForTest { now }
    ImeBrowserDomainContext.remember("com.android.chrome", "example.com", ImeBrowserDomainContext.Source.WEB_DOMAIN)

    ImeBrowserDomainContext.remember("com.android.chrome", "", ImeBrowserDomainContext.Source.WEB_DOMAIN)

    assertNull(ImeBrowserDomainContext.resolve("com.android.chrome"))
  }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```powershell
.\gradlew.bat :app:testDevDebugUnitTest --tests com.lyy.keepassa.service.autofill.ImeBrowserDomainContextTest
```

Expected: `BUILD FAILED` with unresolved reference `ImeBrowserDomainContext`.

- [ ] **Step 3: Add the domain context implementation**

Create `app/src/main/java/com/lyy/keepassa/service/autofill/ImeBrowserDomainContext.kt`:

```kotlin
package com.lyy.keepassa.service.autofill

internal object ImeBrowserDomainContext {
  const val TTL_MS = 60_000L

  enum class Source {
    WEB_DOMAIN,
    ADDRESS_BAR,
    HTML_VALUE
  }

  data class Snapshot(
    val browserPackage: String,
    val domain: String,
    val updatedAt: Long,
    val source: Source
  )

  private var clock: () -> Long = { System.currentTimeMillis() }
  private var snapshot: Snapshot? = null

  fun remember(
    browserPackage: String,
    domain: String,
    source: Source
  ) {
    val normalizedPackage = browserPackage.trim()
    val normalizedDomain = domain.trim()
    if (normalizedPackage.isEmpty() || normalizedDomain.isEmpty()) {
      clear()
      return
    }
    snapshot = Snapshot(
      browserPackage = normalizedPackage,
      domain = normalizedDomain,
      updatedAt = clock(),
      source = source
    )
  }

  fun resolve(browserPackage: String?): String? {
    val current = snapshot ?: return null
    if (browserPackage.isNullOrBlank()) return null
    if (!current.browserPackage.equals(browserPackage.trim(), ignoreCase = true)) return null
    if (clock() - current.updatedAt > TTL_MS) return null
    return current.domain
  }

  fun clear() {
    snapshot = null
  }

  fun setClockForTest(clock: () -> Long) {
    this.clock = clock
  }

  fun resetForTest() {
    clock = { System.currentTimeMillis() }
    snapshot = null
  }
}
```

- [ ] **Step 4: Wire domain publication in StructureParser**

Modify `app/src/main/java/com/lyy/keepassa/service/autofill/StructureParser.kt` at each place that currently assigns both `domainUrl` and `W3cHints.curDomainUrl`.

For `parseLocked`, after:

```kotlin
domainUrl = domain
W3cHints.curDomainUrl = domainUrl
Timber.d("domainUrl = $domainUrl")
```

add:

```kotlin
ImeBrowserDomainContext.remember(
  browserPackage = pkgName,
  domain = domainUrl,
  source = ImeBrowserDomainContext.Source.WEB_DOMAIN
)
```

For `rememberBrowserAddressFieldDomain`, after:

```kotlin
domainUrl = domain
W3cHints.curDomainUrl = domain
Timber.d("domainUrl = $domainUrl")
```

add:

```kotlin
ImeBrowserDomainContext.remember(
  browserPackage = pkgName,
  domain = domainUrl,
  source = ImeBrowserDomainContext.Source.ADDRESS_BAR
)
```

For `innerAppWebView`, after:

```kotlin
domainUrl = domain
W3cHints.curDomainUrl = domainUrl
Timber.d("domainUrl = $domainUrl")
```

add:

```kotlin
ImeBrowserDomainContext.remember(
  browserPackage = pkgName,
  domain = domainUrl,
  source = ImeBrowserDomainContext.Source.WEB_DOMAIN
)
```

- [ ] **Step 5: Run tests to verify they pass**

Run:

```powershell
.\gradlew.bat :app:testDevDebugUnitTest --tests com.lyy.keepassa.service.autofill.ImeBrowserDomainContextTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit domain context**

Run:

```powershell
git add -- app/src/main/java/com/lyy/keepassa/service/autofill/ImeBrowserDomainContext.kt app/src/main/java/com/lyy/keepassa/service/autofill/StructureParser.kt app/src/test/java/com/lyy/keepassa/service/autofill/ImeBrowserDomainContextTest.kt
git commit -m "feat: add ime browser domain context"
```

Expected: one commit containing only the files listed in this task.

## Task 3: IME-Safe Entry Search and Ranking

**Files:**
- Create: `app/src/main/java/com/lyy/keepassa/service/input/search/ImeEntrySearchEngine.kt`
- Test: `app/src/test/java/com/lyy/keepassa/service/input/search/ImeEntrySearchEngineTest.kt`

- [ ] **Step 1: Write the failing search engine tests**

Create `app/src/test/java/com/lyy/keepassa/service/input/search/ImeEntrySearchEngineTest.kt`:

```kotlin
package com.lyy.keepassa.service.input.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImeEntrySearchEngineTest {

  @Test fun searchCandidates_ranksTitleBeforeUrlBeforeUsernameBeforeNotes() {
    val candidates = listOf(
      candidate("notes", title = "Alpha", notes = "github"),
      candidate("username", title = "Alpha", username = "github-user"),
      candidate("url", title = "Alpha", url = "https://github.com/login"),
      candidate("titleContains", title = "My GitHub"),
      candidate("titlePrefix", title = "GitHub Work"),
      candidate("titleExact", title = "github")
    )

    val result = ImeEntrySearchEngine.searchCandidates("github", candidates)

    assertEquals(
      listOf("titleExact", "titlePrefix", "titleContains", "url", "username", "notes"),
      result
    )
  }

  @Test fun searchCandidates_preservesOriginalOrderForSameRank() {
    val candidates = listOf(
      candidate("first", title = "GitHub one"),
      candidate("second", title = "GitHub two")
    )

    assertEquals(listOf("first", "second"), ImeEntrySearchEngine.searchCandidates("github", candidates))
  }

  @Test fun searchCandidates_limitsToTenResults() {
    val candidates = (0 until 12).map { index ->
      candidate("item-$index", title = "GitHub $index")
    }

    val result = ImeEntrySearchEngine.searchCandidates("github", candidates)

    assertEquals(10, result.size)
    assertEquals("item-0", result.first())
    assertEquals("item-9", result.last())
  }

  @Test fun allowedField_excludesPasswordProtectedAndTotpFields() {
    assertFalse(ImeEntrySearchEngine.isAllowedCustomField("Password", protected = false))
    assertFalse(ImeEntrySearchEngine.isAllowedCustomField("TOTP Seed", protected = false))
    assertFalse(ImeEntrySearchEngine.isAllowedCustomField("otp", protected = false))
    assertFalse(ImeEntrySearchEngine.isAllowedCustomField("Custom", protected = true))
    assertTrue(ImeEntrySearchEngine.isAllowedCustomField("CustomerId", protected = false))
  }

  @Test fun searchCandidates_matchesAllowedCustomFieldsLast() {
    val candidates = listOf(
      candidate("custom", title = "Alpha", customFields = listOf("github")),
      candidate("username", title = "Alpha", username = "github")
    )

    val result = ImeEntrySearchEngine.searchCandidates("github", candidates)

    assertEquals(listOf("username", "custom"), result)
  }

  private fun candidate(
    value: String,
    title: String,
    username: String = "",
    url: String = "",
    notes: String = "",
    customFields: List<String> = emptyList()
  ) = ImeEntrySearchEngine.Candidate(
    value = value,
    title = title,
    username = username,
    url = url,
    notes = notes,
    customFields = customFields
  )
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```powershell
.\gradlew.bat :app:testDevDebugUnitTest --tests com.lyy.keepassa.service.input.search.ImeEntrySearchEngineTest
```

Expected: `BUILD FAILED` with unresolved reference `ImeEntrySearchEngine`.

- [ ] **Step 3: Add the IME search engine**

Create `app/src/main/java/com/lyy/keepassa/service/input/search/ImeEntrySearchEngine.kt`:

```kotlin
package com.lyy.keepassa.service.input.search

import com.keepassdroid.database.PwEntry
import com.keepassdroid.database.PwEntryV4
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.util.getRealUserName

internal object ImeEntrySearchEngine {
  private const val MAX_RESULTS = 10
  private val blockedFieldNames = setOf(
    "password",
    "pwd",
    "pass",
    "otp",
    "totp",
    "totp seed",
    "timeotp secret base32",
    "timeotp secret base64",
    "timeotp secret hex",
    "hmacotp secret base32",
    "hmacotp secret base64",
    "hmacotp secret hex"
  )

  data class Candidate<T>(
    val value: T,
    val title: String,
    val username: String = "",
    val url: String = "",
    val notes: String = "",
    val customFields: List<String> = emptyList()
  )

  fun search(query: String): List<PwEntry> {
    val pm = BaseApp.KDB?.pm ?: return emptyList()
    val candidates = pm.entries.values.mapNotNull { entry ->
      val pwEntry = entry as? PwEntry ?: return@mapNotNull null
      Candidate(
        value = pwEntry,
        title = pwEntry.title.orEmpty(),
        username = pwEntry.getRealUserName(),
        url = pwEntry.url.orEmpty(),
        notes = when (pwEntry) {
          is PwEntryV4 -> pwEntry.notes.orEmpty()
          else -> ""
        },
        customFields = allowedCustomFieldValues(pwEntry)
      )
    }
    return searchCandidates(query, candidates)
  }

  fun <T> searchCandidates(
    query: String,
    candidates: List<Candidate<T>>
  ): List<T> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return emptyList()
    return candidates.mapIndexedNotNull { index, candidate ->
      val rank = rank(candidate, normalizedQuery) ?: return@mapIndexedNotNull null
      RankedCandidate(index, rank, candidate.value)
    }
      .sortedWith(compareBy<RankedCandidate<T>> { it.rank }.thenBy { it.index })
      .take(MAX_RESULTS)
      .map { it.value }
  }

  fun isAllowedCustomField(
    key: String,
    protected: Boolean
  ): Boolean {
    if (protected) return false
    val normalized = key.trim().lowercase()
    if (normalized.isEmpty()) return false
    if (blockedFieldNames.contains(normalized)) return false
    if (normalized.contains("password")) return false
    if (normalized.contains("totp")) return false
    if (normalized.contains("otp")) return false
    return true
  }

  private fun <T> rank(
    candidate: Candidate<T>,
    query: String
  ): Int? {
    val title = candidate.title
    return when {
      title.equals(query, ignoreCase = true) -> 0
      title.startsWith(query, ignoreCase = true) -> 1
      title.contains(query, ignoreCase = true) -> 2
      candidate.url.contains(query, ignoreCase = true) -> 3
      candidate.username.contains(query, ignoreCase = true) -> 4
      candidate.notes.contains(query, ignoreCase = true) -> 5
      candidate.customFields.any { it.contains(query, ignoreCase = true) } -> 5
      else -> null
    }
  }

  private fun allowedCustomFieldValues(entry: PwEntry): List<String> {
    if (entry !is PwEntryV4) return emptyList()
    return entry.strings.mapNotNull { item ->
      val key = item.key ?: return@mapNotNull null
      if (!isAllowedCustomField(key, item.value?.isProtected == true)) return@mapNotNull null
      item.value?.toString()
    }
  }

  private data class RankedCandidate<T>(
    val index: Int,
    val rank: Int,
    val value: T
  )
}
```

- [ ] **Step 4: Run the search tests**

Run:

```powershell
.\gradlew.bat :app:testDevDebugUnitTest --tests com.lyy.keepassa.service.input.search.ImeEntrySearchEngineTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit IME search engine**

Run:

```powershell
git add -- app/src/main/java/com/lyy/keepassa/service/input/search/ImeEntrySearchEngine.kt app/src/test/java/com/lyy/keepassa/service/input/search/ImeEntrySearchEngineTest.kt
git commit -m "feat: add ime entry search ranking"
```

Expected: one commit containing only the files listed in this task.

## Task 4: Search Session and Manual Selection Policy

**Files:**
- Create: `app/src/main/java/com/lyy/keepassa/service/input/search/ImeSearchSession.kt`
- Create: `app/src/main/java/com/lyy/keepassa/service/input/ImeManualSelectionPolicy.kt`
- Test: `app/src/test/java/com/lyy/keepassa/service/input/search/ImeSearchSessionTest.kt`
- Test: `app/src/test/java/com/lyy/keepassa/service/input/ImeManualSelectionPolicyTest.kt`

- [ ] **Step 1: Write failing tests for search session**

Create `app/src/test/java/com/lyy/keepassa/service/input/search/ImeSearchSessionTest.kt`:

```kotlin
package com.lyy.keepassa.service.input.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImeSearchSessionTest {

  @Test fun debounceConstant_is250Milliseconds() {
    assertEquals(250L, ImeSearchSession.DEBOUNCE_MS)
  }

  @Test fun updateResults_selectsFirstResultByDefault() {
    val session = ImeSearchSession<String>()

    session.updateResults(listOf("a", "b"))

    assertEquals("a", session.selected)
    assertEquals(listOf("a", "b"), session.results)
    assertFalse(session.isEmptyStateVisible)
  }

  @Test fun updateResults_emptyShowsEmptyStateWhenQueryIsNotBlank() {
    val session = ImeSearchSession<String>()
    session.setQuery("github")

    session.updateResults(emptyList())

    assertNull(session.selected)
    assertTrue(session.isEmptyStateVisible)
  }

  @Test fun clearQuery_clearsResultsAndSelection() {
    val session = ImeSearchSession<String>()
    session.setQuery("github")
    session.updateResults(listOf("a"))

    session.clear()

    assertEquals("", session.query)
    assertTrue(session.results.isEmpty())
    assertNull(session.selected)
    assertFalse(session.isEmptyStateVisible)
  }

  @Test fun select_existingResult_updatesSelected() {
    val session = ImeSearchSession<String>()
    session.updateResults(listOf("a", "b"))

    assertTrue(session.select("b"))

    assertEquals("b", session.selected)
  }
}
```

- [ ] **Step 2: Write failing tests for manual selection policy**

Create `app/src/test/java/com/lyy/keepassa/service/input/ImeManualSelectionPolicyTest.kt`:

```kotlin
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
```

- [ ] **Step 3: Run tests to verify they fail**

Run:

```powershell
.\gradlew.bat :app:testDevDebugUnitTest --tests com.lyy.keepassa.service.input.search.ImeSearchSessionTest --tests com.lyy.keepassa.service.input.ImeManualSelectionPolicyTest
```

Expected: `BUILD FAILED` with unresolved references for `ImeSearchSession` and `ImeManualSelectionPolicy`.

- [ ] **Step 4: Add the search session implementation**

Create `app/src/main/java/com/lyy/keepassa/service/input/search/ImeSearchSession.kt`:

```kotlin
package com.lyy.keepassa.service.input.search

internal class ImeSearchSession<T> {
  companion object {
    const val DEBOUNCE_MS = 250L
  }

  var query: String = ""
  private set

  var results: List<T> = emptyList()
    private set

  var selected: T? = null
    private set

  val isEmptyStateVisible: Boolean
    get() = query.trim().isNotEmpty() && results.isEmpty()

  fun setQuery(value: String) {
    query = value
  }

  fun appendToQuery(value: String) {
    query += value
  }

  fun backspaceQuery() {
    if (query.isNotEmpty()) {
      query = query.dropLast(1)
    }
  }

  fun updateResults(results: List<T>) {
    this.results = results
    selected = results.firstOrNull()
  }

  fun select(item: T): Boolean {
    if (!results.contains(item)) return false
    selected = item
    return true
  }

  fun clear() {
    query = ""
    results = emptyList()
    selected = null
  }
}
```

- [ ] **Step 5: Add the manual selection policy implementation**

Create `app/src/main/java/com/lyy/keepassa/service/input/ImeManualSelectionPolicy.kt`:

```kotlin
package com.lyy.keepassa.service.input

internal class ImeManualSelectionPolicy<T> {
  private var packageName: String? = null

  var currentSelection: T? = null
    private set

  fun rememberManualSelection(
    packageName: String?,
    selection: T
  ) {
    this.packageName = packageName?.trim()?.takeIf { it.isNotEmpty() }
    currentSelection = selection
  }

  fun onStartInput(newPackageName: String?) {
    val currentPackage = packageName
    if (currentPackage == null) return
    if (!currentPackage.equals(newPackageName?.trim(), ignoreCase = true)) {
      clear()
    }
  }

  fun hasManualSelectionFor(packageName: String?): Boolean {
    val currentPackage = this.packageName ?: return false
    return currentSelection != null && currentPackage.equals(packageName?.trim(), ignoreCase = true)
  }

  fun shouldUseAutomaticCandidates(packageName: String?): Boolean =
    !hasManualSelectionFor(packageName)

  fun onNewSearch() {
    clear()
  }

  fun clear() {
    packageName = null
    currentSelection = null
  }
}
```

- [ ] **Step 6: Run tests**

Run:

```powershell
.\gradlew.bat :app:testDevDebugUnitTest --tests com.lyy.keepassa.service.input.search.ImeSearchSessionTest --tests com.lyy.keepassa.service.input.ImeManualSelectionPolicyTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit search session and manual selection policy**

Run:

```powershell
git add -- app/src/main/java/com/lyy/keepassa/service/input/search/ImeSearchSession.kt app/src/main/java/com/lyy/keepassa/service/input/ImeManualSelectionPolicy.kt app/src/test/java/com/lyy/keepassa/service/input/search/ImeSearchSessionTest.kt app/src/test/java/com/lyy/keepassa/service/input/ImeManualSelectionPolicyTest.kt
git commit -m "feat: add ime search session policy"
```

Expected: one commit containing only the files listed in this task.

## Task 5: Keyboard Haptic Preference

**Files:**
- Create: `app/src/main/java/com/lyy/keepassa/service/input/keyboard/ImeKeyboardPreferences.kt`
- Modify: `app/src/main/res/xml/app_setting.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-zh-rCN/strings.xml`
- Test: `app/src/test/java/com/lyy/keepassa/view/setting/AppSettingImeKeyboardPreferenceTest.kt`

- [ ] **Step 1: Write the failing settings source test**

Create `app/src/test/java/com/lyy/keepassa/view/setting/AppSettingImeKeyboardPreferenceTest.kt`:

```kotlin
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
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```powershell
.\gradlew.bat :app:testDevDebugUnitTest --tests com.lyy.keepassa.view.setting.AppSettingImeKeyboardPreferenceTest
```

Expected: `BUILD FAILED` or assertion failure because the haptic preference does not exist.

- [ ] **Step 3: Add strings**

Modify `app/src/main/res/values/strings.xml`:

```xml
<string name="set_key_ime_keyboard_haptic_feedback"><![CDATA[set_key_ime_keyboard_haptic_feedback]]></string>
<string name="set_title_ime_keyboard_haptic_feedback"><![CDATA[Keyboard haptic feedback]]></string>
<string name="set_summary_ime_keyboard_haptic_feedback_on"><![CDATA[Vibrate lightly when using the KeePassA keyboard]]></string>
<string name="set_summary_ime_keyboard_haptic_feedback_off"><![CDATA[Do not vibrate when using the KeePassA keyboard]]></string>
<string name="ime_search_entry_hint"><![CDATA[Search entries]]></string>
<string name="ime_search_no_entry"><![CDATA[No matching entries]]></string>
<string name="ime_search_select_entry_first"><![CDATA[Select an entry first]]></string>
```

Modify `app/src/main/res/values-zh-rCN/strings.xml`:

```xml
<string name="set_key_ime_keyboard_haptic_feedback">set_key_ime_keyboard_haptic_feedback</string>
<string name="set_title_ime_keyboard_haptic_feedback">键盘震动反馈</string>
<string name="set_summary_ime_keyboard_haptic_feedback_on">使用 KeePassA 键盘时轻微震动</string>
<string name="set_summary_ime_keyboard_haptic_feedback_off">使用 KeePassA 键盘时不震动</string>
<string name="ime_search_entry_hint">搜索条目</string>
<string name="ime_search_no_entry">无匹配条目</string>
<string name="ime_search_select_entry_first">请先选择条目</string>
```

- [ ] **Step 4: Add the haptic preference under secure keyboard**

Modify `app/src/main/res/xml/app_setting.xml` immediately after the `Preference` with `app:key="@string/set_key_open_kpa_ime"`:

```xml
    <SwitchPreference
        android:defaultValue="true"
        android:title="@string/set_title_ime_keyboard_haptic_feedback"
        app:icon="@drawable/ic_keyboard"
        app:key="@string/set_key_ime_keyboard_haptic_feedback"
        app:summaryOff="@string/set_summary_ime_keyboard_haptic_feedback_off"
        app:summaryOn="@string/set_summary_ime_keyboard_haptic_feedback_on" />
```

- [ ] **Step 5: Add preference reader**

Create `app/src/main/java/com/lyy/keepassa/service/input/keyboard/ImeKeyboardPreferences.kt`:

```kotlin
package com.lyy.keepassa.service.input.keyboard

import android.content.Context
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.preference.PreferenceManager
import com.lyy.keepassa.R

internal class ImeKeyboardPreferences(
  private val context: Context
) {
  fun hapticFeedbackEnabled(): Boolean =
    PreferenceManager.getDefaultSharedPreferences(context)
      .getBoolean(context.getString(R.string.set_key_ime_keyboard_haptic_feedback), true)

  fun performKeyboardHaptic(view: View) {
    if (!hapticFeedbackEnabled()) return
    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
  }
}
```

- [ ] **Step 6: Run the settings test**

Run:

```powershell
.\gradlew.bat :app:testDevDebugUnitTest --tests com.lyy.keepassa.view.setting.AppSettingImeKeyboardPreferenceTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit haptic setting**

Run:

```powershell
git add -- app/src/main/java/com/lyy/keepassa/service/input/keyboard/ImeKeyboardPreferences.kt app/src/main/res/xml/app_setting.xml app/src/main/res/values/strings.xml app/src/main/res/values-zh-rCN/strings.xml app/src/test/java/com/lyy/keepassa/view/setting/AppSettingImeKeyboardPreferenceTest.kt
git commit -m "feat: add ime keyboard haptic setting"
```

Expected: one commit containing only the files listed in this task.

## Task 6: IME Layout, Candidate Item, and Keyboard Binder

**Files:**
- Create: `app/src/main/java/com/lyy/keepassa/service/input/keyboard/ImeKeyboardViewBinder.kt`
- Modify: `app/src/main/res/layout/layout_kpa_ime.xml`
- Modify: `app/src/main/res/layout/item_ime_entry.xml`
- Modify: `app/src/main/java/com/lyy/keepassa/service/input/CandidatesAdapter.kt`
- Test: `app/src/test/java/com/lyy/keepassa/service/input/InputIMEServiceSourceTest.kt`

- [ ] **Step 1: Write the failing source structure test**

Create `app/src/test/java/com/lyy/keepassa/service/input/InputIMEServiceSourceTest.kt`:

```kotlin
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
```

- [ ] **Step 2: Run the source test to verify it fails**

Run:

```powershell
.\gradlew.bat :app:testDevDebugUnitTest --tests com.lyy.keepassa.service.input.InputIMEServiceSourceTest
```

Expected: `BUILD FAILED` or assertion failure because the layout and adapter have not been updated.

- [ ] **Step 3: Add search and keyboard containers to IME layout**

Modify `app/src/main/res/layout/layout_kpa_ime.xml` so the top of the root `ConstraintLayout` contains this search bar below `line`:

```xml
  <LinearLayout
      android:id="@+id/imeSearchBar"
      android:layout_width="0dp"
      android:layout_height="36dp"
      android:layout_marginStart="8dp"
      android:layout_marginTop="8dp"
      android:layout_marginEnd="8dp"
      android:background="@drawable/bg_white_radius_2"
      android:clickable="true"
      android:focusable="true"
      android:gravity="center_vertical"
      android:orientation="horizontal"
      android:paddingStart="12dp"
      android:paddingEnd="8dp"
      app:layout_constraintEnd_toEndOf="parent"
      app:layout_constraintStart_toStartOf="parent"
      app:layout_constraintTop_toBottomOf="@+id/line">

    <androidx.appcompat.widget.AppCompatImageView
        android:id="@+id/ivImeSearchIcon"
        android:layout_width="20dp"
        android:layout_height="20dp"
        app:srcCompat="@drawable/ic_search" />

    <TextView
        android:id="@+id/tvImeSearchQuery"
        style="@style/KpaContentTextStyle"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_marginStart="8dp"
        android:layout_weight="1"
        android:ellipsize="end"
        android:maxLines="1"
        android:text="@string/ime_search_entry_hint" />

    <androidx.appcompat.widget.AppCompatImageView
        android:id="@+id/btImeSearchClear"
        android:layout_width="32dp"
        android:layout_height="32dp"
        android:padding="6dp"
        android:visibility="gone"
        app:srcCompat="@drawable/ic_ime_close" />
  </LinearLayout>
```

Move `rvContent` so `app:layout_constraintTop_toBottomOf="@+id/imeSearchBar"`.

Add the keyboard container below `btEnter`:

```xml
  <LinearLayout
      android:id="@+id/llImeKeyboard"
      android:layout_width="0dp"
      android:layout_height="wrap_content"
      android:layout_marginStart="6dp"
      android:layout_marginTop="8dp"
      android:layout_marginEnd="6dp"
      android:orientation="vertical"
      app:layout_constraintEnd_toEndOf="parent"
      app:layout_constraintStart_toStartOf="parent"
      app:layout_constraintTop_toBottomOf="@+id/btChangeIme" />
```

- [ ] **Step 4: Update candidate item layout**

Modify `app/src/main/res/layout/item_ime_entry.xml` root and text area:

```xml
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="160dp"
    android:layout_height="50dp"
    android:background="@drawable/bg_ime_entry"
    android:padding="8dp">

  <androidx.appcompat.widget.AppCompatImageView
      android:id="@+id/icon"
      android:layout_width="24dp"
      android:layout_height="24dp"
      android:transitionName="@string/transition_entry_icon"
      app:layout_constraintBottom_toBottomOf="parent"
      app:layout_constraintStart_toStartOf="parent"
      app:layout_constraintTop_toTopOf="parent"
      app:srcCompat="@drawable/ic_android" />

  <TextView
      android:id="@+id/text"
      style="@style/KpaContentTextStyle"
      android:layout_width="0dp"
      android:layout_height="wrap_content"
      android:layout_marginStart="8dp"
      android:ellipsize="end"
      android:maxLines="1"
      android:textColor="@color/text_black_color"
      app:layout_constraintEnd_toEndOf="parent"
      app:layout_constraintStart_toEndOf="@+id/icon"
      app:layout_constraintTop_toTopOf="parent" />

  <TextView
      android:id="@+id/subtitle"
      style="@style/KpaContentTextStyle"
      android:layout_width="0dp"
      android:layout_height="wrap_content"
      android:layout_marginStart="8dp"
      android:ellipsize="end"
      android:maxLines="1"
      android:textColor="@color/text_gray_color"
      android:textSize="12sp"
      app:layout_constraintEnd_toEndOf="parent"
      app:layout_constraintStart_toEndOf="@+id/icon"
      app:layout_constraintTop_toBottomOf="@+id/text" />
</androidx.constraintlayout.widget.ConstraintLayout>
```

- [ ] **Step 5: Update CandidatesAdapter for subtitle and empty state**

Modify `app/src/main/java/com/lyy/keepassa/service/input/CandidatesAdapter.kt`:

```kotlin
package com.lyy.keepassa.service.input

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.arialyy.frame.util.adapter.AbsHolder
import com.arialyy.frame.util.adapter.AbsRVAdapter
import com.keepassdroid.database.PwEntry
import com.lyy.keepassa.R
import com.lyy.keepassa.entity.SimpleItemEntity
import com.lyy.keepassa.service.input.CandidatesAdapter.Holder
import com.lyy.keepassa.util.IconUtil

class CandidatesAdapter(
  context: Context,
  data: List<SimpleItemEntity>
) : AbsRVAdapter<SimpleItemEntity, Holder>(context, data) {

  companion object {
    const val ITEM_TYPE_EMPTY = -10
  }

  override fun getViewHolder(convertView: View, viewType: Int): Holder = Holder(convertView)

  override fun setLayoutId(type: Int): Int = R.layout.item_ime_entry

  override fun bindData(holder: Holder, position: Int, item: SimpleItemEntity) {
    if (item.type == ITEM_TYPE_EMPTY) {
      holder.icon.visibility = View.GONE
      holder.text.text = item.title
      holder.subtitle.visibility = View.GONE
      holder.itemView.isSelected = false
      return
    }

    holder.icon.visibility = View.VISIBLE
    val entry = item.obj as? PwEntry
    if (entry != null) {
      IconUtil.setEntryIcon(entry, holder.icon)
    } else {
      holder.icon.setImageResource(R.drawable.ic_android)
    }
    holder.text.text = item.title
    val subtitle = item.subTitle?.toString().orEmpty()
    holder.subtitle.visibility = if (subtitle.isBlank()) View.GONE else View.VISIBLE
    holder.subtitle.text = subtitle
    holder.itemView.isSelected = item.isSelected
  }

  class Holder(view: View) : AbsHolder(view) {
    val icon: ImageView = view.findViewById(R.id.icon)
    val text: TextView = view.findViewById(R.id.text)
    val subtitle: TextView = view.findViewById(R.id.subtitle)
  }
}
```

- [ ] **Step 6: Add keyboard view binder**

Create `app/src/main/java/com/lyy/keepassa/service/input/keyboard/ImeKeyboardViewBinder.kt`:

```kotlin
package com.lyy.keepassa.service.input.keyboard

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.lyy.keepassa.R

internal class ImeKeyboardViewBinder(
  private val context: Context,
  private val container: LinearLayout,
  private val onKey: (View, ImeKeyAction) -> Unit,
  private val onShiftLongPress: (View) -> Unit
) {

  fun render(page: ImeKeyboardPage) {
    container.removeAllViews()
    val rows = when (page) {
      ImeKeyboardPage.ALPHABET -> ImeKeyboardLayout.alphabetRows()
      ImeKeyboardPage.SYMBOLS -> ImeKeyboardLayout.symbolRows()
    }
    rows.forEach { row ->
      container.addView(createRow(row))
    }
  }

  private fun createRow(actions: List<ImeKeyAction>): View {
    return LinearLayout(context).apply {
      orientation = LinearLayout.HORIZONTAL
      gravity = Gravity.CENTER
      layoutParams = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      actions.forEach { action ->
        addView(createKey(action))
      }
    }
  }

  private fun createKey(action: ImeKeyAction): View {
    return TextView(context).apply {
      text = label(action)
      gravity = Gravity.CENTER
      minHeight = context.resources.getDimensionPixelSize(R.dimen.ime_key_height)
      setBackgroundResource(R.drawable.bg_ime_key)
      setTextColor(context.getColor(R.color.color_4E85DB))
      textSize = 16f
      isClickable = true
      isFocusable = true
      layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight(action)).apply {
        setMargins(3, 3, 3, 3)
      }
      setOnClickListener { onKey(this, action) }
      if (action == ImeKeyAction.Shift) {
        setOnLongClickListener {
          onShiftLongPress(this)
          true
        }
      }
    }
  }

  private fun label(action: ImeKeyAction): String = when (action) {
    is ImeKeyAction.CommitText -> action.text
    ImeKeyAction.Space -> " "
    ImeKeyAction.Backspace -> "del"
    ImeKeyAction.Enter -> "enter"
    ImeKeyAction.Shift -> "shift"
    ImeKeyAction.SwitchToSymbols -> "123"
    ImeKeyAction.SwitchToAlphabet -> "ABC"
    ImeKeyAction.EnterSearchMode -> "search"
    ImeKeyAction.ClearSearch -> "clear"
    ImeKeyAction.ExitSearchMode -> "close"
  }

  private fun weight(action: ImeKeyAction): Float = when (action) {
    ImeKeyAction.Space -> 4f
    ImeKeyAction.Backspace,
    ImeKeyAction.Enter,
    ImeKeyAction.SwitchToSymbols,
    ImeKeyAction.SwitchToAlphabet,
    ImeKeyAction.Shift -> 1.5f
    else -> 1f
  }
}
```

Add this dimension to `app/src/main/res/values/dimens.xml` if it does not exist:

```xml
<dimen name="ime_key_height">40dp</dimen>
```

- [ ] **Step 7: Run source test**

Run:

```powershell
.\gradlew.bat :app:testDevDebugUnitTest --tests com.lyy.keepassa.service.input.InputIMEServiceSourceTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Commit layout and binder**

Run:

```powershell
git add -- app/src/main/java/com/lyy/keepassa/service/input/keyboard/ImeKeyboardViewBinder.kt app/src/main/java/com/lyy/keepassa/service/input/CandidatesAdapter.kt app/src/main/res/layout/layout_kpa_ime.xml app/src/main/res/layout/item_ime_entry.xml app/src/main/res/values/dimens.xml app/src/test/java/com/lyy/keepassa/service/input/InputIMEServiceSourceTest.kt
git commit -m "feat: add ime keyboard layout"
```

Expected: one commit containing only the files listed in this task.

## Task 7: Wire InputIMEService

**Files:**
- Modify: `app/src/main/java/com/lyy/keepassa/service/input/InputIMEService.kt`
- Modify: `app/src/test/java/com/lyy/keepassa/service/input/InputIMEServiceSourceTest.kt`

- [ ] **Step 1: Extend the source test for wiring boundaries**

Append these tests to `app/src/test/java/com/lyy/keepassa/service/input/InputIMEServiceSourceTest.kt`:

```kotlin
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
```

- [ ] **Step 2: Run the source test to verify it fails**

Run:

```powershell
.\gradlew.bat :app:testDevDebugUnitTest --tests com.lyy.keepassa.service.input.InputIMEServiceSourceTest
```

Expected: assertion failure because `InputIMEService` is not wired to the new components.

- [ ] **Step 3: Add imports and fields to InputIMEService**

Modify `app/src/main/java/com/lyy/keepassa/service/input/InputIMEService.kt` imports:

```kotlin
import android.widget.LinearLayout
import com.lyy.keepassa.service.autofill.ImeBrowserDomainContext
import com.lyy.keepassa.service.input.keyboard.ImeKeyAction
import com.lyy.keepassa.service.input.keyboard.ImeKeyboardPage
import com.lyy.keepassa.service.input.keyboard.ImeKeyboardPreferences
import com.lyy.keepassa.service.input.keyboard.ImeKeyboardState
import com.lyy.keepassa.service.input.keyboard.ImeKeyboardViewBinder
import com.lyy.keepassa.service.input.search.ImeEntrySearchEngine
import com.lyy.keepassa.service.input.search.ImeSearchSession
import com.lyy.keepassa.util.getRealUserName
import kotlinx.coroutines.Job
```

Add fields:

```kotlin
private val keyboardState = ImeKeyboardState()
private val searchSession = ImeSearchSession<PwEntry>()
private val manualSelectionPolicy = ImeManualSelectionPolicy<PwEntry>()
private lateinit var keyboardPreferences: ImeKeyboardPreferences
private var keyboardBinder: ImeKeyboardViewBinder? = null
private var imeSearchJob: Job? = null
private var isShowingSearchResults = false
```

- [ ] **Step 4: Initialize search bar and keyboard binder**

Inside `onCreateInputView()` after `curImeView = layout`, add:

```kotlin
keyboardPreferences = ImeKeyboardPreferences(this)
initImeSearchBar(layout)
initKeyboard(layout)
```

Add these methods:

```kotlin
private fun initImeSearchBar(layout: View) {
  val searchBar = layout.findViewById<View>(R.id.imeSearchBar)
  val clear = layout.findViewById<View>(R.id.btImeSearchClear)
  searchBar.setOnClickListener {
    keyboardPreferences.performKeyboardHaptic(searchBar)
    enterImeSearchMode()
  }
  clear.setOnClickListener {
    keyboardPreferences.performKeyboardHaptic(clear)
    if (keyboardState.searchQuery.isEmpty()) {
      exitImeSearchMode(clearResults = true)
    } else {
      keyboardState.clearSearchQuery()
      searchSession.clear()
      updateImeSearchUi()
      showImeSearchEmptyOrResults()
    }
  }
}

private fun initKeyboard(layout: View) {
  val container = layout.findViewById<LinearLayout>(R.id.llImeKeyboard)
  keyboardBinder = ImeKeyboardViewBinder(
    context = this,
    container = container,
    onKey = { view, action -> handleImeKeyAction(view, action) },
    onShiftLongPress = { view ->
      keyboardPreferences.performKeyboardHaptic(view)
      keyboardState.longPressShift()
      renderKeyboard()
    }
  )
  renderKeyboard()
}

private fun renderKeyboard() {
  keyboardBinder?.render(keyboardState.page)
}
```

- [ ] **Step 5: Add key action routing**

Add:

```kotlin
private fun handleImeKeyAction(view: View, action: ImeKeyAction) {
  keyboardPreferences.performKeyboardHaptic(view)
  when (action) {
    is ImeKeyAction.CommitText -> {
      val text = if (keyboardState.page == ImeKeyboardPage.ALPHABET) {
        keyboardState.applyShiftTo(action.text)
      } else {
        action.text
      }
      if (keyboardState.isSearchMode) {
        handleSearchTextInput(text)
      } else {
        fillData(text)
      }
      renderKeyboard()
    }
    ImeKeyAction.Space -> {
      if (keyboardState.isSearchMode) {
        handleSearchTextInput(" ")
      } else {
        fillData(" ")
      }
    }
    ImeKeyAction.Backspace -> {
      if (keyboardState.isSearchMode) {
        keyboardState.backspaceSearchQuery()
        searchSession.setQuery(keyboardState.searchQuery)
        updateImeSearchUi()
        scheduleImeSearch()
      } else {
        ic?.deleteSurroundingText(1, 0)
      }
    }
    ImeKeyAction.Enter -> {
      if (keyboardState.isSearchMode) {
        runImeSearchNow()
      } else {
        ic?.performEditorAction(imeOption)
      }
    }
    ImeKeyAction.Shift -> {
      keyboardState.tapShift(System.currentTimeMillis())
      renderKeyboard()
    }
    ImeKeyAction.SwitchToSymbols -> {
      keyboardState.switchToSymbols()
      renderKeyboard()
    }
    ImeKeyAction.SwitchToAlphabet -> {
      keyboardState.switchToAlphabet()
      renderKeyboard()
    }
    ImeKeyAction.EnterSearchMode -> enterImeSearchMode()
    ImeKeyAction.ClearSearch -> {
      keyboardState.clearSearchQuery()
      searchSession.clear()
      updateImeSearchUi()
      showImeSearchEmptyOrResults()
    }
    ImeKeyAction.ExitSearchMode -> exitImeSearchMode(clearResults = true)
  }
}

private fun handleSearchTextInput(text: String) {
  if (!keyboardState.appendSearchText(text)) return
  searchSession.setQuery(keyboardState.searchQuery)
  updateImeSearchUi()
  scheduleImeSearch()
}
```

- [ ] **Step 6: Add search mode functions**

Add:

```kotlin
private fun enterImeSearchMode() {
  if (!dbIsOpen()) return
  manualSelectionPolicy.onNewSearch()
  keyboardState.enterSearchMode()
  searchSession.clear()
  isShowingSearchResults = true
  updateImeSearchUi()
  showImeSearchEmptyOrResults()
}

private fun exitImeSearchMode(clearResults: Boolean) {
  keyboardState.exitSearchMode()
  imeSearchJob?.cancel()
  if (clearResults) {
    searchSession.clear()
    isShowingSearchResults = false
    candidatesData.clear()
    candidatesAdapter.notifyDataSetChanged()
    candidatesList.visibility = View.GONE
  }
  updateImeSearchUi()
}

private fun updateImeSearchUi() {
  val root = curImeView ?: return
  val query = keyboardState.searchQuery
  root.findViewById<TextView>(R.id.tvImeSearchQuery).text =
    if (keyboardState.isSearchMode && query.isNotEmpty()) query else getString(R.string.ime_search_entry_hint)
  root.findViewById<View>(R.id.btImeSearchClear).visibility =
    if (keyboardState.isSearchMode) View.VISIBLE else View.GONE
}

private fun scheduleImeSearch() {
  imeSearchJob?.cancel()
  imeSearchJob = scope.launch {
    delay(ImeSearchSession.DEBOUNCE_MS)
    runImeSearchNow()
  }
}

private fun runImeSearchNow() {
  if (!keyboardState.isSearchMode) return
  val results = ImeEntrySearchEngine.search(searchSession.query)
  searchSession.updateResults(results)
  showImeSearchEmptyOrResults()
}

private fun showImeSearchEmptyOrResults() {
  candidatesData.clear()
  if (searchSession.isEmptyStateVisible) {
    candidatesList.visibility = View.VISIBLE
    candidatesData.add(SimpleItemEntity().apply {
      type = CandidatesAdapter.ITEM_TYPE_EMPTY
      title = getString(R.string.ime_search_no_entry)
    })
    candidatesAdapter.notifyDataSetChanged()
    return
  }
  val results = searchSession.results
  selectionTracker.show(results)
  if (results.isEmpty()) {
    candidatesList.visibility = View.GONE
    candidatesAdapter.notifyDataSetChanged()
    return
  }
  candidatesList.visibility = View.VISIBLE
  val flags = selectionTracker.selectedFlags()
  results.forEachIndexed { index, entry ->
    candidatesData.add(entry.toImeCandidateItem(flags[index]))
  }
  candidatesAdapter.notifyDataSetChanged()
}
```

- [ ] **Step 7: Add candidate item helper and click behavior**

Add:

```kotlin
private fun PwEntry.toImeCandidateItem(selected: Boolean): SimpleItemEntity {
  return SimpleItemEntity().also { item ->
    item.title = title
    item.subTitle = getRealUserName().ifBlank { url }
    item.obj = this
    item.isSelected = selected
  }
}
```

In the existing candidate click listener, after a successful `selectionTracker.click(position)`, add:

```kotlin
if (keyboardState.isSearchMode) {
  searchSession.results.getOrNull(position)?.let { entry ->
    searchSession.select(entry)
    manualSelectionPolicy.rememberManualSelection(appPkgName, entry)
    exitImeSearchMode(clearResults = false)
  }
}
```

- [ ] **Step 8: Respect manual selection and browser domain context in onStartInputView**

At the start of `onStartInputView`, after `appPkgName = info?.packageName`, add:

```kotlin
manualSelectionPolicy.onStartInput(appPkgName)
if (keyboardState.isSearchMode) {
  exitImeSearchMode(clearResults = manualSelectionPolicy.currentSelection == null)
}
```

Replace the final automatic call:

```kotlin
showEntryList(searchEntry(appPkgName))
```

with:

```kotlin
if (manualSelectionPolicy.shouldUseAutomaticCandidates(appPkgName)) {
  showEntryList(searchEntry(appPkgName))
} else {
  manualSelectionPolicy.currentSelection?.let { showEntryList(listOf(it), forceVisible = true) }
}
```

Change `showEntryList` signature:

```kotlin
private fun showEntryList(
  entries: List<PwEntry>,
  forceVisible: Boolean = false
)
```

Change the single-entry branch:

```kotlin
if (selectionTracker.size == 1 && !forceVisible) {
  candidatesList.visibility = View.GONE
  return
}
```

Use `entry.toImeCandidateItem(flags[i])` when building `candidatesData`.

Change browser search inside `searchEntry`:

```kotlin
if (W3cHints.isBrowser(pkgName)) {
  val domain = ImeBrowserDomainContext.resolve(pkgName)
  Timber.d("ime browser domain context available = ${domain != null}")
  KdbUtil.searchEntriesByDomain(domain, listStorage)
  return listStorage
}
```

- [ ] **Step 9: Fill selected search result and clear search on lock/destroy**

In username/password/TOTP/other-field button handling, before returning after a successful fill or dialog launch, add:

```kotlin
if (keyboardState.isSearchMode) {
  exitImeSearchMode(clearResults = false)
}
```

When the lock button clears state, add:

```kotlin
manualSelectionPolicy.clear()
keyboardState.exitSearchMode()
searchSession.clear()
isShowingSearchResults = false
```

In `onDestroy`, add:

```kotlin
imeSearchJob?.cancel()
manualSelectionPolicy.clear()
searchSession.clear()
```

- [ ] **Step 10: Run source test and focused keyboard/search tests**

Run:

```powershell
.\gradlew.bat :app:testDevDebugUnitTest --tests com.lyy.keepassa.service.input.InputIMEServiceSourceTest --tests com.lyy.keepassa.service.input.keyboard.ImeKeyboardStateTest --tests com.lyy.keepassa.service.input.keyboard.ImeKeyboardLayoutTest --tests com.lyy.keepassa.service.input.search.ImeEntrySearchEngineTest --tests com.lyy.keepassa.service.input.search.ImeSearchSessionTest --tests com.lyy.keepassa.service.input.ImeManualSelectionPolicyTest --tests com.lyy.keepassa.service.autofill.ImeBrowserDomainContextTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 11: Commit IME wiring**

Run:

```powershell
git add -- app/src/main/java/com/lyy/keepassa/service/input/InputIMEService.kt app/src/test/java/com/lyy/keepassa/service/input/InputIMEServiceSourceTest.kt
git commit -m "feat: wire ime keyboard search"
```

Expected: one commit containing only the files listed in this task.

## Task 8: Build Verification and ADB Manual Test

**Files:**
- No planned source changes.

- [ ] **Step 1: Run all focused IME and autofill tests**

Run:

```powershell
.\gradlew.bat :app:testDevDebugUnitTest --tests com.lyy.keepassa.service.input.keyboard.ImeKeyboardStateTest --tests com.lyy.keepassa.service.input.keyboard.ImeKeyboardLayoutTest --tests com.lyy.keepassa.service.input.search.ImeEntrySearchEngineTest --tests com.lyy.keepassa.service.input.search.ImeSearchSessionTest --tests com.lyy.keepassa.service.input.ImeManualSelectionPolicyTest --tests com.lyy.keepassa.service.autofill.ImeBrowserDomainContextTest --tests com.lyy.keepassa.service.input.InputIMEServiceSourceTest --tests com.lyy.keepassa.view.setting.AppSettingImeKeyboardPreferenceTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Run the existing browser/autofill regression tests touched by previous work**

Run:

```powershell
.\gradlew.bat :app:testDevDebugUnitTest --tests com.lyy.keepassa.service.autofill.BrowserThirdPartyAutofillSupportTest --tests com.lyy.keepassa.service.autofill.BrowserAutofillStrategyTest --tests com.lyy.keepassa.service.autofill.W3cHintsCompatBrowserTest --tests com.lyy.keepassa.service.autofill.AutoFillServiceSourceTest --tests com.lyy.keepassa.view.setting.AppSettingBrowserAutofillPreferenceTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Assemble dev debug APK**

Run:

```powershell
.\gradlew.bat :app:assembleDevDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Install APK on the connected device**

Run:

```powershell
adb -s 192.168.50.205:39743 install -r app\build\outputs\apk\dev\debug\app-dev-debug.apk
```

Expected: output contains `Success`.

- [ ] **Step 5: Manual IME verification**

Use the device:

1. Open Android input method settings and enable KeePassA IME if it is not enabled.
2. Open Chrome or another browser input field.
3. Switch to KeePassA IME.
4. Type `abc 123 @.-_` in ordinary mode and confirm the browser input field receives the text.
5. Tap the IME search entry.
6. Type a query for a known KeePass entry and confirm the browser input field does not receive the query.
7. Confirm results appear horizontally and the first result is selected.
8. Tap another result and confirm selection changes.
9. Tap username and password buttons in the appropriate browser fields and confirm the selected entry is filled.
10. Lock the database and confirm ordinary keyboard typing still works.
11. While locked, tap search or fill and confirm unlock is required.
12. Toggle keyboard haptic feedback off in app settings and confirm new keyboard keys no longer trigger haptic feedback.

Expected: every step behaves as described.

- [ ] **Step 6: Confirm verification did not leave accidental changes**

Run:

```powershell
git status --short
```

Expected: no unexpected generated files are shown. If manual verification exposed a defect, stop this task and create a new focused fix task with its own failing test, implementation, verification command, and scoped commit.
