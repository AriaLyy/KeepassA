# TOTP Autofill Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add conservative TOTP Autofill support, including TOTP-only Autofill requests, without changing browser domain matching or saving TOTP codes.

**Architecture:** Add a dedicated TOTP field role beside username/password, recognize TOTP fields through a small token policy, and route TOTP dataset values through the existing `OtpUtil.getOtpPass(PwEntryV4)` generator. Keep browser/app entry matching unchanged and prevent TOTP fields from being saved as usernames.

**Tech Stack:** Android Autofill Service, Kotlin, JUnit4 unit/source tests, KeePassDroid `PwEntry`/`PwEntryV4`, existing `OtpUtil`.

---

## Preconditions And Worktree Rules

The current worktree may already contain unrelated user or generated changes. Do not revert them. During implementation, stage only the files named in the current task.

Run before starting:

```powershell
git status --short --branch
```

Expected: it may show existing modified files unrelated to TOTP. Leave them alone.

Use this Java setup for Gradle commands:

```powershell
$env:JAVA_HOME='D:\scoop\user\apps\temurin17-jdk\current'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
```

## File Structure

Create:

- `app/src/main/java/com/lyy/keepassa/service/autofill/AutofillTotpFieldPolicy.kt`  
  Pure token matching for TOTP fields. Does not depend on `AssistStructure`.

- `app/src/main/java/com/lyy/keepassa/service/autofill/AutofillFieldRole.kt`  
  Defines `USERNAME`, `PASSWORD`, and `TOTP` roles used by metadata and value mapping.

- `app/src/main/java/com/lyy/keepassa/service/autofill/AutofillTextValuePolicy.kt`  
  Pure role-to-value selection used by `AutoFillHelper`.

- `app/src/main/java/com/lyy/keepassa/service/autofill/AutofillSaveFieldPolicy.kt`  
  Pure save-flow decision helper so TOTP is never saved as username.

- Tests:
  - `app/src/test/java/com/lyy/keepassa/service/autofill/AutofillTotpFieldPolicyTest.kt`
  - `app/src/test/java/com/lyy/keepassa/service/autofill/AutofillTextValuePolicyTest.kt`
  - `app/src/test/java/com/lyy/keepassa/service/autofill/AutofillSaveFieldPolicyTest.kt`
  - `app/src/test/java/com/lyy/keepassa/service/autofill/TotpAutofillSourceIntegrationTest.kt`

Modify:

- `app/src/main/java/com/lyy/keepassa/service/autofill/model/AutoFillFieldMetadata.kt`  
  Store field role and expose `isPassword`/`isTotp`.

- `app/src/main/java/com/lyy/keepassa/service/autofill/StructureParser.kt`  
  Detect TOTP fields, keep TOTP-only requests, and avoid treating TOTP as username.

- `app/src/main/java/com/lyy/keepassa/service/autofill/W3cHints.kt`  
  Add W3C TOTP detection and stop treating every `autocomplete` attribute as password.

- `app/src/main/java/com/lyy/keepassa/service/autofill/AutoFillHelper.kt`  
  Fill TOTP fields from generated TOTP codes and allow TOTP-only responses without `SaveInfo`.

- `app/src/main/java/com/lyy/keepassa/service/autofill/datasource/KDBAutoFillRepository.kt`  
  Skip TOTP in save and locked-save user/pass extraction.

---

### Task 1: Add Conservative TOTP Token Policy

**Files:**
- Create: `app/src/test/java/com/lyy/keepassa/service/autofill/AutofillTotpFieldPolicyTest.kt`
- Create: `app/src/main/java/com/lyy/keepassa/service/autofill/AutofillTotpFieldPolicy.kt`

- [ ] **Step 1: Write the failing token policy test**

Create `app/src/test/java/com/lyy/keepassa/service/autofill/AutofillTotpFieldPolicyTest.kt`:

```kotlin
package com.lyy.keepassa.service.autofill

import android.util.Pair
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutofillTotpFieldPolicyTest {

  @Test fun recognizesW3cOneTimeCode() {
    assertTrue(AutofillTotpFieldPolicy.isTotpToken("one-time-code"))
    assertTrue(
      AutofillTotpFieldPolicy.isTotpField(
        autofillHints = arrayOf("one-time-code"),
        idEntry = null,
        hint = null,
        htmlAttributes = null
      )
    )
    assertTrue(
      AutofillTotpFieldPolicy.isTotpField(
        autofillHints = null,
        idEntry = null,
        hint = null,
        htmlAttributes = listOf(Pair("autocomplete", "one-time-code"))
      )
    )
  }

  @Test fun recognizesCommonTotpTokens() {
    listOf(
      "otp",
      "totp",
      "2fa",
      "mfa",
      "verification-code",
      "auth-code",
      "authenticator",
      "two_factor_code",
      "mfaCode"
    ).forEach { token ->
      assertTrue("$token should be treated as TOTP", AutofillTotpFieldPolicy.isTotpToken(token))
    }
  }

  @Test fun recognizesChineseTotpPrompts() {
    listOf("验证码", "动态码", "一次性密码", "二次验证", "两步验证").forEach { token ->
      assertTrue("$token should be treated as TOTP", AutofillTotpFieldPolicy.isTotpToken(token))
    }
  }

  @Test fun genericCodeAloneIsNotTotp() {
    listOf("code", "promo_code", "invite-code", "postal-code", "recovery code").forEach { token ->
      assertFalse("$token should not be treated as TOTP", AutofillTotpFieldPolicy.isTotpToken(token))
    }
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
$env:JAVA_HOME='D:\scoop\user\apps\temurin17-jdk\current'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat :app:testDevDebugUnitTest --tests com.lyy.keepassa.service.autofill.AutofillTotpFieldPolicyTest
```

Expected: FAIL because `AutofillTotpFieldPolicy` does not exist.

- [ ] **Step 3: Implement the token policy**

Create `app/src/main/java/com/lyy/keepassa/service/autofill/AutofillTotpFieldPolicy.kt`:

```kotlin
package com.lyy.keepassa.service.autofill

import android.util.Pair
import java.util.Locale

internal object AutofillTotpFieldPolicy {
  const val AUTOFILL_HINT_TOTP = "keepassa:totp"

  private val chineseTokens = listOf("验证码", "动态码", "一次性密码", "二次验证", "两步验证")
  private val strongNormalizedTokens = listOf(
    "otp",
    "totp",
    "2fa",
    "mfa",
    "onetimecode",
    "authenticator"
  )
  private val codeQualifiers = listOf(
    "verification",
    "auth",
    "authentication",
    "twofactor",
    "secondfactor"
  )

  fun isTotpField(
    autofillHints: Array<String>?,
    idEntry: String?,
    hint: CharSequence?,
    htmlAttributes: List<Pair<String, String>>?
  ): Boolean {
    val tokens = ArrayList<String>()
    autofillHints?.forEach(tokens::add)
    idEntry?.let(tokens::add)
    hint?.toString()?.let(tokens::add)
    htmlAttributes?.forEach { attribute ->
      attribute.first?.let(tokens::add)
      attribute.second?.let(tokens::add)
      if (attribute.first.equals("autocomplete", ignoreCase = true)) {
        attribute.second?.let(tokens::add)
      }
    }
    return tokens.any(::isTotpToken)
  }

  fun isTotpToken(value: CharSequence?): Boolean {
    val raw = value?.toString()?.trim()?.takeIf { it.isNotEmpty() } ?: return false
    val lower = raw.lowercase(Locale.ROOT)
    if (chineseTokens.any { lower.contains(it) }) {
      return true
    }

    val normalized = lower.replace(Regex("[^a-z0-9]"), "")
    if (normalized.isEmpty()) {
      return false
    }
    if (strongNormalizedTokens.any { normalized == it || normalized.contains(it) }) {
      return true
    }
    return normalized.contains("code") && codeQualifiers.any { normalized.contains(it) }
  }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```powershell
.\gradlew.bat :app:testDevDebugUnitTest --tests com.lyy.keepassa.service.autofill.AutofillTotpFieldPolicyTest
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add -- app/src/main/java/com/lyy/keepassa/service/autofill/AutofillTotpFieldPolicy.kt app/src/test/java/com/lyy/keepassa/service/autofill/AutofillTotpFieldPolicyTest.kt
git commit -m "test: add totp autofill token policy"
```

---

### Task 2: Add Field Roles To Autofill Metadata

**Files:**
- Create: `app/src/main/java/com/lyy/keepassa/service/autofill/AutofillFieldRole.kt`
- Modify: `app/src/main/java/com/lyy/keepassa/service/autofill/model/AutoFillFieldMetadata.kt`
- Create: `app/src/test/java/com/lyy/keepassa/service/autofill/TotpAutofillSourceIntegrationTest.kt`

- [ ] **Step 1: Write the failing metadata/source integration test**

Create `app/src/test/java/com/lyy/keepassa/service/autofill/TotpAutofillSourceIntegrationTest.kt`:

```kotlin
package com.lyy.keepassa.service.autofill

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class TotpAutofillSourceIntegrationTest {

  @Test fun metadataStoresDedicatedTotpRole() {
    val source = File("src/main/java/com/lyy/keepassa/service/autofill/model/AutoFillFieldMetadata.kt")
      .readText()

    assertTrue(source.contains("var fieldRole: AutofillFieldRole"))
    assertTrue(source.contains("AutofillFieldRole.TOTP"))
    assertTrue(source.contains("val isTotp: Boolean"))
    assertTrue(source.contains("AutofillTotpFieldPolicy.AUTOFILL_HINT_TOTP"))
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
.\gradlew.bat :app:testDevDebugUnitTest --tests com.lyy.keepassa.service.autofill.TotpAutofillSourceIntegrationTest
```

Expected: FAIL because metadata does not store a TOTP role yet.

- [ ] **Step 3: Create field role enum**

Create `app/src/main/java/com/lyy/keepassa/service/autofill/AutofillFieldRole.kt`:

```kotlin
package com.lyy.keepassa.service.autofill

internal enum class AutofillFieldRole {
  USERNAME,
  PASSWORD,
  TOTP
}
```

- [ ] **Step 4: Modify metadata role handling**

In `app/src/main/java/com/lyy/keepassa/service/autofill/model/AutoFillFieldMetadata.kt`:

1. Add imports:

```kotlin
import com.lyy.keepassa.service.autofill.AutofillFieldRole
import com.lyy.keepassa.service.autofill.AutofillTotpFieldPolicy
```

2. Replace the current mutable password flag:

```kotlin
var isPassword: Boolean = false
```

with:

```kotlin
var fieldRole: AutofillFieldRole = AutofillFieldRole.USERNAME
  private set

val isPassword: Boolean
  get() = fieldRole == AutofillFieldRole.PASSWORD

val isTotp: Boolean
  get() = fieldRole == AutofillFieldRole.TOTP
```

3. In `updateSaveTypeFromHints()`, set default role at the top:

```kotlin
fieldRole = AutofillFieldRole.USERNAME
```

4. Add this `when` branch before the standard Android hint branches:

```kotlin
AutofillTotpFieldPolicy.AUTOFILL_HINT_TOTP -> {
  fieldRole = AutofillFieldRole.TOTP
}
```

5. Replace the old password assignment:

```kotlin
isPassword = true
```

with:

```kotlin
fieldRole = AutofillFieldRole.PASSWORD
```

- [ ] **Step 5: Run test to verify it passes**

Run:

```powershell
.\gradlew.bat :app:testDevDebugUnitTest --tests com.lyy.keepassa.service.autofill.TotpAutofillSourceIntegrationTest
```

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add -- app/src/main/java/com/lyy/keepassa/service/autofill/AutofillFieldRole.kt app/src/main/java/com/lyy/keepassa/service/autofill/model/AutoFillFieldMetadata.kt app/src/test/java/com/lyy/keepassa/service/autofill/TotpAutofillSourceIntegrationTest.kt
git commit -m "feat: add totp autofill field role"
```

---

### Task 3: Wire TOTP Detection Into StructureParser And W3C Hints

**Files:**
- Modify: `app/src/main/java/com/lyy/keepassa/service/autofill/W3cHints.kt`
- Modify: `app/src/main/java/com/lyy/keepassa/service/autofill/StructureParser.kt`
- Modify: `app/src/test/java/com/lyy/keepassa/service/autofill/TotpAutofillSourceIntegrationTest.kt`

- [ ] **Step 1: Extend the source integration test**

Append these tests to `TotpAutofillSourceIntegrationTest`:

```kotlin
  @Test fun structureParserKeepsTotpOnlyRequests() {
    val source = File("src/main/java/com/lyy/keepassa/service/autofill/StructureParser.kt")
      .readText()

    assertTrue(source.contains("val totpFields = ArrayList<ViewNode>()"))
    assertTrue(source.contains("totpFields.clear()"))
    assertTrue(source.contains("passFields.isEmpty() && totpFields.isEmpty() && !isManual && !isW3c"))
    assertTrue(source.contains("private fun addTotpField("))
    assertTrue(source.contains("AutoFillFieldMetadata(viewNode, AutofillTotpFieldPolicy.AUTOFILL_HINT_TOTP)"))
  }

  @Test fun w3cHintsExposeTotpDetectionAndDoNotTreatEveryAutocompleteAsPassword() {
    val source = File("src/main/java/com/lyy/keepassa/service/autofill/W3cHints.kt")
      .readText()

    assertTrue(source.contains("fun isW3cTotpByHints(viewNode: ViewNode): Boolean"))
    assertTrue(source.contains("AutofillTotpFieldPolicy.isTotpField("))
    assertTrue(source.contains("name == \"autocomplete\" && PASSWORD_HINT_LIST.contains(value)"))
  }
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
.\gradlew.bat :app:testDevDebugUnitTest --tests com.lyy.keepassa.service.autofill.TotpAutofillSourceIntegrationTest
```

Expected: FAIL because parser and W3C TOTP hooks are not present.

- [ ] **Step 3: Add W3C TOTP detection**

In `app/src/main/java/com/lyy/keepassa/service/autofill/W3cHints.kt`:

1. Add this function near `isW3CUserByHints()`:

```kotlin
  fun isW3cTotpByHints(viewNode: ViewNode): Boolean {
    if (!viewNode.htmlInfo?.tag.equals("input", true)) {
      return false
    }
    return AutofillTotpFieldPolicy.isTotpField(
      autofillHints = viewNode.autofillHints,
      idEntry = viewNode.idEntry,
      hint = viewNode.hint,
      htmlAttributes = viewNode.htmlInfo?.attributes
    )
  }
```

2. In `isW3cPassWord()`, replace the broad autocomplete condition:

```kotlin
      || (p.first == "autocomplete")
```

with the narrower standard-password condition:

```kotlin
      || (p.first.equals("autocomplete", ignoreCase = true) && PASSWORD_HINT_LIST.contains(temp))
```

3. In `isW3CPassByHints()`, replace the broad attribute branch:

```kotlin
      if (ATTR_LIST.contains(name) && PASSWORD_HINT_LIST.contains(value)) {
        return true
      }
      if (name == "label" && value.contains(HINT_PASSWORD_LABEL)) {
        return true
      }
```

with:

```kotlin
      if (ATTR_LIST.contains(name) && PASSWORD_HINT_LIST.contains(value)) {
        return true
      }
      if (name == "autocomplete" && PASSWORD_HINT_LIST.contains(value)) {
        return true
      }
      if (name == "label" && value.contains(HINT_PASSWORD_LABEL)) {
        return true
      }
```

- [ ] **Step 4: Add StructureParser TOTP collection**

In `app/src/main/java/com/lyy/keepassa/service/autofill/StructureParser.kt`:

1. Add a field next to `passFields`:

```kotlin
  val totpFields = ArrayList<ViewNode>()
```

2. Add to `clear()`:

```kotlin
    totpFields.clear()
```

3. Replace the non-manual clear guard:

```kotlin
    if (passFields.isEmpty() && !isManual && !isW3c) {
      autoFillFields.clear()
    }
```

with:

```kotlin
    if (passFields.isEmpty() && totpFields.isEmpty() && !isManual && !isW3c) {
      autoFillFields.clear()
    }
```

4. In `rememberBrowserFormFieldCandidate()`, after the search/url check, add:

```kotlin
    if (isTotp(viewNode)) {
      return
    }
```

5. In `getAndroidViewInfo()`, insert TOTP before username:

```kotlin
    if (isTotp(viewNode)) {
      addTotpField(viewNode)
      return
    }
```

6. In `getW3CInfo()`, insert TOTP before username:

```kotlin
    if (W3cHints.isW3cTotpByHints(viewNode)) {
      Timber.i("addTotp by hints")
      addTotpField(viewNode)
      return
    }
```

7. Add this method near `addPassField()`:

```kotlin
  private fun addTotpField(viewNode: ViewNode, force: Boolean = false) {
    if (!force && !isW3c && !isInnerAppW3c && (viewNode.visibility != View.VISIBLE || !viewNode.isFocusable)) {
      return
    }
    Timber.d("totp autofillType = ${viewNode.autofillType}, fillId = ${viewNode.autofillId}, idEntry = ${viewNode.idEntry}, hint = ${viewNode.hint}, visibility = ${viewNode.visibility}, isActivated = ${viewNode.isActivated}")
    totpFields.add(viewNode)
    autoFillFields.add(AutoFillFieldMetadata(viewNode, AutofillTotpFieldPolicy.AUTOFILL_HINT_TOTP))
  }
```

8. Add this method before `isUserName()`:

```kotlin
  private fun isTotp(f: ViewNode): Boolean {
    if (isLikelySearchOrUrlField(f) || isPassword(f)) {
      return false
    }
    return AutofillTotpFieldPolicy.isTotpField(
      autofillHints = f.autofillHints,
      idEntry = f.idEntry,
      hint = f.hint,
      htmlAttributes = f.htmlInfo?.attributes
    )
  }
```

- [ ] **Step 5: Run tests to verify they pass**

Run:

```powershell
.\gradlew.bat :app:testDevDebugUnitTest --tests com.lyy.keepassa.service.autofill.TotpAutofillSourceIntegrationTest --tests com.lyy.keepassa.service.autofill.AutofillTotpFieldPolicyTest
```

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add -- app/src/main/java/com/lyy/keepassa/service/autofill/W3cHints.kt app/src/main/java/com/lyy/keepassa/service/autofill/StructureParser.kt app/src/test/java/com/lyy/keepassa/service/autofill/TotpAutofillSourceIntegrationTest.kt
git commit -m "feat: classify totp autofill fields"
```

---

### Task 4: Fill TOTP Values In Autofill Datasets

**Files:**
- Create: `app/src/main/java/com/lyy/keepassa/service/autofill/AutofillTextValuePolicy.kt`
- Modify: `app/src/main/java/com/lyy/keepassa/service/autofill/AutoFillHelper.kt`
- Create: `app/src/test/java/com/lyy/keepassa/service/autofill/AutofillTextValuePolicyTest.kt`
- Modify: `app/src/test/java/com/lyy/keepassa/service/autofill/TotpAutofillSourceIntegrationTest.kt`

- [ ] **Step 1: Write failing pure value policy test**

Create `app/src/test/java/com/lyy/keepassa/service/autofill/AutofillTextValuePolicyTest.kt`:

```kotlin
package com.lyy.keepassa.service.autofill

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AutofillTextValuePolicyTest {

  @Test fun usernameRoleUsesUsername() {
    assertEquals(
      "alice",
      AutofillTextValuePolicy.valueForRole(
        role = AutofillFieldRole.USERNAME,
        username = "alice",
        password = "secret",
        totp = "123456"
      )
    )
  }

  @Test fun passwordRoleUsesPassword() {
    assertEquals(
      "secret",
      AutofillTextValuePolicy.valueForRole(
        role = AutofillFieldRole.PASSWORD,
        username = "alice",
        password = "secret",
        totp = "123456"
      )
    )
  }

  @Test fun totpRoleUsesTotpWhenPresent() {
    assertEquals(
      "123456",
      AutofillTextValuePolicy.valueForRole(
        role = AutofillFieldRole.TOTP,
        username = "alice",
        password = "secret",
        totp = "123456"
      )
    )
  }

  @Test fun totpRoleReturnsNullWhenEntryHasNoTotp() {
    assertNull(
      AutofillTextValuePolicy.valueForRole(
        role = AutofillFieldRole.TOTP,
        username = "alice",
        password = "secret",
        totp = null
      )
    )
  }
}
```

- [ ] **Step 2: Extend source integration test for AutoFillHelper**

Append this test to `TotpAutofillSourceIntegrationTest`:

```kotlin
  @Test fun autoFillHelperUsesTotpResolverAndBuildsResponsesWithoutSaveInfoForTotpOnly() {
    val source = File("src/main/java/com/lyy/keepassa/service/autofill/AutoFillHelper.kt")
      .readText()

    assertTrue(source.contains("OtpUtil.getOtpPass(pwEntry).second"))
    assertTrue(source.contains("AutofillTextValuePolicy.valueForRole("))
    assertTrue(source.contains("var addedDataset = false"))
    assertTrue(source.contains("return if (addedDataset)"))
  }
```

- [ ] **Step 3: Run tests to verify they fail**

Run:

```powershell
.\gradlew.bat :app:testDevDebugUnitTest --tests com.lyy.keepassa.service.autofill.AutofillTextValuePolicyTest --tests com.lyy.keepassa.service.autofill.TotpAutofillSourceIntegrationTest
```

Expected: FAIL because `AutofillTextValuePolicy` and AutoFillHelper integration do not exist.

- [ ] **Step 4: Create value policy**

Create `app/src/main/java/com/lyy/keepassa/service/autofill/AutofillTextValuePolicy.kt`:

```kotlin
package com.lyy.keepassa.service.autofill

internal object AutofillTextValuePolicy {
  fun valueForRole(
    role: AutofillFieldRole,
    username: String,
    password: String,
    totp: String?
  ): String? {
    return when (role) {
      AutofillFieldRole.USERNAME -> username
      AutofillFieldRole.PASSWORD -> password
      AutofillFieldRole.TOTP -> totp?.takeIf { it.isNotBlank() }
    }
  }
}
```

- [ ] **Step 5: Modify AutoFillHelper dataset generation**

In `app/src/main/java/com/lyy/keepassa/service/autofill/AutoFillHelper.kt`:

1. Add imports:

```kotlin
import com.lyy.keepassa.util.totp.OtpUtil
```

2. In `newResponse()`, replace:

```kotlin
    entries?.forEach { entry ->
      val dataSet = newDataSet(context, metadata, entry, dataSetAuth, apkPageName)
      dataSet?.let(responseBuilder::addDataset)
    }
```

with:

```kotlin
    var addedDataset = false
    entries?.forEach { entry ->
      val dataSet = newDataSet(context, metadata, entry, dataSetAuth, apkPageName)
      dataSet?.let {
        responseBuilder.addDataset(it)
        addedDataset = true
      }
    }
```

3. Replace the final `return if (metadata.saveType != 0) { ... } else { ... }` block with:

```kotlin
    if (!addedDataset) {
      Timber.d("No datasets available for autofill response.")
      return null
    }

    if (metadata.saveType != 0) {
      val autoFillIds = metadata.autoFillIds
      responseBuilder.setSaveInfo(
        SaveInfo.Builder(
          metadata.saveType,
          autoFillIds.toTypedArray()
        )
          .build()
      )
    }

    return responseBuilder.build()
```

4. In `applyDataInfoToFields()`, inside `View.AUTOFILL_TYPE_TEXT`, replace:

```kotlin
            if (fillField.isPassword) {
              dataSetBuilder.setValue(fillId, AutofillValue.forText(KdbUtil.getPassword(pwEntry)))
            } else {
              dataSetBuilder.setValue(fillId, AutofillValue.forText(KdbUtil.getUserName(pwEntry)))
            }
            setValueAtLeastOnce = true
```

with:

```kotlin
            val totp = (pwEntry as? PwEntryV4)?.let { OtpUtil.getOtpPass(it).second }
            val textValue = AutofillTextValuePolicy.valueForRole(
              role = fillField.fieldRole,
              username = KdbUtil.getUserName(pwEntry),
              password = KdbUtil.getPassword(pwEntry),
              totp = totp
            ) ?: continue@loop
            dataSetBuilder.setValue(fillId, AutofillValue.forText(textValue))
            setValueAtLeastOnce = true
```

- [ ] **Step 6: Run tests to verify they pass**

Run:

```powershell
.\gradlew.bat :app:testDevDebugUnitTest --tests com.lyy.keepassa.service.autofill.AutofillTextValuePolicyTest --tests com.lyy.keepassa.service.autofill.TotpAutofillSourceIntegrationTest
```

Expected: PASS.

- [ ] **Step 7: Commit**

```powershell
git add -- app/src/main/java/com/lyy/keepassa/service/autofill/AutofillTextValuePolicy.kt app/src/main/java/com/lyy/keepassa/service/autofill/AutoFillHelper.kt app/src/test/java/com/lyy/keepassa/service/autofill/AutofillTextValuePolicyTest.kt app/src/test/java/com/lyy/keepassa/service/autofill/TotpAutofillSourceIntegrationTest.kt
git commit -m "feat: fill totp autofill values"
```

---

### Task 5: Keep TOTP Out Of Autofill Save

**Files:**
- Create: `app/src/main/java/com/lyy/keepassa/service/autofill/AutofillSaveFieldPolicy.kt`
- Modify: `app/src/main/java/com/lyy/keepassa/service/autofill/datasource/KDBAutoFillRepository.kt`
- Create: `app/src/test/java/com/lyy/keepassa/service/autofill/AutofillSaveFieldPolicyTest.kt`
- Modify: `app/src/test/java/com/lyy/keepassa/service/autofill/TotpAutofillSourceIntegrationTest.kt`

- [ ] **Step 1: Write failing save policy test**

Create `app/src/test/java/com/lyy/keepassa/service/autofill/AutofillSaveFieldPolicyTest.kt`:

```kotlin
package com.lyy.keepassa.service.autofill

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutofillSaveFieldPolicyTest {

  @Test fun passwordFieldsCanBeSavedAsPassword() {
    assertTrue(AutofillSaveFieldPolicy.shouldSaveAsPassword(isPassword = true, isTotp = false))
    assertFalse(AutofillSaveFieldPolicy.shouldSaveAsUsername(isPassword = true, isTotp = false))
  }

  @Test fun usernameFieldsCanBeSavedAsUsername() {
    assertFalse(AutofillSaveFieldPolicy.shouldSaveAsPassword(isPassword = false, isTotp = false))
    assertTrue(AutofillSaveFieldPolicy.shouldSaveAsUsername(isPassword = false, isTotp = false))
  }

  @Test fun totpFieldsAreNeverSavedAsUsernameOrPassword() {
    assertFalse(AutofillSaveFieldPolicy.shouldSaveAsPassword(isPassword = false, isTotp = true))
    assertFalse(AutofillSaveFieldPolicy.shouldSaveAsUsername(isPassword = false, isTotp = true))
  }
}
```

- [ ] **Step 2: Extend source integration test for repository**

Append this test to `TotpAutofillSourceIntegrationTest`:

```kotlin
  @Test fun repositoryDoesNotSaveTotpAsUsername() {
    val source = File("src/main/java/com/lyy/keepassa/service/autofill/datasource/KDBAutoFillRepository.kt")
      .readText()

    assertTrue(source.contains("AutofillSaveFieldPolicy.shouldSaveAsPassword("))
    assertTrue(source.contains("AutofillSaveFieldPolicy.shouldSaveAsUsername("))
  }
```

- [ ] **Step 3: Run tests to verify they fail**

Run:

```powershell
.\gradlew.bat :app:testDevDebugUnitTest --tests com.lyy.keepassa.service.autofill.AutofillSaveFieldPolicyTest --tests com.lyy.keepassa.service.autofill.TotpAutofillSourceIntegrationTest
```

Expected: FAIL because `AutofillSaveFieldPolicy` and repository calls do not exist.

- [ ] **Step 4: Create save field policy**

Create `app/src/main/java/com/lyy/keepassa/service/autofill/AutofillSaveFieldPolicy.kt`:

```kotlin
package com.lyy.keepassa.service.autofill

internal object AutofillSaveFieldPolicy {
  fun shouldSaveAsPassword(isPassword: Boolean, isTotp: Boolean): Boolean {
    return isPassword && !isTotp
  }

  fun shouldSaveAsUsername(isPassword: Boolean, isTotp: Boolean): Boolean {
    return !isPassword && !isTotp
  }
}
```

- [ ] **Step 5: Modify KDBAutoFillRepository save mapping**

In `app/src/main/java/com/lyy/keepassa/service/autofill/datasource/KDBAutoFillRepository.kt`:

1. Add import:

```kotlin
import com.lyy.keepassa.service.autofill.AutofillSaveFieldPolicy
```

2. In `saveDataToKdb()`, replace:

```kotlin
          if (fillField.isPassword) {
            entry.setPassword(fillField.autoFillField.textValue, BaseApp.KDB!!.pm)
            Timber.d("pass = ${fillField.autoFillField.textValue}")
          } else {
            entry.setUsername(fillField.autoFillField.textValue, BaseApp.KDB!!.pm)
            Timber.d("userName = ${fillField.autoFillField.textValue}")
          }
```

with:

```kotlin
          if (AutofillSaveFieldPolicy.shouldSaveAsPassword(fillField.isPassword, fillField.isTotp)) {
            entry.setPassword(fillField.autoFillField.textValue, BaseApp.KDB!!.pm)
            Timber.d("pass = ${fillField.autoFillField.textValue}")
          } else if (AutofillSaveFieldPolicy.shouldSaveAsUsername(fillField.isPassword, fillField.isTotp)) {
            entry.setUsername(fillField.autoFillField.textValue, BaseApp.KDB!!.pm)
            Timber.d("userName = ${fillField.autoFillField.textValue}")
          }
```

3. In `getUserInfo()`, replace:

```kotlin
          if (fillField.isPassword) {
            pass = fillField.autoFillField.textValue
          }
          if (!fillField.isPassword) {
            user = fillField.autoFillField.textValue
          }
```

with:

```kotlin
          if (AutofillSaveFieldPolicy.shouldSaveAsPassword(fillField.isPassword, fillField.isTotp)) {
            pass = fillField.autoFillField.textValue
          }
          if (AutofillSaveFieldPolicy.shouldSaveAsUsername(fillField.isPassword, fillField.isTotp)) {
            user = fillField.autoFillField.textValue
          }
```

- [ ] **Step 6: Run tests to verify they pass**

Run:

```powershell
.\gradlew.bat :app:testDevDebugUnitTest --tests com.lyy.keepassa.service.autofill.AutofillSaveFieldPolicyTest --tests com.lyy.keepassa.service.autofill.TotpAutofillSourceIntegrationTest
```

Expected: PASS.

- [ ] **Step 7: Commit**

```powershell
git add -- app/src/main/java/com/lyy/keepassa/service/autofill/AutofillSaveFieldPolicy.kt app/src/main/java/com/lyy/keepassa/service/autofill/datasource/KDBAutoFillRepository.kt app/src/test/java/com/lyy/keepassa/service/autofill/AutofillSaveFieldPolicyTest.kt app/src/test/java/com/lyy/keepassa/service/autofill/TotpAutofillSourceIntegrationTest.kt
git commit -m "fix: exclude totp from autofill save"
```

---

### Task 6: Full Verification And Device Install

**Files:**
- No source changes.

- [ ] **Step 1: Run focused Autofill tests**

Run:

```powershell
$env:JAVA_HOME='D:\scoop\user\apps\temurin17-jdk\current'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat :app:testDevDebugUnitTest --tests com.lyy.keepassa.service.autofill.AutofillTotpFieldPolicyTest --tests com.lyy.keepassa.service.autofill.AutofillTextValuePolicyTest --tests com.lyy.keepassa.service.autofill.AutofillSaveFieldPolicyTest --tests com.lyy.keepassa.service.autofill.TotpAutofillSourceIntegrationTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Run existing Autofill regression tests touched by nearby code**

Run:

```powershell
.\gradlew.bat :app:testDevDebugUnitTest --tests com.lyy.keepassa.service.autofill.AutoFillServiceSourceTest --tests com.lyy.keepassa.service.autofill.W3cHintsCompatBrowserTest --tests com.lyy.keepassa.service.autofill.BrowserAutofillStrategyTest --tests com.lyy.keepassa.service.autofill.AutofillEntryLookupPolicyTest --tests com.lyy.keepassa.service.autofill.AutofillSingleFieldFallbackPolicyTest --tests com.lyy.keepassa.service.autofill.AutofillBrowserAuthContextStoreTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Build debug APK**

Run:

```powershell
.\gradlew.bat :app:assembleDevDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Install on connected device**

Check devices:

```powershell
& 'D:\Dev\sdk\platform-tools\adb.exe' devices
```

Install to the first online ADB device:

```powershell
$device = (& 'D:\Dev\sdk\platform-tools\adb.exe' devices |
  Select-String -Pattern "`tdevice$" |
  Select-Object -First 1).ToString().Split("`t")[0]
if ([string]::IsNullOrWhiteSpace($device)) {
  throw "No online ADB device found"
}
& 'D:\Dev\sdk\platform-tools\adb.exe' -s $device install -r 'app\build\outputs\apk\dev\debug\app-dev-debug.apk'
```

Expected: `Success`.

- [ ] **Step 5: Commit verification note only if a verification artifact file was created**

No commit is needed for this task if no files changed. If Gradle or Android tooling created tracked file changes, inspect them with `git status --short` and do not commit generated noise.

---

## Self-Review Notes

Spec coverage:

- TOTP field detection is covered by Task 1 and Task 3.
- TOTP-only requests are covered by Task 3 parser guard changes.
- Dataset TOTP fill is covered by Task 4.
- Entry matching remains unchanged because no matching files are modified.
- TOTP save is explicitly blocked by Task 5.
- Conservative token matching, including `二次验证` and `两步验证`, is covered by Task 1 tests.

Implementation constraints:

- No browser package fallback changes.
- No multilingual resource changes.
- No `arrays.xml` changes.
- No TOTP save behavior.
- Stage only task files because the worktree may contain unrelated dirty changes.
