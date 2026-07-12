# Merge Attachment Isolation And UI Design

## Problem

`ProtectedBinary` stores database-loaded attachments in encrypted temporary files. `Database.LoadData()` passes `Context.filesDir` to `ImporterV4`, while `ImporterV4` names attachment files with binary-pool indexes such as `0` and `1`.

Opening the downloaded cloud database with the application context therefore overwrites temporary files that belong to the already-open local database. The local `ProtectedBinary` keeps its original AES key and IV, so reading the overwritten file fails with `BadPaddingException`. Treating that read failure as binary inequality makes every entry with an attachment appear conflicted.

## Approved Design

- Open every downloaded cloud database with a wrapped context whose `filesDir` is a unique cache directory dedicated to that open operation.
- Keep the cloud attachment directory alive because merge results can retain cloud `ProtectedBinary` instances until the merged database is saved and for the remainder of the in-memory database lifetime.
- Preserve strict binary equality: map key, protection state, length, and actual bytes must all match.
- Do not treat equal sizes as sufficient equality and do not infer attachment names from UI state.
- Keep binary read failures conservative: they remain conflicts, but normal local attachments must no longer become unreadable when the cloud database opens.

## UI

- Display the current entry or group name as the centered `MaterialToolbar` title.
- Remove the duplicate title row below the toolbar.
- Keep merge progress at the left side of the bottom action bar.
- Reduce the bottom action bar from `48dp` to `40dp` and reduce action minimum widths and horizontal padding proportionally.
- Keep the local-only section title in the toolbar.

## Verification

- Reproduce the overwritten-file failure with two file-backed `ProtectedBinary` instances that share a path and use different encryption parameters.
- Verify distinct temporary directories allow equal file-backed binaries to compare as `ThreeWay.Same`.
- Verify cloud open contexts always use directories different from the application `filesDir` and from each other.
- Run merge and interceptor unit tests, compile Kotlin and Java, assemble the dev APK, install it, and cold-launch it on the connected device.
