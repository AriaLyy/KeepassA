# Merge Attachment Isolation And UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prevent unchanged attachments from becoming merge conflicts and compact the conflict screen header and bottom action bar.

**Architecture:** A small context factory gives each cloud database open operation its own attachment storage directory while leaving local database loading unchanged. Binary diff semantics remain model-based and strict. The existing merge activity renders the item name directly in a centered toolbar and uses a shorter action bar.

**Tech Stack:** Kotlin, Android `ContextWrapper`, KeePass `ProtectedBinary`, JUnit 4, Android XML layouts, Gradle.

---

### Task 1: Reproduce Attachment File Overwrite

**Files:**
- Modify: `app/src/test/java/com/lyy/keepassa/util/cloud/merge/EntryDifferImplTest.kt`

- [ ] Add a test helper that creates and closes a file-backed `ProtectedBinary`.
- [ ] Add a test proving equal attachment payloads compare as conflict after a second `ProtectedBinary` overwrites the same encrypted file path.
- [ ] Run `./gradlew.bat :app:testDevDebugUnitTest --offline --tests "com.lyy.keepassa.util.cloud.merge.EntryDifferImplTest"` and confirm the new regression scenario is red for the desired behavior.

### Task 2: Isolate Cloud Attachment Storage

**Files:**
- Create: `app/src/main/java/com/lyy/keepassa/util/cloud/interceptor/CloudDatabaseOpenContext.kt`
- Modify: `app/src/main/java/com/lyy/keepassa/util/cloud/interceptor/DbSyncCompareInterceptor.kt`
- Create: `app/src/test/java/com/lyy/keepassa/util/cloud/interceptor/CloudDatabaseOpenContextTest.kt`

- [ ] Add tests that request two cloud-open directories and assert they differ from the local files directory and each other.
- [ ] Implement a `ContextWrapper` backed by a unique directory under `cacheDir/cloud_merge_attachments`.
- [ ] Pass the wrapped context to `KDBHandlerHelper.openDb()` when opening a downloaded cloud database.
- [ ] Run the context and entry differ tests and confirm they pass.

### Task 3: Compact Merge Activity Layout

**Files:**
- Modify: `app/src/main/res/layout/activity_merge_conflict.xml`
- Modify: `app/src/main/java/com/lyy/keepassa/util/cloud/merge/MergeConflictActivity.kt`

- [ ] Enable centered toolbar titles and remove the duplicate `mergeItemTitle` view.
- [ ] Bind the current item name directly to the toolbar title; keep the local-only title for that section.
- [ ] Reduce the bottom action bar height to `40dp`, action minimum widths to `64dp`, and horizontal action padding to `8dp`.
- [ ] Compile view binding and Kotlin sources to catch stale binding references.

### Task 4: Full Verification

**Files:**
- Verify all files changed above.

- [ ] Run merge, interceptor, foreground save, and loading dialog unit tests offline.
- [ ] Run Kotlin and Java compilation and assemble `devDebug` offline.
- [ ] Run `git diff --check`.
- [ ] Install `devDebug` on the connected device, cold-launch the app, and inspect logcat for fatal exceptions and `BadPaddingException`.
