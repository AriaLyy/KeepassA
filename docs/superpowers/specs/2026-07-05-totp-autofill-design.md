# TOTP Autofill Support Design

## Context

KeePassA's current Android Autofill path classifies fields as username or password, then builds
datasets that map text fields to either `KdbUtil.getUserName(entry)` or `KdbUtil.getPassword(entry)`.
The app already has TOTP generation support through `OtpUtil.getOtpPass(PwEntryV4)`, including
KeePassXC, KeeTrayTOTP, KeeOtp, KeeOtp2, and KeePass TOTP formats. That TOTP capability is not
currently connected to Autofill field classification or dataset generation.

The goal is to add TOTP Autofill without changing browser domain matching behavior and without
falling back to unsafe package matching for browser pages.

## Scope

Implement option B:

1. If a login form contains username, password, and TOTP fields, selecting a matched entry fills all
   supported fields, including the current TOTP code.
2. If the current Autofill request contains only a clearly recognized TOTP field, KeePassA still
   shows matched entries and fills only the TOTP code after selection.

Out of scope:

- Saving TOTP from Autofill save flows. A webpage usually exposes only the current 6-digit code,
  not the TOTP secret, so saving it would create incorrect entries.
- Broad `code` detection. A generic `code` token can mean invitation code, coupon code, postal
  code, recovery code, or other non-TOTP values.
- Any browser package-name fallback when domain is missing.

## Field Classification

Add a dedicated Autofill role for TOTP instead of reusing username or password.

Recognized TOTP indicators:

- W3C: `one-time-code`
- Common tokens: `otp`, `totp`, `2fa`, `mfa`, `one-time-code`, `verification-code`, `auth-code`,
  `authenticator`
- Chinese tokens: `验证码`, `动态码`, `一次性密码`, `二次验证`, `两步验证`

Token sources:

- Android `autofillHints`
- `ViewNode.idEntry`
- `ViewNode.hint`
- HTML attributes from `ViewNode.htmlInfo`

Classification order:

1. Exclude search and URL fields.
2. Password.
3. TOTP.
4. Username.

This keeps existing credential field behavior stable and prevents a password field from being
treated as a TOTP field.

## Structure Parsing

`StructureParser` should collect TOTP fields similarly to username and password fields:

- Add TOTP hint constants or token matching helpers.
- Add an `addTotpField()` path that stores metadata with a dedicated TOTP hint.
- Track TOTP fields so the non-manual "clear fields when no password" guard can allow a
  TOTP-only request.

The current guard prevents noisy Autofill prompts by clearing fields when no password is detected
in non-manual, non-W3C contexts. It should become:

```text
clear fields only when password fields are empty, TOTP fields are empty, and the request is not W3C
```

This preserves the anti-noise behavior while allowing a focused TOTP field to trigger Autofill.

## W3C Handling

Extend `W3cHints` with TOTP detection:

- `autocomplete="one-time-code"` should be classified as TOTP.
- TOTP classification should run before username classification for W3C input nodes.

This follows the web standard and handles modern browser login pages without relying on localized
labels.

## Dataset Generation

`AutoFillHelper.applyDataInfoToFields()` should map field role to value:

- Password field: `KdbUtil.getPassword(entry)`
- Username field: `KdbUtil.getUserName(entry)`
- TOTP field: `OtpUtil.getOtpPass(entry).second`, only for `PwEntryV4`

If a dataset includes username/password fields and the entry does not have TOTP, KeePassA should
still fill username/password. If the request contains only TOTP fields and the entry has no TOTP
code, KeePassA should not show a dataset for that entry because selecting it would have no effect.

## Entry Matching

Entry matching remains unchanged:

- Browser pages are matched by domain.
- Native apps are matched by package URL fields such as `androidapp://<package>`.
- Missing browser domains do not fall back to browser package-name matching.
- If no matched entry exists, the existing search flow is used.

This keeps the new TOTP feature from reopening the browser compatibility bugs fixed earlier.

## Tests

Add focused tests before implementation:

1. W3C `one-time-code` is recognized as TOTP.
2. Common and Chinese TOTP tokens, including `二次验证` and `两步验证`, are recognized.
3. A generic `code` token alone is not recognized as TOTP.
4. TOTP metadata is distinct from username and password metadata.
5. TOTP-only requests are not cleared by `StructureParser`'s non-manual no-password guard.
6. Dataset generation fills TOTP fields from `OtpUtil.getOtpPass()` rather than username/password.
7. TOTP-only datasets are omitted for entries without TOTP.

## Risk Controls

- Keep TOTP recognition conservative. Do not treat `code` alone as TOTP.
- Do not modify browser matching or package fallback rules.
- Do not modify save flows for TOTP.
- Keep the implementation scoped to field classification, metadata role storage, and dataset value
  mapping.
