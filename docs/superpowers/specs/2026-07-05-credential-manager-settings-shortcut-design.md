# Credential Manager Settings Shortcut Design

## Context

KeePassA now registers an Android Credential Manager provider for password get and
save flows. Users still need to enable the provider in system settings. The current
KeePassA settings screen already has an "Autofill settings" section with an
Autofill service switch and browser autofill settings entries, so the Credential
Manager shortcut belongs in the same section.

Android API 35 exposes `Settings.ACTION_CREDENTIAL_PROVIDER`
(`android.settings.CREDENTIAL_PROVIDER`), but OEM ROMs are not required to expose
an activity for that action. The connected MIUI device did not resolve
`android.settings.CREDENTIAL_PROVIDER` through `adb shell am start`, so the app
must treat direct navigation as best-effort.

## Goal

Add a KeePassA settings entry that helps users reach system Credential Manager,
password, passkey, or credential provider settings with the fewest possible taps.

## Non-Goals

- Do not change Credential Manager provider query, select, save, or unlock flows.
- Do not add passkey support.
- Do not depend on MIUI private settings components in the first version.
- Do not replace the existing Autofill service switch.

## UI

Add one normal `Preference` under the existing "Autofill settings" category:

- English title: `Credential Manager`
- English summary: `Open system password, passkey, and credential provider settings`
- Chinese title: `凭据管理器`
- Chinese summary: `打开系统密码、通行密钥和凭据提供程序设置`

The entry should use an existing settings/autofill/password-style icon to stay
consistent with the current settings screen.

## Behavior

When the user taps the entry, KeePassA tries to open settings in this order:

1. `Settings.ACTION_CREDENTIAL_PROVIDER` on API 35 and above.
2. Literal action `android.settings.CREDENTIAL_PROVIDER` as a compatibility
   attempt for devices where the SDK constant is unavailable at compile call sites
   or guarded by API level.
3. `Settings.ACTION_SECURITY_SETTINGS`.
4. `Settings.ACTION_SETTINGS`.

Each candidate must be checked with `PackageManager.resolveActivity` before
launching. If the launched page is a fallback rather than the credential provider
page, KeePassA shows a long Toast telling the user that the current system does
not support direct Credential Manager settings and that they can search system
settings for "凭据" or "密码".

If no settings page can be opened, KeePassA shows a long failure Toast instead of
crashing.

## Architecture

Create a small helper near the settings feature, rather than embedding all intent
selection logic inside `AppSettingFragment`.

The helper should expose:

- A candidate action policy that is easy to unit test.
- A function that opens the first resolvable settings intent and reports whether
  it opened the direct page, a fallback page, or nothing.

`AppSettingFragment` only wires the new preference click to this helper and shows
the corresponding Toast. This keeps the settings fragment from accumulating OEM
navigation details.

## Testing

Add focused unit coverage for the pure policy:

- API 35+ includes `android.settings.CREDENTIAL_PROVIDER` before fallback actions.
- Lower API levels still include the literal credential provider action and
  fallback actions.
- Security settings and general settings remain the final fallbacks.

Manual verification:

- Build `:app:assembleDevDebug`.
- Install the dev debug APK on the connected MIUI device.
- Open KeePassA settings and tap `凭据管理器`.
- Confirm it either reaches a credential settings page or falls back to a safe
  settings page with a clear Toast.
