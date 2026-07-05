# Credential Manager Settings Shortcut Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a KeePassA settings button that opens Android Credential Manager settings when available, with safe fallback pages on ROMs such as MIUI.

**Architecture:** Put system settings navigation in a focused helper under `view/setting`, and keep `AppSettingFragment` responsible only for wiring the preference click and showing user-facing Toast messages. The helper exposes pure candidate-action policy for JVM unit tests and an Android-facing `open` function for runtime use.

**Tech Stack:** Kotlin, AndroidX Preference, Android system `Settings` intents, JUnit 4 JVM tests, Gradle `assembleDevDebug`.

---

## File Structure

- Create: `app/src/main/java/com/lyy/keepassa/view/setting/CredentialManagerSettingsShortcut.kt`
  - Owns candidate intent ordering and best-effort settings launch.
  - Returns `DIRECT`, `FALLBACK`, or `FAILED` so the Fragment can decide which Toast to show.
- Create: `app/src/test/java/com/lyy/keepassa/view/setting/CredentialManagerSettingsShortcutTest.kt`
  - Tests candidate ordering and fallback policy without launching Android settings.
- Modify: `app/src/main/java/com/lyy/keepassa/view/setting/AppSettingFragment.kt`
  - Calls `setCredentialManagerSettings()` from `onCreatePreferences`.
  - Wires the new preference click to `CredentialManagerSettingsShortcut.open(requireContext())`.
- Modify: `app/src/main/res/xml/app_setting.xml`
  - Adds a `Preference` under the existing Autofill category.
- Modify: `app/src/main/res/values/pre_key.xml`
  - Adds the non-translatable preference key.
- Modify: `app/src/main/res/values/strings.xml`
  - Adds English title, summary, fallback Toast, and failure Toast strings.
- Modify: `app/src/main/res/values-zh-rCN/strings.xml`
  - Adds Chinese title, summary, fallback Toast, and failure Toast strings.
- Modify: `app/src/test/java/com/lyy/keepassa/view/setting/AppSettingBrowserAutofillPreferenceTest.kt`
  - Verifies the new preference lives in the Autofill category and `AppSettingFragment` initializes it.

---

### Task 1: Write Failing Shortcut Policy And Settings XML Tests

**Files:**
- Create: `app/src/test/java/com/lyy/keepassa/view/setting/CredentialManagerSettingsShortcutTest.kt`
- Modify: `app/src/test/java/com/lyy/keepassa/view/setting/AppSettingBrowserAutofillPreferenceTest.kt`

- [ ] **Step 1: Add the failing policy test**

Create `app/src/test/java/com/lyy/keepassa/view/setting/CredentialManagerSettingsShortcutTest.kt` with:

```kotlin
package com.lyy.keepassa.view.setting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CredentialManagerSettingsShortcutTest {

  @Test fun api35AndAboveTryCredentialProviderSettingsBeforeFallbacks() {
    val candidates = CredentialManagerSettingsShortcut.candidateActions(sdkInt = 35)

    assertEquals("android.settings.CREDENTIAL_PROVIDER", candidates[0].action)
    assertTrue(candidates[0].isDirectCredentialProvider)
    assertEquals("android.settings.SECURITY_SETTINGS", candidates[1].action)
    assertEquals("android.settings.SETTINGS", candidates[2].action)
  }

  @Test fun lowerApiLevelsStillTryLiteralCredentialProviderActionBeforeFallbacks() {
    val candidates = CredentialManagerSettingsShortcut.candidateActions(sdkInt = 34)

    assertEquals("android.settings.CREDENTIAL_PROVIDER", candidates[0].action)
    assertTrue(candidates[0].isDirectCredentialProvider)
    assertEquals("android.settings.SECURITY_SETTINGS", candidates[1].action)
    assertEquals("android.settings.SETTINGS", candidates[2].action)
  }

  @Test fun candidateActionsDoNotContainDuplicates() {
    val actions = CredentialManagerSettingsShortcut.candidateActions(sdkInt = 35)
      .map { it.action }

    assertEquals(actions.distinct(), actions)
  }
}
```

- [ ] **Step 2: Extend the failing settings source test**

In `app/src/test/java/com/lyy/keepassa/view/setting/AppSettingBrowserAutofillPreferenceTest.kt`, add this test near `autofillPreferencesAreInDedicatedCategory()`:

```kotlin
  @Test fun credentialManagerSettingsPreferenceIsInAutofillCategory() {
    val category = findCategoryByTitle(appSettingDocument(), "@string/auto_fill_set")

    assertNotNull("Autofill category should be present", category)
    assertTrue(
      category!!.directChildPreferenceKeys()
        .contains("@string/set_key_credential_manager_settings")
    )
  }
```

Add this test near `appSettingFragmentInitializesBrowserAutofillSettings()`:

```kotlin
  @Test fun appSettingFragmentInitializesCredentialManagerSettings() {
    val fragment = File("src/main/java/com/lyy/keepassa/view/setting/AppSettingFragment.kt")
      .readText()

    assertTrue(fragment.contains("setCredentialManagerSettings()"))
    assertTrue(fragment.contains("CredentialManagerSettingsShortcut.open"))
  }
```

Add this test near `browserAutofillSettingsTitleUsesBrowserNamePlaceholder()`:

```kotlin
  @Test fun credentialManagerSettingsStringsExistInDefaultAndChineseResources() {
    assertEquals(
      "Credential Manager",
      stringValue(
        file = File("src/main/res/values/strings.xml"),
        name = "credential_manager_settings_title"
      )
    )
    assertEquals(
      "凭据管理器",
      stringValue(
        file = File("src/main/res/values-zh-rCN/strings.xml"),
        name = "credential_manager_settings_title"
      )
    )
    assertTrue(
      stringValue(
        file = File("src/main/res/values/strings.xml"),
        name = "credential_manager_settings_summary"
      ).contains("passkey")
    )
    assertTrue(
      stringValue(
        file = File("src/main/res/values-zh-rCN/strings.xml"),
        name = "credential_manager_settings_summary"
      ).contains("通行密钥")
    )
  }
```

- [ ] **Step 3: Run tests to verify they fail**

Run:

```powershell
.\gradlew.bat :app:testDevDebugUnitTest --tests "com.lyy.keepassa.view.setting.CredentialManagerSettingsShortcutTest" --tests "com.lyy.keepassa.view.setting.AppSettingBrowserAutofillPreferenceTest"
```

Expected: FAIL because `CredentialManagerSettingsShortcut` and the new preference resources do not exist yet.

- [ ] **Step 4: Commit the failing tests**

Run:

```powershell
git add app/src/test/java/com/lyy/keepassa/view/setting/CredentialManagerSettingsShortcutTest.kt app/src/test/java/com/lyy/keepassa/view/setting/AppSettingBrowserAutofillPreferenceTest.kt
git commit -m "test: cover credential manager settings shortcut"
```

---

### Task 2: Implement Shortcut Helper And Settings Entry

**Files:**
- Create: `app/src/main/java/com/lyy/keepassa/view/setting/CredentialManagerSettingsShortcut.kt`
- Modify: `app/src/main/java/com/lyy/keepassa/view/setting/AppSettingFragment.kt`
- Modify: `app/src/main/res/xml/app_setting.xml`
- Modify: `app/src/main/res/values/pre_key.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-zh-rCN/strings.xml`

- [ ] **Step 1: Add the shortcut helper**

Create `app/src/main/java/com/lyy/keepassa/view/setting/CredentialManagerSettingsShortcut.kt`:

```kotlin
package com.lyy.keepassa.view.setting

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import timber.log.Timber

data class CredentialManagerSettingsCandidate(
  val action: String,
  val isDirectCredentialProvider: Boolean
)

enum class CredentialManagerSettingsOpenResult {
  DIRECT,
  FALLBACK,
  FAILED
}

object CredentialManagerSettingsShortcut {
  private const val API_35 = 35
  const val CREDENTIAL_PROVIDER_SETTINGS_ACTION = "android.settings.CREDENTIAL_PROVIDER"

  fun candidateActions(
    sdkInt: Int = Build.VERSION.SDK_INT
  ): List<CredentialManagerSettingsCandidate> {
    val credentialProviderAction = if (sdkInt >= API_35) {
      Settings.ACTION_CREDENTIAL_PROVIDER
    } else {
      CREDENTIAL_PROVIDER_SETTINGS_ACTION
    }
    return listOf(
      CredentialManagerSettingsCandidate(
        action = credentialProviderAction,
        isDirectCredentialProvider = true
      ),
      CredentialManagerSettingsCandidate(
        action = Settings.ACTION_SECURITY_SETTINGS,
        isDirectCredentialProvider = false
      ),
      CredentialManagerSettingsCandidate(
        action = Settings.ACTION_SETTINGS,
        isDirectCredentialProvider = false
      )
    ).distinctBy { it.action }
  }

  @Suppress("DEPRECATION")
  fun open(context: Context): CredentialManagerSettingsOpenResult {
    for (candidate in candidateActions()) {
      val intent = Intent(candidate.action)
      if (context !is Activity) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      if (intent.resolveActivity(context.packageManager) == null) {
        continue
      }
      val opened = runCatching {
        context.startActivity(intent)
        true
      }.getOrElse {
        Timber.e(it, "open credential manager settings failed: %s", candidate.action)
        false
      }
      if (opened) {
        return if (candidate.isDirectCredentialProvider) {
          CredentialManagerSettingsOpenResult.DIRECT
        } else {
          CredentialManagerSettingsOpenResult.FALLBACK
        }
      }
    }
    return CredentialManagerSettingsOpenResult.FAILED
  }
}
```

- [ ] **Step 2: Add the preference key**

In `app/src/main/res/values/pre_key.xml`, add the key immediately after `set_open_auto_fill`:

```xml
  <string name="set_key_credential_manager_settings" translatable="false">set_key_credential_manager_settings</string>
```

- [ ] **Step 3: Add English strings**

In `app/src/main/res/values/strings.xml`, add these strings near the Autofill settings strings:

```xml
    <string name="credential_manager_settings_title"><![CDATA[Credential Manager]]></string>
    <string name="credential_manager_settings_summary"><![CDATA[Open system password, passkey, and credential provider settings]]></string>
    <string name="credential_manager_settings_fallback"><![CDATA[This system does not support direct Credential Manager settings. Search system settings for "credentials" or "passwords".]]></string>
    <string name="credential_manager_settings_open_failed"><![CDATA[Unable to open system Credential Manager settings.]]></string>
```

- [ ] **Step 4: Add Chinese strings**

In `app/src/main/res/values-zh-rCN/strings.xml`, add these strings near the Autofill settings strings:

```xml
    <string name="credential_manager_settings_title">凭据管理器</string>
    <string name="credential_manager_settings_summary">打开系统密码、通行密钥和凭据提供程序设置</string>
    <string name="credential_manager_settings_fallback">当前系统不支持直达凭据管理器设置，请在系统设置中搜索“凭据”或“密码”。</string>
    <string name="credential_manager_settings_open_failed">无法打开系统凭据管理器设置。</string>
```

- [ ] **Step 5: Add the settings preference**

In `app/src/main/res/xml/app_setting.xml`, add this `Preference` under the existing Autofill category after the Autofill service `SwitchPreference` and before `supported_browsers_title`:

```xml
    <Preference
        android:title="@string/credential_manager_settings_title"
        app:icon="@drawable/ic_password"
        app:key="@string/set_key_credential_manager_settings"
        app:order="11"
        app:summary="@string/credential_manager_settings_summary" />
```

Then change the existing supported browsers preference order from `11` to `12`:

```xml
        app:order="12"
```

- [ ] **Step 6: Wire the Fragment click handler**

In `app/src/main/java/com/lyy/keepassa/view/setting/AppSettingFragment.kt`, call the new setup method after `setAtoFill()`:

```kotlin
    setAtoFill()
    setCredentialManagerSettings()
    setBrowserAutofillSettings()
```

Add this method near `setAtoFill()` or before `setBrowserAutofillSettings()`:

```kotlin
  private fun setCredentialManagerSettings() {
    findPreference<Preference>(
      getString(R.string.set_key_credential_manager_settings)
    )?.setOnPreferenceClickListener {
      when (CredentialManagerSettingsShortcut.open(requireContext())) {
        CredentialManagerSettingsOpenResult.DIRECT -> Unit
        CredentialManagerSettingsOpenResult.FALLBACK -> {
          ToastUtils.showLong(getString(R.string.credential_manager_settings_fallback))
        }
        CredentialManagerSettingsOpenResult.FAILED -> {
          ToastUtils.showLong(getString(R.string.credential_manager_settings_open_failed))
        }
      }
      true
    }
  }
```

- [ ] **Step 7: Run focused tests to verify they pass**

Run:

```powershell
.\gradlew.bat :app:testDevDebugUnitTest --tests "com.lyy.keepassa.view.setting.CredentialManagerSettingsShortcutTest" --tests "com.lyy.keepassa.view.setting.AppSettingBrowserAutofillPreferenceTest"
```

Expected: PASS for both test classes.

- [ ] **Step 8: Commit the implementation**

Run:

```powershell
git add app/src/main/java/com/lyy/keepassa/view/setting/CredentialManagerSettingsShortcut.kt app/src/main/java/com/lyy/keepassa/view/setting/AppSettingFragment.kt app/src/main/res/xml/app_setting.xml app/src/main/res/values/pre_key.xml app/src/main/res/values/strings.xml app/src/main/res/values-zh-rCN/strings.xml app/src/test/java/com/lyy/keepassa/view/setting/CredentialManagerSettingsShortcutTest.kt app/src/test/java/com/lyy/keepassa/view/setting/AppSettingBrowserAutofillPreferenceTest.kt
git commit -m "feat: add credential manager settings shortcut"
```

---

### Task 3: Build And Device Verification

**Files:**
- No source edits expected.

- [ ] **Step 1: Build the dev debug APK**

Run:

```powershell
.\gradlew.bat :app:assembleDevDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Install on the connected MIUI device**

Run:

```powershell
adb devices
adb install -r app\build\outputs\apk\dev\debug\app-dev-debug.apk
```

Expected: one connected device and `Success` from install.

- [ ] **Step 3: Manually verify the settings entry**

On the phone:

1. Open KeePassA.
2. Open app settings.
3. Open the `自动填充设置` section.
4. Tap `凭据管理器`.

Expected on ROMs that expose Credential Manager settings: the system Credential Manager provider page opens.

Expected on the connected MIUI device if the direct action is still unavailable: a safe fallback system settings page opens and KeePassA shows the fallback Toast:

```text
当前系统不支持直达凭据管理器设置，请在系统设置中搜索“凭据”或“密码”。
```

- [ ] **Step 4: Record known baseline test status**

Run the focused tests again if any source changed during verification:

```powershell
.\gradlew.bat :app:testDevDebugUnitTest --tests "com.lyy.keepassa.view.setting.CredentialManagerSettingsShortcutTest" --tests "com.lyy.keepassa.view.setting.AppSettingBrowserAutofillPreferenceTest"
```

Expected: PASS.

Do not claim full unit suite success unless `.\gradlew.bat :app:testDevDebugUnitTest` is run and passes. The branch currently has known pre-existing full-suite failures unrelated to this shortcut.

---

## Self-Review

- Spec coverage: the plan adds the Autofill-category settings entry, best-effort direct action, security/settings fallbacks, Toast messages, policy tests, build, install, and MIUI manual verification.
- Placeholder scan: no `TODO`, `TBD`, or unresolved placeholders remain in this plan.
- Type consistency: `CredentialManagerSettingsShortcut`, `CredentialManagerSettingsCandidate`, and `CredentialManagerSettingsOpenResult` are introduced before later tasks reference them.
