# Credential Manager Provider Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Android 14+ Credential Manager password provider support for reading and saving KeePassA passwords.

**Architecture:** Keep the existing `AutoFillService` for traditional Android autofill. Add a separate Credential Manager provider service under `service/credential`, backed by small pure mapping/policy classes and thin result bridge activities under `view/credential`. Reuse existing database lookup, unlock, quick unlock, and entry creation flows wherever possible.

**Tech Stack:** Kotlin, AndroidX Credentials Provider APIs, Android Autofill/KeePassA existing repository utilities, JUnit 4, Gradle Android app module.

---

## Approved Spec

Use this spec as the source of truth:

```text
docs/superpowers/specs/2026-07-05-credential-manager-provider-design.md
```

The first implementation supports password get and password save only. It does not support passkeys.

## File Structure

Create:

```text
app/src/main/java/com/lyy/keepassa/service/credential/CredentialLookupTarget.kt
app/src/main/java/com/lyy/keepassa/service/credential/CredentialLookupTargetMapper.kt
app/src/main/java/com/lyy/keepassa/service/credential/CredentialSaveDraft.kt
app/src/main/java/com/lyy/keepassa/service/credential/CredentialSaveRequestMapper.kt
app/src/main/java/com/lyy/keepassa/service/credential/CredentialPasswordRepository.kt
app/src/main/java/com/lyy/keepassa/service/credential/CredentialProviderPendingIntents.kt
app/src/main/java/com/lyy/keepassa/service/credential/KeepassACredentialProviderService.kt
app/src/main/java/com/lyy/keepassa/view/credential/CredentialGetActivity.kt
app/src/main/java/com/lyy/keepassa/view/credential/CredentialSaveActivity.kt
app/src/main/res/xml/credential_provider.xml
app/src/test/java/com/lyy/keepassa/service/credential/CredentialLookupTargetMapperTest.kt
app/src/test/java/com/lyy/keepassa/service/credential/CredentialSaveRequestMapperTest.kt
app/src/test/java/com/lyy/keepassa/service/credential/CredentialProviderPendingIntentsTest.kt
app/src/test/java/com/lyy/keepassa/view/create/entry/CreateEntryCredentialResultPolicyTest.kt
```

Modify:

```text
app/build.gradle
app/src/main/AndroidManifest.xml
app/src/main/java/com/lyy/keepassa/view/create/entry/AutoFillSaveEntryBinder.kt
app/src/main/java/com/lyy/keepassa/view/create/entry/CreateEntryActivity.kt
app/src/main/java/com/lyy/keepassa/view/create/entry/CreateEntryModule.kt
app/src/main/java/com/lyy/keepassa/view/launcher/LauncherActivity.kt
app/src/main/java/com/lyy/keepassa/view/launcher/OpenDbFragment.kt
app/src/main/java/com/lyy/keepassa/view/main/QuickUnlockActivity.kt
app/src/main/res/values/strings.xml
```

Do not remove or rewrite:

```text
app/src/main/java/com/lyy/keepassa/service/autofill/AutoFillService.kt
app/src/main/java/com/lyy/keepassa/service/autofill/AutofillEntryLookup.kt
```

`CredentialPasswordRepository` should reuse `AutofillEntryLookup.find(packageName, domain)` so Credential Manager gets the same domain-first/package-fallback behavior already used by updated autofill code.

---

### Task 1: Add Credential Lookup Mapping

**Files:**
- Create: `app/src/main/java/com/lyy/keepassa/service/credential/CredentialLookupTarget.kt`
- Create: `app/src/main/java/com/lyy/keepassa/service/credential/CredentialLookupTargetMapper.kt`
- Test: `app/src/test/java/com/lyy/keepassa/service/credential/CredentialLookupTargetMapperTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/lyy/keepassa/service/credential/CredentialLookupTargetMapperTest.kt`:

```kotlin
package com.lyy.keepassa.service.credential

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CredentialLookupTargetMapperTest {

  @Test fun mapsPackageNameAndNormalizesDomain() {
    val target = CredentialLookupTargetMapper.from(
      packageName = " com.example.app ",
      origin = "https://Login.Example.com/path"
    )

    assertEquals("com.example.app", target?.packageName)
    assertEquals("login.example.com", target?.domain)
  }

  @Test fun mapsPackageNameWithoutDomain() {
    val target = CredentialLookupTargetMapper.from(
      packageName = "com.example.app",
      origin = null
    )

    assertEquals("com.example.app", target?.packageName)
    assertNull(target?.domain)
  }

  @Test fun returnsNullForBlankPackageName() {
    val target = CredentialLookupTargetMapper.from(
      packageName = " ",
      origin = "https://example.com"
    )

    assertNull(target)
  }

  @Test fun stripsAndroidApkOriginIntoNoDomain() {
    val target = CredentialLookupTargetMapper.from(
      packageName = "com.example.app",
      origin = "android:apk-key-hash:abc"
    )

    assertEquals("com.example.app", target?.packageName)
    assertNull(target?.domain)
  }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```powershell
.\gradlew.bat :app:testDevDebugUnitTest --tests "com.lyy.keepassa.service.credential.CredentialLookupTargetMapperTest"
```

Expected: FAIL because `CredentialLookupTargetMapper` does not exist.

- [ ] **Step 3: Add the minimal implementation**

Create `app/src/main/java/com/lyy/keepassa/service/credential/CredentialLookupTarget.kt`:

```kotlin
/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.service.credential

internal data class CredentialLookupTarget(
  val packageName: String,
  val domain: String?
)
```

Create `app/src/main/java/com/lyy/keepassa/service/credential/CredentialLookupTargetMapper.kt`:

```kotlin
/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.service.credential

import java.net.URI

internal object CredentialLookupTargetMapper {

  fun from(
    packageName: String?,
    origin: String?
  ): CredentialLookupTarget? {
    val normalizedPackage = packageName?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return CredentialLookupTarget(
      packageName = normalizedPackage,
      domain = normalizeDomain(origin)
    )
  }

  private fun normalizeDomain(origin: String?): String? {
    val raw = origin?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    if (raw.startsWith("android:", ignoreCase = true)) {
      return null
    }
    return runCatching {
      val uri = URI(raw)
      uri.host?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
    }.getOrNull()
  }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run:

```powershell
.\gradlew.bat :app:testDevDebugUnitTest --tests "com.lyy.keepassa.service.credential.CredentialLookupTargetMapperTest"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/lyy/keepassa/service/credential/CredentialLookupTarget.kt `
  app/src/main/java/com/lyy/keepassa/service/credential/CredentialLookupTargetMapper.kt `
  app/src/test/java/com/lyy/keepassa/service/credential/CredentialLookupTargetMapperTest.kt
git commit -m "feat: add credential lookup target mapping"
```

---

### Task 2: Add Save Draft Mapping and Entry Association Rules

**Files:**
- Create: `app/src/main/java/com/lyy/keepassa/service/credential/CredentialSaveDraft.kt`
- Create: `app/src/main/java/com/lyy/keepassa/service/credential/CredentialSaveRequestMapper.kt`
- Modify: `app/src/main/java/com/lyy/keepassa/view/create/entry/AutoFillSaveEntryBinder.kt`
- Test: `app/src/test/java/com/lyy/keepassa/service/credential/CredentialSaveRequestMapperTest.kt`
- Test: `app/src/test/java/com/lyy/keepassa/view/create/entry/AutoFillSaveEntryBinderTest.kt`

- [ ] **Step 1: Write the failing mapper test**

Create `app/src/test/java/com/lyy/keepassa/service/credential/CredentialSaveRequestMapperTest.kt`:

```kotlin
package com.lyy.keepassa.service.credential

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CredentialSaveRequestMapperTest {

  @Test fun mapsValidPasswordSaveRequest() {
    val draft = CredentialSaveRequestMapper.from(
      packageName = " com.example.app ",
      origin = "https://example.com/login",
      username = " alice ",
      password = "secret"
    )

    assertEquals("com.example.app", draft?.packageName)
    assertEquals("example.com", draft?.domain)
    assertEquals("alice", draft?.username)
    assertEquals("secret", draft?.password)
  }

  @Test fun returnsNullWhenPackageIsBlank() {
    assertNull(
      CredentialSaveRequestMapper.from(
        packageName = "",
        origin = "https://example.com",
        username = "alice",
        password = "secret"
      )
    )
  }

  @Test fun returnsNullWhenUsernameIsBlank() {
    assertNull(
      CredentialSaveRequestMapper.from(
        packageName = "com.example.app",
        origin = "https://example.com",
        username = " ",
        password = "secret"
      )
    )
  }

  @Test fun returnsNullWhenPasswordIsBlank() {
    assertNull(
      CredentialSaveRequestMapper.from(
        packageName = "com.example.app",
        origin = "https://example.com",
        username = "alice",
        password = ""
      )
    )
  }
}
```

- [ ] **Step 2: Extend the existing binder test with domain plus package association**

Append this test to `app/src/test/java/com/lyy/keepassa/view/create/entry/AutoFillSaveEntryBinderTest.kt`:

```kotlin
  @Test fun prepareCustomFieldsForCreateUi_addsPackageAssociationEvenWhenDomainExists() {
    val strings = linkedMapOf<String, ProtectedString>()
    val param = AutoFillParam(
      apkPkgName = "com.lyy.autofill.savedemo",
      domain = "example.com",
      isSave = true
    )

    val changed = AutoFillSaveEntryBinder.prepareCustomFieldsForCreateUi(strings, param)

    assertTrue(changed)
    assertEquals("androidapp://com.lyy.autofill.savedemo", strings["KP2A_URL_1"].toString())
  }
```

If the file does not already import `assertTrue`, add:

```kotlin
import org.junit.Assert.assertTrue
```

- [ ] **Step 3: Run tests to verify they fail**

Run:

```powershell
.\gradlew.bat :app:testDevDebugUnitTest --tests "com.lyy.keepassa.service.credential.CredentialSaveRequestMapperTest" --tests "com.lyy.keepassa.view.create.entry.AutoFillSaveEntryBinderTest"
```

Expected: FAIL because the mapper does not exist and `AutoFillSaveEntryBinder` currently skips package association when domain exists.

- [ ] **Step 4: Add save draft implementation**

Create `app/src/main/java/com/lyy/keepassa/service/credential/CredentialSaveDraft.kt`:

```kotlin
/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.service.credential

internal data class CredentialSaveDraft(
  val packageName: String,
  val domain: String?,
  val username: String,
  val password: String
)
```

Create `app/src/main/java/com/lyy/keepassa/service/credential/CredentialSaveRequestMapper.kt`:

```kotlin
/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.service.credential

internal object CredentialSaveRequestMapper {

  fun from(
    packageName: String?,
    origin: String?,
    username: String?,
    password: String?
  ): CredentialSaveDraft? {
    val target = CredentialLookupTargetMapper.from(packageName, origin) ?: return null
    val normalizedUsername = username?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val normalizedPassword = password?.takeIf { it.isNotEmpty() } ?: return null
    return CredentialSaveDraft(
      packageName = target.packageName,
      domain = target.domain,
      username = normalizedUsername,
      password = normalizedPassword
    )
  }
}
```

- [ ] **Step 5: Modify `AutoFillSaveEntryBinder`**

Change `prepareCustomFieldsForCreateUi()` and `applyPackageAssociation(autoFillParam)` in `app/src/main/java/com/lyy/keepassa/view/create/entry/AutoFillSaveEntryBinder.kt` to:

```kotlin
  fun prepareCustomFieldsForCreateUi(
    strings: MutableMap<String, ProtectedString>,
    autoFillParam: AutoFillParam?
  ): Boolean {
    return applyPackageAssociation(strings, autoFillParam)
  }

  fun applyPackageAssociation(
    strings: MutableMap<String, ProtectedString>,
    autoFillParam: AutoFillParam?
  ): Boolean {
    if (autoFillParam?.isSave != true) {
      return false
    }
    return applyPackageAssociation(strings, autoFillParam.apkPkgName)
  }
```

Change `getPackageAssociationUrl()` to allow domain saves:

```kotlin
  private fun getPackageAssociationUrl(autoFillParam: AutoFillParam?): String? {
    if (autoFillParam?.isSave != true) {
      return null
    }
    val normalizedPackageName = autoFillParam.apkPkgName.trim().takeIf { it.isNotEmpty() }
      ?: return null
    return "$ANDROID_APP_URL_PREFIX$normalizedPackageName"
  }
```

- [ ] **Step 6: Run tests to verify they pass**

Run:

```powershell
.\gradlew.bat :app:testDevDebugUnitTest --tests "com.lyy.keepassa.service.credential.CredentialSaveRequestMapperTest" --tests "com.lyy.keepassa.view.create.entry.AutoFillSaveEntryBinderTest"
```

Expected: PASS.

- [ ] **Step 7: Commit**

```powershell
git add app/src/main/java/com/lyy/keepassa/service/credential/CredentialSaveDraft.kt `
  app/src/main/java/com/lyy/keepassa/service/credential/CredentialSaveRequestMapper.kt `
  app/src/main/java/com/lyy/keepassa/view/create/entry/AutoFillSaveEntryBinder.kt `
  app/src/test/java/com/lyy/keepassa/service/credential/CredentialSaveRequestMapperTest.kt `
  app/src/test/java/com/lyy/keepassa/view/create/entry/AutoFillSaveEntryBinderTest.kt
git commit -m "feat: map credential password save drafts"
```

---

### Task 3: Register Credential Provider Capability

**Files:**
- Modify: `app/build.gradle`
- Modify: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/xml/credential_provider.xml`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Add dependency**

In `app/build.gradle`, add this near the existing Android component dependencies:

```gradle
  implementation(libs.jetpack.credentials) // Credential Manager provider integration
```

- [ ] **Step 2: Add provider XML**

Create `app/src/main/res/xml/credential_provider.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<credential-provider xmlns:android="http://schemas.android.com/apk/res/android">
  <capabilities>
    <capability name="android.credentials.TYPE_PASSWORD_CREDENTIAL" />
  </capabilities>
</credential-provider>
```

- [ ] **Step 3: Add strings**

Add these entries to `app/src/main/res/values/strings.xml`:

```xml
    <string name="credential_provider_unlock_title"><![CDATA[Unlock KeePassA]]></string>
    <string name="credential_provider_save_title"><![CDATA[Save to KeePassA]]></string>
    <string name="credential_provider_no_match"><![CDATA[No matching credential]]></string>
```

- [ ] **Step 4: Register service and bridge activities**

Add these entries in `app/src/main/AndroidManifest.xml` inside `<application>`:

```xml
    <activity
        android:name=".view.credential.CredentialGetActivity"
        android:exported="false"
        android:theme="@style/DialogActivityStyle" />

    <activity
        android:name=".view.credential.CredentialSaveActivity"
        android:exported="false"
        android:theme="@style/DialogActivityStyle" />

    <service
        android:name=".service.credential.KeepassACredentialProviderService"
        android:enabled="true"
        android:exported="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:permission="android.permission.BIND_CREDENTIAL_PROVIDER_SERVICE"
        tools:targetApi="upside_down_cake">
      <intent-filter>
        <action android:name="android.service.credentials.CredentialProviderService" />
      </intent-filter>
      <meta-data
          android:name="android.credentials.provider"
          android:resource="@xml/credential_provider" />
    </service>
```

- [ ] **Step 5: Verify registration files exist**

Run:

```powershell
Test-Path app\src\main\res\xml\credential_provider.xml
Select-String -Path app\src\main\AndroidManifest.xml -Pattern "CredentialProviderService|android.credentials.provider"
```

Expected:

```text
True
```

The `Select-String` output includes both `CredentialProviderService` and `android.credentials.provider`.

- [ ] **Step 6: Commit after Task 4 classes exist**

After Task 4 creates `KeepassACredentialProviderService`, `CredentialGetActivity`, and `CredentialSaveActivity`, run:

```powershell
git add app/build.gradle app/src/main/AndroidManifest.xml app/src/main/res/xml/credential_provider.xml app/src/main/res/values/strings.xml
git commit -m "feat: register credential manager provider"
```

---

### Task 4: Add Pending Intent Factory and Provider Skeleton

**Files:**
- Create: `app/src/main/java/com/lyy/keepassa/service/credential/CredentialProviderPendingIntents.kt`
- Create: `app/src/main/java/com/lyy/keepassa/service/credential/CredentialPasswordRepository.kt`
- Create: `app/src/main/java/com/lyy/keepassa/service/credential/KeepassACredentialProviderService.kt`
- Create: `app/src/main/java/com/lyy/keepassa/view/credential/CredentialGetActivity.kt`
- Create: `app/src/main/java/com/lyy/keepassa/view/credential/CredentialSaveActivity.kt`
- Test: `app/src/test/java/com/lyy/keepassa/service/credential/CredentialProviderPendingIntentsTest.kt`

- [ ] **Step 1: Write failing pure pending intent metadata test**

Create `app/src/test/java/com/lyy/keepassa/service/credential/CredentialProviderPendingIntentsTest.kt`:

```kotlin
package com.lyy.keepassa.service.credential

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.util.UUID

class CredentialProviderPendingIntentsTest {

  @Test fun getExtrasDoNotContainPassword() {
    val entryId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    val extras = CredentialProviderPendingIntents.getPasswordExtras(
      entryId = entryId,
      target = CredentialLookupTarget(
        packageName = "com.example.app",
        domain = "example.com"
      )
    )

    assertEquals(entryId.toString(), extras[CredentialProviderPendingIntents.EXTRA_ENTRY_ID])
    assertEquals("com.example.app", extras[CredentialProviderPendingIntents.EXTRA_PACKAGE_NAME])
    assertEquals("example.com", extras[CredentialProviderPendingIntents.EXTRA_DOMAIN])
    assertFalse(extras.containsKey("password"))
    assertFalse(extras.containsKey("pass"))
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
.\gradlew.bat :app:testDevDebugUnitTest --tests "com.lyy.keepassa.service.credential.CredentialProviderPendingIntentsTest"
```

Expected: FAIL because `CredentialProviderPendingIntents` does not exist.

- [ ] **Step 3: Add pending intent factory**

Create `app/src/main/java/com/lyy/keepassa/service/credential/CredentialProviderPendingIntents.kt`:

```kotlin
/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.service.credential

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.lyy.keepassa.view.credential.CredentialGetActivity
import com.lyy.keepassa.view.credential.CredentialSaveActivity
import java.util.UUID

internal object CredentialProviderPendingIntents {
  const val EXTRA_ENTRY_ID = "credential_entry_id"
  const val EXTRA_PACKAGE_NAME = "credential_package_name"
  const val EXTRA_DOMAIN = "credential_domain"
  const val REQ_GET_PASSWORD = 4101
  const val REQ_SAVE_PASSWORD = 4102

  fun getPasswordExtras(
    entryId: UUID,
    target: CredentialLookupTarget
  ): Map<String, String> {
    return buildMap {
      put(EXTRA_ENTRY_ID, entryId.toString())
      put(EXTRA_PACKAGE_NAME, target.packageName)
      target.domain?.let { put(EXTRA_DOMAIN, it) }
    }
  }

  fun createGetPasswordPendingIntent(
    context: Context,
    entryId: UUID,
    target: CredentialLookupTarget
  ): PendingIntent {
    val intent = Intent(context, CredentialGetActivity::class.java).apply {
      getPasswordExtras(entryId, target).forEach { (key, value) -> putExtra(key, value) }
    }
    return PendingIntent.getActivity(
      context,
      REQ_GET_PASSWORD,
      intent,
      PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
  }

  fun createSavePasswordPendingIntent(context: Context): PendingIntent {
    return PendingIntent.getActivity(
      context,
      REQ_SAVE_PASSWORD,
      Intent(context, CredentialSaveActivity::class.java),
      PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
  }
}
```

- [ ] **Step 4: Add repository wrapper**

Create `app/src/main/java/com/lyy/keepassa/service/credential/CredentialPasswordRepository.kt`:

```kotlin
/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.service.credential

import com.keepassdroid.database.PwEntry
import com.lyy.keepassa.service.autofill.AutofillEntryLookup

internal object CredentialPasswordRepository {
  fun find(target: CredentialLookupTarget): MutableList<PwEntry>? {
    return AutofillEntryLookup.find(
      packageName = target.packageName,
      domain = target.domain
    )
  }
}
```

- [ ] **Step 5: Add provider skeleton**

Create `app/src/main/java/com/lyy/keepassa/service/credential/KeepassACredentialProviderService.kt`:

```kotlin
/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.service.credential

import android.os.CancellationSignal
import android.os.OutcomeReceiver
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.provider.BeginCreateCredentialRequest
import androidx.credentials.provider.BeginCreateCredentialResponse
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.BeginGetCredentialResponse
import androidx.credentials.provider.CredentialProviderService

class KeepassACredentialProviderService : CredentialProviderService() {

  override fun onBeginGetCredentialRequest(
    request: BeginGetCredentialRequest,
    cancellationSignal: CancellationSignal,
    callback: OutcomeReceiver<BeginGetCredentialResponse, GetCredentialException>
  ) {
    callback.onResult(BeginGetCredentialResponse())
  }

  override fun onBeginCreateCredentialRequest(
    request: BeginCreateCredentialRequest,
    cancellationSignal: CancellationSignal,
    callback: OutcomeReceiver<BeginCreateCredentialResponse, CreateCredentialException>
  ) {
    callback.onResult(BeginCreateCredentialResponse())
  }
}
```

- [ ] **Step 6: Add bridge activity skeletons**

Create `app/src/main/java/com/lyy/keepassa/view/credential/CredentialGetActivity.kt`:

```kotlin
/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.view.credential

import android.app.Activity
import android.os.Bundle

class CredentialGetActivity : Activity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setResult(RESULT_CANCELED)
    finish()
  }
}
```

Create `app/src/main/java/com/lyy/keepassa/view/credential/CredentialSaveActivity.kt`:

```kotlin
/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.view.credential

import android.app.Activity
import android.os.Bundle

class CredentialSaveActivity : Activity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setResult(RESULT_CANCELED)
    finish()
  }
}
```

- [ ] **Step 7: Run tests and build**

Run:

```powershell
.\gradlew.bat :app:testDevDebugUnitTest --tests "com.lyy.keepassa.service.credential.CredentialProviderPendingIntentsTest"
.\gradlew.bat :app:assembleDevDebug
```

Expected: test PASS and build SUCCESS.

- [ ] **Step 8: Commit with Task 3 if Task 3 was waiting on classes**

```powershell
git add app/src/main/java/com/lyy/keepassa/service/credential/CredentialProviderPendingIntents.kt `
  app/src/main/java/com/lyy/keepassa/service/credential/CredentialPasswordRepository.kt `
  app/src/main/java/com/lyy/keepassa/service/credential/KeepassACredentialProviderService.kt `
  app/src/main/java/com/lyy/keepassa/view/credential/CredentialGetActivity.kt `
  app/src/main/java/com/lyy/keepassa/view/credential/CredentialSaveActivity.kt `
  app/src/test/java/com/lyy/keepassa/service/credential/CredentialProviderPendingIntentsTest.kt
git commit -m "feat: add credential provider skeleton"
```

---

### Task 5: Implement Password Get Entries and Result Activity

**Files:**
- Modify: `app/src/main/java/com/lyy/keepassa/service/credential/KeepassACredentialProviderService.kt`
- Modify: `app/src/main/java/com/lyy/keepassa/view/credential/CredentialGetActivity.kt`

- [ ] **Step 1: Implement begin-get password handling**

Replace `onBeginGetCredentialRequest()` in `KeepassACredentialProviderService.kt` with:

```kotlin
  override fun onBeginGetCredentialRequest(
    request: BeginGetCredentialRequest,
    cancellationSignal: CancellationSignal,
    callback: OutcomeReceiver<BeginGetCredentialResponse, GetCredentialException>
  ) {
    if (BaseApp.KDB == null || BaseApp.isLocked) {
      callback.onResult(
        BeginGetCredentialResponse(
          authenticationActions = mutableListOf(
            AuthenticationAction(
              getString(R.string.credential_provider_unlock_title),
              CredentialProviderPendingIntents.createSavePasswordPendingIntent(this)
            )
          )
        )
      )
      return
    }

    val packageName = request.callingAppInfo?.packageName
    val browserDomain = packageName?.let { AutofillBrowserAuthContextStore.find(it)?.domain }
    val target = CredentialLookupTargetMapper.from(
      packageName = packageName,
      origin = browserDomain?.let { "https://$it" }
    )
    if (target == null) {
      callback.onResult(BeginGetCredentialResponse())
      return
    }

    val credentialEntries = mutableListOf<CredentialEntry>()
    request.beginGetCredentialOptions.forEach { option ->
      if (option is BeginGetPasswordOption) {
        CredentialPasswordRepository.find(target)?.forEach { entry ->
          val username = KdbUtil.getUserName(entry).takeIf { it.isNotBlank() } ?: return@forEach
          credentialEntries.add(
            PasswordCredentialEntry(
              context = applicationContext,
              username = username,
              pendingIntent = CredentialProviderPendingIntents.createGetPasswordPendingIntent(
                context = this,
                entryId = entry.uuid,
                target = target
              ),
              beginGetPasswordOption = option,
              displayName = entry.title,
              icon = null
            )
          )
        }
      }
    }

    callback.onResult(BeginGetCredentialResponse(credentialEntries = credentialEntries))
  }
```

Add imports:

```kotlin
import androidx.credentials.provider.AuthenticationAction
import androidx.credentials.provider.BeginGetPasswordOption
import androidx.credentials.provider.CredentialEntry
import androidx.credentials.provider.PasswordCredentialEntry
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.service.autofill.AutofillBrowserAuthContextStore
import com.lyy.keepassa.util.KdbUtil
```

- [ ] **Step 2: Implement `CredentialGetActivity` result**

Replace `CredentialGetActivity.kt` with:

```kotlin
/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.view.credential

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.credentials.PasswordCredential
import androidx.credentials.GetCredentialResponse
import androidx.credentials.provider.PendingIntentHandler
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.service.credential.CredentialProviderPendingIntents
import com.lyy.keepassa.util.KdbUtil
import timber.log.Timber
import java.util.UUID

class CredentialGetActivity : Activity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val providerRequest = PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)
    if (providerRequest == null) {
      cancel()
      return
    }
    if (BaseApp.KDB == null || BaseApp.isLocked) {
      Timber.w("CredentialGetActivity opened while database is locked")
      cancel()
      return
    }
    val entryId = intent.getStringExtra(CredentialProviderPendingIntents.EXTRA_ENTRY_ID)
      ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
    if (entryId == null) {
      cancel()
      return
    }
    val entry = BaseApp.KDB.pm.entries[entryId]
    if (entry == null) {
      cancel()
      return
    }
    val username = KdbUtil.getUserName(entry).takeIf { it.isNotBlank() }
    val password = KdbUtil.getPassword(entry).takeIf { it.isNotEmpty() }
    if (username == null || password == null) {
      cancel()
      return
    }

    val result = Intent()
    PendingIntentHandler.setGetCredentialResponse(
      result,
      GetCredentialResponse(PasswordCredential(username, password))
    )
    setResult(RESULT_OK, result)
    finish()
  }

  private fun cancel() {
    setResult(RESULT_CANCELED)
    finish()
  }
}
```

- [ ] **Step 3: Run focused build**

Run:

```powershell
.\gradlew.bat :app:assembleDevDebug
```

Expected: SUCCESS.

- [ ] **Step 4: Manual smoke test on Android 14+**

Use a test app that calls:

```kotlin
CredentialManager.create(context).getCredential(
  context = activity,
  request = GetCredentialRequest(listOf(GetPasswordOption()))
)
```

Expected with unlocked KeePassA database: KeePassA password entries appear in the system selector and selecting one returns a `PasswordCredential`.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/lyy/keepassa/service/credential/KeepassACredentialProviderService.kt `
  app/src/main/java/com/lyy/keepassa/view/credential/CredentialGetActivity.kt
git commit -m "feat: return credential manager password entries"
```

---

### Task 6: Implement Password Save Entry and Bridge Activity

**Files:**
- Modify: `app/src/main/java/com/lyy/keepassa/service/credential/KeepassACredentialProviderService.kt`
- Modify: `app/src/main/java/com/lyy/keepassa/view/credential/CredentialSaveActivity.kt`

- [ ] **Step 1: Implement begin-create password handling**

Replace `onBeginCreateCredentialRequest()` in `KeepassACredentialProviderService.kt` with:

```kotlin
  override fun onBeginCreateCredentialRequest(
    request: BeginCreateCredentialRequest,
    cancellationSignal: CancellationSignal,
    callback: OutcomeReceiver<BeginCreateCredentialResponse, CreateCredentialException>
  ) {
    if (request !is BeginCreatePasswordCredentialRequest) {
      callback.onResult(BeginCreateCredentialResponse())
      return
    }
    callback.onResult(
      BeginCreateCredentialResponse(
        createEntries = mutableListOf(
          CreateEntry(
            getString(R.string.credential_provider_save_title),
            CredentialProviderPendingIntents.createSavePasswordPendingIntent(this)
          )
        )
      )
    )
  }
```

Add imports:

```kotlin
import androidx.credentials.provider.BeginCreatePasswordCredentialRequest
import androidx.credentials.provider.CreateEntry
```

- [ ] **Step 2: Implement save bridge activity**

Replace `CredentialSaveActivity.kt` with:

```kotlin
/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.view.credential

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContract
import androidx.credentials.CreatePasswordResponse
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.provider.PendingIntentHandler
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.entity.AutoFillParam
import com.lyy.keepassa.service.autofill.AutofillBrowserAuthContextStore
import com.lyy.keepassa.service.credential.CredentialSaveRequestMapper
import com.lyy.keepassa.view.create.entry.CreateEntryActivity
import com.lyy.keepassa.view.launcher.LauncherActivity

class CredentialSaveActivity : Activity() {

  private val createEntryLauncher = registerForActivityResult(
    object : ActivityResultContract<AutoFillParam, Boolean>() {
      override fun createIntent(context: android.content.Context, input: AutoFillParam): Intent {
        return Intent(context, CreateEntryActivity::class.java).apply {
          putExtra(LauncherActivity.KEY_AUTO_FILL_PARAM, input)
          putExtra(CreateEntryActivity.EXTRA_FINISH_WITH_RESULT, true)
        }
      }

      override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
        return resultCode == RESULT_OK
      }
    }
  ) { saved ->
    if (saved) {
      val result = Intent()
      PendingIntentHandler.setCreateCredentialResponse(result, CreatePasswordResponse())
      setResult(RESULT_OK, result)
    } else {
      setResult(RESULT_CANCELED)
    }
    finish()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val providerRequest = PendingIntentHandler.retrieveProviderCreateCredentialRequest(intent)
    val passwordRequest = providerRequest?.callingRequest as? CreatePasswordRequest
    if (providerRequest == null || passwordRequest == null) {
      cancel()
      return
    }
    if (BaseApp.KDB == null || BaseApp.isLocked) {
      cancel()
      return
    }
    val packageName = providerRequest.callingAppInfo.packageName
    val browserDomain = AutofillBrowserAuthContextStore.find(packageName)?.domain
    val draft = CredentialSaveRequestMapper.from(
      packageName = packageName,
      origin = browserDomain?.let { "https://$it" },
      username = passwordRequest.id,
      password = passwordRequest.password
    )
    if (draft == null) {
      cancel()
      return
    }
    createEntryLauncher.launch(
      AutoFillParam(
        apkPkgName = draft.packageName,
        domain = draft.domain,
        isSave = true,
        saveUserName = draft.username,
        savePass = draft.password
      )
    )
  }

  private fun cancel() {
    setResult(RESULT_CANCELED)
    finish()
  }
}
```

- [ ] **Step 3: Run build**

Run:

```powershell
.\gradlew.bat :app:assembleDevDebug
```

Expected: compile failure because `CreateEntryActivity.EXTRA_FINISH_WITH_RESULT` does not exist yet. This is the expected red state for Task 7.

- [ ] **Step 4: Do not commit yet**

Task 6 depends on Task 7 result support. Commit after Task 7 passes.

---

### Task 7: Add CreateEntryActivity Result Mode for Credential Saves

**Files:**
- Create: `app/src/main/java/com/lyy/keepassa/view/create/entry/CreateEntryCredentialResultPolicy.kt`
- Modify: `app/src/main/java/com/lyy/keepassa/view/create/entry/CreateEntryActivity.kt`
- Modify: `app/src/main/java/com/lyy/keepassa/view/create/entry/CreateEntryModule.kt`
- Test: `app/src/test/java/com/lyy/keepassa/view/create/entry/CreateEntryCredentialResultPolicyTest.kt`

- [ ] **Step 1: Write failing policy test**

Create `app/src/test/java/com/lyy/keepassa/view/create/entry/CreateEntryCredentialResultPolicyTest.kt`:

```kotlin
package com.lyy.keepassa.view.create.entry

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateEntryCredentialResultPolicyTest {

  @Test fun returnsOkOnlyForCredentialResultModeAndSuccessfulSave() {
    assertTrue(
      CreateEntryCredentialResultPolicy.shouldReturnOk(
        finishWithResult = true,
        saveSucceeded = true
      )
    )
  }

  @Test fun doesNotReturnOkForNormalCreateFlow() {
    assertFalse(
      CreateEntryCredentialResultPolicy.shouldReturnOk(
        finishWithResult = false,
        saveSucceeded = true
      )
    )
  }

  @Test fun doesNotReturnOkWhenSaveFails() {
    assertFalse(
      CreateEntryCredentialResultPolicy.shouldReturnOk(
        finishWithResult = true,
        saveSucceeded = false
      )
    )
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
.\gradlew.bat :app:testDevDebugUnitTest --tests "com.lyy.keepassa.view.create.entry.CreateEntryCredentialResultPolicyTest"
```

Expected: FAIL because `CreateEntryCredentialResultPolicy` does not exist.

- [ ] **Step 3: Add policy implementation**

Create `app/src/main/java/com/lyy/keepassa/view/create/entry/CreateEntryCredentialResultPolicy.kt`:

```kotlin
/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.lyy.keepassa.view.create.entry

internal object CreateEntryCredentialResultPolicy {
  fun shouldReturnOk(
    finishWithResult: Boolean,
    saveSucceeded: Boolean
  ): Boolean {
    return finishWithResult && saveSucceeded
  }
}
```

- [ ] **Step 4: Modify `CreateEntryActivity`**

Add this constant to `CreateEntryActivity.Companion`:

```kotlin
    const val EXTRA_FINISH_WITH_RESULT = "EXTRA_FINISH_WITH_RESULT"
```

Add this method to `CreateEntryActivity`:

```kotlin
  internal fun finishAfterEntrySave(saveSucceeded: Boolean) {
    val finishWithResult = intent.getBooleanExtra(EXTRA_FINISH_WITH_RESULT, false)
    if (CreateEntryCredentialResultPolicy.shouldReturnOk(finishWithResult, saveSucceeded)) {
      setResult(Activity.RESULT_OK)
    }
    finishAfterTransition()
  }
```

- [ ] **Step 5: Modify `CreateEntryModule` save completion**

In `CreateEntryModule.updateEntryGroupIdAndSave()`, replace:

```kotlin
        context.finishAfterTransition()
```

with:

```kotlin
        context.finishAfterEntrySave(state == DbSynUtil.STATE_SUCCEED)
```

- [ ] **Step 6: Run tests and build**

Run:

```powershell
.\gradlew.bat :app:testDevDebugUnitTest --tests "com.lyy.keepassa.view.create.entry.CreateEntryCredentialResultPolicyTest"
.\gradlew.bat :app:assembleDevDebug
```

Expected: PASS and build SUCCESS.

- [ ] **Step 7: Commit Task 6 and Task 7 together**

```powershell
git add app/src/main/java/com/lyy/keepassa/service/credential/KeepassACredentialProviderService.kt `
  app/src/main/java/com/lyy/keepassa/view/credential/CredentialSaveActivity.kt `
  app/src/main/java/com/lyy/keepassa/view/create/entry/CreateEntryCredentialResultPolicy.kt `
  app/src/main/java/com/lyy/keepassa/view/create/entry/CreateEntryActivity.kt `
  app/src/main/java/com/lyy/keepassa/view/create/entry/CreateEntryModule.kt `
  app/src/test/java/com/lyy/keepassa/view/create/entry/CreateEntryCredentialResultPolicyTest.kt
git commit -m "feat: save credential manager passwords through entry editor"
```

---

### Task 8: Add Unlock Result Mode and Continue Credential Requests

**Files:**
- Modify: `app/src/main/java/com/lyy/keepassa/service/credential/KeepassACredentialProviderService.kt`
- Modify: `app/src/main/java/com/lyy/keepassa/view/credential/CredentialGetActivity.kt`
- Modify: `app/src/main/java/com/lyy/keepassa/view/credential/CredentialSaveActivity.kt`
- Modify: `app/src/main/java/com/lyy/keepassa/view/launcher/LauncherActivity.kt`
- Modify: `app/src/main/java/com/lyy/keepassa/view/launcher/OpenDbFragment.kt`
- Modify: `app/src/main/java/com/lyy/keepassa/view/main/QuickUnlockActivity.kt`

- [ ] **Step 1: Add unlock result constants**

In `LauncherActivity.Companion`, add:

```kotlin
    const val EXTRA_RETURN_UNLOCK_RESULT = "EXTRA_RETURN_UNLOCK_RESULT"
    const val REQ_CODE_CREDENTIAL_UNLOCK = 3
```

Add these methods to `LauncherActivity`:

```kotlin
  internal fun shouldReturnUnlockResult(): Boolean {
    return intent.getBooleanExtra(EXTRA_RETURN_UNLOCK_RESULT, false)
  }

  internal fun finishUnlockResult(success: Boolean) {
    setResult(if (success) Activity.RESULT_OK else Activity.RESULT_CANCELED)
    superFinish()
  }
```

Add this helper to `LauncherActivity.Companion`:

```kotlin
    internal fun createUnlockResultIntent(context: Context): Intent {
      return Intent(context, LauncherActivity::class.java).apply {
        putExtra(EXTRA_RETURN_UNLOCK_RESULT, true)
      }
    }
```

- [ ] **Step 2: Return result from full database unlock**

In `OpenDbFragment.listenerOpenDb()`, replace:

```kotlin
        modlue.autoFillParam?.let {
          Timber.d("自动填充，不进入首页")
          modlue.autoFillDelegate?.handleAutoFill(it)
          return@collectLatest
        }
        Routerfit.create(ActivityRouter::class.java, requireActivity()).toMainActivity(
          opt = ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity())
        )
```

with:

```kotlin
        val launcherActivity = requireActivity() as LauncherActivity
        if (launcherActivity.shouldReturnUnlockResult()) {
          launcherActivity.finishUnlockResult(true)
          return@collectLatest
        }
        modlue.autoFillParam?.let {
          Timber.d("自动填充，不进入首页")
          modlue.autoFillDelegate?.handleAutoFill(it)
          return@collectLatest
        }
        Routerfit.create(ActivityRouter::class.java, requireActivity()).toMainActivity(
          opt = ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity())
        )
```

- [ ] **Step 3: Return result from quick unlock**

In `QuickUnlockActivity.Companion`, add:

```kotlin
    internal fun createQuickUnlockResultIntent(context: Context): Intent {
      return Intent(context, QuickUnlockActivity::class.java).apply {
        putExtra(LauncherActivity.EXTRA_RETURN_UNLOCK_RESULT, true)
      }
    }
```

In `QuickUnlockActivity.turnActivity()`, immediately after:

```kotlin
    NotificationUtil.startDbOpenNotify(this@QuickUnlockActivity)
```

add:

```kotlin
    if (intent.getBooleanExtra(LauncherActivity.EXTRA_RETURN_UNLOCK_RESULT, false)) {
      setResult(Activity.RESULT_OK)
      finish()
      return
    }
```

- [ ] **Step 4: Fix unlock pending intent target**

In `KeepassACredentialProviderService.onBeginGetCredentialRequest()`, replace the temporary save pending intent used by the unlock action with a get pending intent action that opens `CredentialGetActivity` without an entry id.

Add this method to `CredentialProviderPendingIntents.kt`:

```kotlin
  fun createUnlockPendingIntent(context: Context): PendingIntent {
    return PendingIntent.getActivity(
      context,
      REQ_GET_PASSWORD,
      Intent(context, CredentialGetActivity::class.java),
      PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
  }
```

Then change:

```kotlin
CredentialProviderPendingIntents.createSavePasswordPendingIntent(this)
```

to:

```kotlin
CredentialProviderPendingIntents.createUnlockPendingIntent(this)
```

- [ ] **Step 5: Add unlock launchers to `CredentialGetActivity`**

In `CredentialGetActivity`, add this launcher:

```kotlin
  private val unlockLauncher = registerForActivityResult(
    object : ActivityResultContract<Unit, Boolean>() {
      override fun createIntent(context: android.content.Context, input: Unit): Intent {
        return if (BaseApp.KDB != null && BaseApp.APP.isCanOpenQuickLock()) {
          QuickUnlockActivity.createQuickUnlockResultIntent(context)
        } else {
          LauncherActivity.createUnlockResultIntent(context)
        }
      }

      override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
        return resultCode == RESULT_OK
      }
    }
  ) { unlocked ->
    if (unlocked) {
      completeGet()
    } else {
      cancel()
    }
  }
```

Add imports:

```kotlin
import androidx.activity.result.contract.ActivityResultContract
import com.lyy.keepassa.view.launcher.LauncherActivity
import com.lyy.keepassa.view.main.QuickUnlockActivity
```

Refactor `onCreate()` so it calls `completeGet()`:

```kotlin
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (BaseApp.KDB == null || BaseApp.isLocked) {
      unlockLauncher.launch(Unit)
      return
    }
    completeGet()
  }
```

Move the existing result-building code from `onCreate()` into:

```kotlin
  private fun completeGet() {
    val providerRequest = PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)
    if (providerRequest == null) {
      cancel()
      return
    }
    if (BaseApp.KDB == null || BaseApp.isLocked) {
      cancel()
      return
    }
    val entryId = intent.getStringExtra(CredentialProviderPendingIntents.EXTRA_ENTRY_ID)
      ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
    if (entryId == null) {
      cancel()
      return
    }
    val entry = BaseApp.KDB.pm.entries[entryId]
    if (entry == null) {
      cancel()
      return
    }
    val username = KdbUtil.getUserName(entry).takeIf { it.isNotBlank() }
    val password = KdbUtil.getPassword(entry).takeIf { it.isNotEmpty() }
    if (username == null || password == null) {
      cancel()
      return
    }

    val result = Intent()
    PendingIntentHandler.setGetCredentialResponse(
      result,
      GetCredentialResponse(PasswordCredential(username, password))
    )
    setResult(RESULT_OK, result)
    finish()
  }
```

- [ ] **Step 6: Add unlock launchers to `CredentialSaveActivity`**

In `CredentialSaveActivity`, add this launcher:

```kotlin
  private val unlockLauncher = registerForActivityResult(
    object : ActivityResultContract<Unit, Boolean>() {
      override fun createIntent(context: android.content.Context, input: Unit): Intent {
        return if (BaseApp.KDB != null && BaseApp.APP.isCanOpenQuickLock()) {
          QuickUnlockActivity.createQuickUnlockResultIntent(context)
        } else {
          LauncherActivity.createUnlockResultIntent(context)
        }
      }

      override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
        return resultCode == RESULT_OK
      }
    }
  ) { unlocked ->
    if (unlocked) {
      continueCreate()
    } else {
      cancel()
    }
  }
```

Add import:

```kotlin
import com.lyy.keepassa.view.main.QuickUnlockActivity
```

Refactor `onCreate()`:

```kotlin
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (BaseApp.KDB == null || BaseApp.isLocked) {
      unlockLauncher.launch(Unit)
      return
    }
    continueCreate()
  }
```

Move the existing request parsing and `createEntryLauncher.launch(...)` code into:

```kotlin
  private fun continueCreate() {
    val providerRequest = PendingIntentHandler.retrieveProviderCreateCredentialRequest(intent)
    val passwordRequest = providerRequest?.callingRequest as? CreatePasswordRequest
    if (providerRequest == null || passwordRequest == null) {
      cancel()
      return
    }
    if (BaseApp.KDB == null || BaseApp.isLocked) {
      cancel()
      return
    }
    val packageName = providerRequest.callingAppInfo.packageName
    val browserDomain = AutofillBrowserAuthContextStore.find(packageName)?.domain
    val draft = CredentialSaveRequestMapper.from(
      packageName = packageName,
      origin = browserDomain?.let { "https://$it" },
      username = passwordRequest.id,
      password = passwordRequest.password
    )
    if (draft == null) {
      cancel()
      return
    }
    createEntryLauncher.launch(
      AutoFillParam(
        apkPkgName = draft.packageName,
        domain = draft.domain,
        isSave = true,
        saveUserName = draft.username,
        savePass = draft.password
      )
    )
  }
```

- [ ] **Step 7: Run build**

Run:

```powershell
.\gradlew.bat :app:assembleDevDebug
```

Expected: SUCCESS.

- [ ] **Step 8: Manual locked-database smoke test**

On Android 14+:

1. Lock or close the KeePassA database.
2. Trigger `CredentialManager.getCredential(GetPasswordOption())` from a test app.
3. Tap KeePassA unlock action.
4. Verify KeePassA opens through the system action.
5. Unlock database.
6. Verify the original Credential Manager operation completes instead of requiring a second get request.
7. Repeat with `CreatePasswordRequest` and verify the original save operation continues to `CreateEntryActivity`.

- [ ] **Step 9: Commit**

```powershell
git add app/src/main/java/com/lyy/keepassa/service/credential/CredentialProviderPendingIntents.kt `
  app/src/main/java/com/lyy/keepassa/service/credential/KeepassACredentialProviderService.kt `
  app/src/main/java/com/lyy/keepassa/view/credential/CredentialGetActivity.kt `
  app/src/main/java/com/lyy/keepassa/view/credential/CredentialSaveActivity.kt `
  app/src/main/java/com/lyy/keepassa/view/launcher/LauncherActivity.kt `
  app/src/main/java/com/lyy/keepassa/view/launcher/OpenDbFragment.kt `
  app/src/main/java/com/lyy/keepassa/view/main/QuickUnlockActivity.kt
git commit -m "feat: handle locked database credential actions"
```

---

### Task 9: Final Verification

**Files:**
- No planned source changes.

- [ ] **Step 1: Run all unit tests**

Run:

```powershell
.\gradlew.bat :app:testDevDebugUnitTest
```

Expected: SUCCESS.

- [ ] **Step 2: Run debug build**

Run:

```powershell
.\gradlew.bat :app:assembleDevDebug
```

Expected: SUCCESS.

- [ ] **Step 3: Inspect manifest output**

Run:

```powershell
Select-String -Path "app\\build\\intermediates\\merged_manifests\\devDebug\\processDevDebugManifest\\AndroidManifest.xml" -Pattern "CredentialProviderService|BIND_CREDENTIAL_PROVIDER_SERVICE|android.credentials.provider"
```

Expected output contains:

```text
android.service.credentials.CredentialProviderService
android.permission.BIND_CREDENTIAL_PROVIDER_SERVICE
android.credentials.provider
```

- [ ] **Step 4: Manual Android 14+ test matrix**

Use a small test app or an existing Credential Manager sample app:

```kotlin
val credentialManager = CredentialManager.create(this)

val getResult = credentialManager.getCredential(
  context = this,
  request = GetCredentialRequest(
    listOf(GetPasswordOption())
  )
)

credentialManager.createCredential(
  context = this,
  request = CreatePasswordRequest(
    id = "alice",
    password = "secret-password"
  )
)
```

Expected:

- KeePassA appears as a credential provider in Android settings.
- Unlocked database returns password entries.
- Selecting a password returns a `PasswordCredential`.
- Save password shows "Save to KeePassA".
- Save opens KeePassA entry editor with username/password prefilled.
- Successful save returns success to the calling app.
- Canceling entry creation cancels the Credential Manager operation.
- Locked database opens KeePassA through a user-tapped system action and does not report fake success.

- [ ] **Step 5: Final status check**

Run:

```powershell
git status --short
git log --oneline -8
```

Expected:

- `git status --short` is empty.
- Recent commits include all Credential Manager implementation commits.
