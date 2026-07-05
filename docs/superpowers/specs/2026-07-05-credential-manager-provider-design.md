# Credential Manager Password Provider Design

Date: 2026-07-05

## Context

KeePassA already has a traditional Android `AutofillService` that supports Android 8+ autofill, browser compatibility packages, `AssistStructure` parsing, authenticated `FillResponse` flows, and save prompts. Android 14+ also supports Credential Manager providers. Apps that call `CredentialManager.getCredential()` or `CredentialManager.createCredential()` will not discover KeePassA unless KeePassA registers a Credential Manager provider service.

This design adds Credential Manager password provider support without replacing the existing autofill service.

## Goals

- Add an Android 14+ Credential Manager provider service for password credentials.
- Support password retrieval through `CredentialManager.getCredential(GetPasswordOption())`.
- Support password saving through `CredentialManager.createCredential(CreatePasswordRequest)`.
- Reuse existing KeePassA database unlock, quick unlock, entry search, and entry creation flows where practical.
- Match credentials by domain first, then fall back to Android package name.
- Open the existing entry edit/confirmation UI before saving new credentials.
- Avoid background activity starts. All UI is launched through system-triggered `PendingIntent`s.

## Non-Goals

- Do not implement passkey creation or passkey login in this first version.
- Do not declare public key credential capability.
- Do not remove or replace the existing `AutofillService`.
- Do not silently save credentials without user confirmation.
- Do not auto-merge duplicate entries.
- Do not add a user-facing permission request for background activity starts.

## Architecture

The existing `AutoFillService` remains responsible for traditional autofill:

- Android 8+ autofill.
- Browser compatibility and `AssistStructure` parsing.
- Existing authentication `FillResponse` behavior.
- Existing autofill save behavior.

A new Credential Manager provider service is added under a separate package:

```text
app/src/main/java/com/lyy/keepassa/service/credential/
  KeepassACredentialProviderService.kt
  CredentialLookupTarget.kt
  CredentialLookupTargetMapper.kt
  CredentialPasswordRepository.kt
  CredentialProviderPendingIntents.kt
  CredentialSaveRequestMapper.kt
```

Credential Manager result bridge activities are added only where the provider protocol requires them:

```text
app/src/main/java/com/lyy/keepassa/view/credential/
  CredentialGetActivity.kt
  CredentialSaveActivity.kt
```

The bridge activities keep Credential Manager-specific `PendingIntentHandler` code out of the existing launcher, quick unlock, and entry creation screens as much as possible.

## Manifest Registration

Add the Jetpack credentials dependency to the app module:

```gradle
implementation(libs.jetpack.credentials)
```

Register a provider service for Android 14+:

```xml
<service
    android:name=".service.credential.KeepassACredentialProviderService"
    android:enabled="true"
    android:exported="true"
    android:label="@string/app_name"
    android:icon="@mipmap/ic_launcher"
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

Add `app/src/main/res/xml/credential_provider.xml`:

```xml
<credential-provider xmlns:android="http://schemas.android.com/apk/res/android">
    <capabilities>
        <capability name="android.credentials.TYPE_PASSWORD_CREDENTIAL" />
    </capabilities>
</credential-provider>
```

The provider must not declare `androidx.credentials.TYPE_PUBLIC_KEY_CREDENTIAL` in this version.

## Lookup Target

Credential Manager requests are mapped into a small query model:

```kotlin
data class CredentialLookupTarget(
    val packageName: String,
    val domain: String?
)
```

The lookup policy is:

1. Use domain when a reliable domain can be extracted.
2. If domain is missing or returns no entries, use `callingAppInfo.packageName`.
3. If package name is missing or unsupported, return no credential entries.

Repository query behavior:

```kotlin
val entries =
    domain?.let { KDBAutoFillRepository.getAutoFillDataByDomain(it) }
        ?.takeIf { it.isNotEmpty() }
        ?: KDBAutoFillRepository.getAutoFillDataByPackageName(packageName)
```

This preserves the existing KeePassA convention for app credentials while supporting web/domain credentials when available.

## Password Retrieval Flow

`KeepassACredentialProviderService.onBeginGetCredentialRequest()` handles only `BeginGetPasswordOption`.

Flow:

```text
onBeginGetCredentialRequest
        |
        +-- no password option
        |       |
        |       +-- return empty/unsupported response
        |
        +-- database missing or locked
        |       |
        |       +-- return AuthenticationAction("Unlock KeePassA", unlock PendingIntent)
        |
        +-- database unlocked
                |
                +-- map request to CredentialLookupTarget
                +-- query domain first, package second
                +-- return one PasswordCredentialEntry per KeePass entry
```

Locked database behavior:

- The service does not call `startActivity()`.
- It returns an `AuthenticationAction`.
- The action launches KeePassA through a `PendingIntent` when the user taps it.
- After successful unlock, KeePassA should complete the Credential Manager request instead of requiring the user to repeat the entire flow.

Unlocked database behavior:

- Each matching KeePass entry becomes a `PasswordCredentialEntry`.
- The entry `PendingIntent` launches `CredentialGetActivity`.
- The pending intent carries only identifiers such as entry UUID, package name, and domain. It must not carry the password.

`CredentialGetActivity` responsibilities:

1. Retrieve the provider get request with `PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)`.
2. Read the selected entry UUID from intent extras.
3. If the database is locked, trigger unlock and resume.
4. Read username and password from the selected KeePass entry.
5. Return the result:

```kotlin
PendingIntentHandler.setGetCredentialResponse(
    resultIntent,
    GetCredentialResponse(
        PasswordCredential(username, password)
    )
)
setResult(Activity.RESULT_OK, resultIntent)
finish()
```

If multiple entries match, the system credential selector shows multiple `PasswordCredentialEntry` items. Existing KeePassA search UI can be used as a fallback when no exact entry is selected or when unlock changes the available result set.

## Password Save Flow

`KeepassACredentialProviderService.onBeginCreateCredentialRequest()` handles only `BeginCreatePasswordCredentialRequest`.

Flow:

```text
onBeginCreateCredentialRequest
        |
        +-- request is not password
        |       |
        |       +-- return unsupported response
        |
        +-- request is password
                |
                +-- return CreateEntry("Save to KeePassA", save PendingIntent)
                        |
                        v
                user taps system save entry
                        |
                        v
                CredentialSaveActivity
                        |
                        +-- database locked: unlock first
                        +-- database unlocked: open CreateEntryActivity
                        +-- user confirms save
                        +-- return CreatePasswordResponse
```

The provider returns:

```kotlin
BeginCreateCredentialResponse(
    createEntries = listOf(
        CreateEntry(
            accountName = getString(R.string.app_name),
            pendingIntent = createSavePasswordPendingIntent()
        )
    )
)
```

`CredentialSaveActivity` retrieves the create request:

```kotlin
val providerRequest =
    PendingIntentHandler.retrieveProviderCreateCredentialRequest(intent)

val request = providerRequest?.callingRequest as? CreatePasswordRequest
```

It maps the request to the existing save model:

```kotlin
AutoFillParam(
    apkPkgName = packageName,
    domain = domain,
    saveUserName = request.id,
    savePass = request.password,
    isSave = true
)
```

Saving uses the existing entry confirmation flow:

- If the database is locked, launch existing full unlock or quick unlock.
- Once unlocked, open `CreateEntryActivity` with the prepared `AutoFillParam`.
- The user chooses group/content and confirms save.
- `CreateEntryActivity` returns `Activity.RESULT_OK` after a successful save.
- `CredentialSaveActivity` then returns:

```kotlin
PendingIntentHandler.setCreateCredentialResponse(
    resultIntent,
    CreatePasswordResponse()
)
setResult(Activity.RESULT_OK, resultIntent)
finish()
```

If the user cancels unlock or cancels entry creation, `CredentialSaveActivity` returns canceled and does not report success to Credential Manager.

## Entry Save Format

Credential Manager saves should be consistent with existing Autofill saves.

Minimum fields:

- Username: `CreatePasswordRequest.id`
- Password: `CreatePasswordRequest.password`
- App URL marker: `androidapp://<packageName>`
- Domain marker when available.

Existing `AutoFillSaveEntryBinder` behavior should be reused or extended so Autofill and Credential Manager produce compatible KeePass entries.

First version does not silently merge duplicates. Duplicate handling remains a user decision in the edit/confirmation UI.

## Security Rules

- Never log passwords.
- Never put passwords into manually created pending intent extras.
- Use `PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT` for Credential Manager provider pending intents, because the system needs to attach provider request information.
- Use system-triggered pending intents for all UI.
- Do not directly start activities from the provider service.
- Do not return password entries when the database is locked. Return an authentication action instead.
- Do not claim passkey support until a real passkey implementation exists.

## Error Handling

- Unsupported get options return no credential entries or a provider error appropriate to the Jetpack API.
- Unsupported create requests return a create credential error.
- Missing package name returns no entries.
- Missing or empty credential data cancels save instead of creating an incomplete entry.
- User-canceled unlock cancels the Credential Manager operation.
- User-canceled edit confirmation cancels the Credential Manager operation.
- Save failure does not return `CreatePasswordResponse`.

## Tests

Add JVM tests for pure mapping and policy behavior:

```text
app/src/test/java/com/lyy/keepassa/service/credential/
  CredentialLookupTargetMapperTest.kt
  CredentialPasswordRepositoryTest.kt
  CredentialProviderActionFactoryTest.kt
  CredentialSaveRequestMapperTest.kt
```

Coverage:

- Begin-get ignores non-password options.
- Begin-get maps calling app info to package name.
- Domain lookup has priority over package lookup.
- Package lookup is used when domain is absent.
- Package lookup is used when domain lookup has no result.
- Locked database returns an unlock action and no password entries.
- Unlocked database returns password entries.
- Begin-create ignores non-password requests.
- Create password request maps to `AutoFillParam`.
- Empty username/password handling follows existing save rules.
- Pending intent extras do not include password values.

Run verification:

```powershell
.\gradlew.bat :app:testDevDebugUnitTest
.\gradlew.bat :app:assembleDevDebug
```

Manual Android 14+ verification:

- Enable KeePassA as a credential provider in system settings.
- Test `CredentialManager.getCredential(GetPasswordOption())` with database locked.
- Test `CredentialManager.getCredential(GetPasswordOption())` with database unlocked.
- Verify domain-first and package fallback matching.
- Verify multiple matching entries are selectable.
- Test `CredentialManager.createCredential(CreatePasswordRequest)`.
- Verify the existing edit/confirmation UI opens.
- Verify successful save returns `CreatePasswordResponse`.
- Verify canceling unlock or save does not report success.

## Implementation Notes

Expected modified files:

```text
app/build.gradle
app/src/main/AndroidManifest.xml
app/src/main/java/com/lyy/keepassa/view/create/entry/CreateEntryActivity.kt
app/src/main/java/com/lyy/keepassa/view/create/entry/AutoFillSaveEntryBinder.kt
app/src/main/java/com/lyy/keepassa/view/launcher/LauncherActivity.kt
app/src/main/java/com/lyy/keepassa/view/main/QuickUnlockActivity.kt
app/src/main/res/values/strings.xml
```

Expected new files:

```text
app/src/main/java/com/lyy/keepassa/service/credential/
app/src/main/java/com/lyy/keepassa/view/credential/
app/src/main/res/xml/credential_provider.xml
app/src/test/java/com/lyy/keepassa/service/credential/
```

The launcher and quick unlock changes should stay narrow. They should expose only the minimum result bridge needed for Credential Manager flows and should not rewrite existing autofill behavior.
