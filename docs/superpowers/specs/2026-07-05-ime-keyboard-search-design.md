# KeePassA IME Keyboard and Search Design

## Background

KeePassA already has an Android `InputMethodService` implementation in
`InputIMEService`. The current IME can fill username, password, TOTP, and other
entry fields, and it can display matched entries in a horizontal candidate list.
It also has a search click handler wired to `CommonSearchActivity`, but the
layout search icon is hidden, and the returned single search result is selected
without being shown as a visible result list.

Browser IME sessions cannot reliably read the current page URL. Relying on the
global `W3cHints.curDomainUrl` is unsafe for IME initial candidates because that
value can be stale across pages, tabs, or packages. The IME therefore needs a
manual search path and a basic soft keyboard so users can type search terms and
ordinary text without switching away from KeePassA.

## Goals

- Add a KeePassA-owned lightweight soft keyboard to the existing IME.
- Add an in-IME search mode that lets users search KeePass entries without
  writing the search query into the browser or app input field.
- Show search results inside the IME candidate area and allow filling the
  selected entry with existing username/password/TOTP/other-field buttons.
- Avoid changing existing AutofillService behavior except for publishing a
  bounded browser-domain context that the IME can safely consume.
- Keep ordinary IME input private: no ordinary typed text is stored, searched,
  logged, or persisted.

## Non-Goals

- Do not build a full general-purpose keyboard with Chinese/Pinyin input,
  dictionaries, autocorrect, suggestions, gesture typing, themes, or advanced
  language switching.
- Do not integrate OpenBoard, HeliBoard, FlorisBoard, AOSP LatinIME, or another
  open-source keyboard engine in the first version.
- Do not add clipboard operations, text selection, all-select, copy, cut, paste,
  or cursor movement in the first version.
- Do not search password fields, protected strings, TOTP secrets, attachments,
  or history records from the IME.
- Do not offer entry creation from the IME no-result state in the first version.

## Existing Code Context

- `InputIMEService` owns the current IME lifecycle, fill actions, candidate
  list, search callback, lock behavior, and input connection.
- `layout_kpa_ime.xml` defines the existing IME panel. Its `ivSearch` is
  currently hidden.
- `CandidatesAdapter` and `CandidateSelectionTracker` already support a
  horizontal selectable candidate list.
- `CommonSearchActivity` and `SearchModule` provide full app search behavior,
  but their search scope is too broad for IME privacy requirements.
- `StructureParser` writes browser domains into `W3cHints.curDomainUrl`; this is
  not sufficient as an IME reliability boundary because it has no package,
  timestamp, or source binding.
- App settings already place the "secure keyboard" item in the safety settings
  group in `app_setting.xml`.

## Chosen Approach

Implement the feature inside the existing KeePassA IME using custom XML views
and small Kotlin components. The service remains the lifecycle and UI
orchestrator; keyboard state, search ranking, debounce, browser-domain context,
and preferences are isolated into testable units.

This avoids the maintenance and security surface of a complete open-source
keyboard fork while still solving the core browser issue: users can manually
search KeePass entries when the IME cannot obtain the page URL.

## Component Design

### InputIMEService

Responsibilities:

- Create and bind the IME view.
- Route key actions to ordinary input or search mode.
- Call `InputConnection.commitText`, `deleteSurroundingText`, and
  `performEditorAction` only when the current mode allows writing to the target
  input field.
- Keep existing fill actions for username, password, TOTP, and other fields.
- Keep existing lock, close, change-input-method, backspace, and enter
  behaviors where they are still valid.
- Bind candidate/result clicks to `CandidateSelectionTracker`.
- Exit search mode when appropriate while preserving the selected entry when
  the user has made a manual choice.

The service should not contain the keyboard layout state machine, ranking
rules, TTL logic, or debounce implementation.

### ImeKeyboardState

Responsibilities:

- Track ordinary mode versus search mode.
- Track alphabet page versus number/symbol page.
- Track Shift state:
  - default lowercase,
  - single tap applies uppercase to the next character only,
  - double tap or long press locks uppercase,
  - symbol page is not affected by Shift.
- Track the in-memory search query.
- Clear the search query when search mode exits, the database locks, or the IME
  is destroyed.

Ordinary typed text is never stored in this state.

### ImeKeyboardLayout and ImeKeyAction

Responsibilities:

- Define the QWERTY and number/symbol key rows.
- Map every key to an action such as commit character, space, backspace, enter,
  shift, switch page, enter search mode, clear search, or exit search mode.
- Keep key definitions independent from Android views so they can be tested.

Alphabet layout:

- Row 1: `qwertyuiop`
- Row 2: `asdfghjkl`
- Row 3: `shift zxcvbnm backspace`
- Function row: `123`, comma, space, period, enter

Number/symbol layout:

- Characters: `0-9 @ . _ - + / : ? & = # % ! * ( )`
- Function keys: `ABC`, space, backspace, enter

No ordinary key long-press symbol popups are included in the first version.
Existing long-press backspace behavior remains.

### ImeEntrySearchEngine

Responsibilities:

- Search KeePass entries using an IME-specific privacy scope.
- Return only entries, not groups.
- Exclude password fields, protected strings, TOTP secrets, attachments, and
  history records.
- Include title, username, URL, notes, and explicitly non-protected custom
  fields.
- Sort results with browser/login relevance:
  1. Title exact match.
  2. Title prefix match.
  3. Title contains match.
  4. URL or domain match.
  5. Username or email match.
  6. Notes or other allowed custom field match.
- Preserve the original database search order for ties.
- Return at most 10 entries.

The search engine must not log the query or matched sensitive values.

### ImeSearchController

Responsibilities:

- Debounce query changes by 250 ms.
- Trim leading and trailing whitespace for matching while preserving the user's
  displayed search query.
- Clear results for an empty query.
- Select the first result by default.
- Publish a lightweight empty state when there are no matches.

### ImeBrowserDomainContext

Responsibilities:

- Store browser-domain context as `browserPackage`, `domain`, `updatedAt`, and
  `source`.
- Be updated by autofill parsing when a browser domain is reliably parsed.
- Be consumed by IME only when the current `EditorInfo.packageName` matches and
  the context is within a 60-second TTL.
- Return no domain when the package differs or the TTL has expired.
- Clear or become unusable when appropriate to prevent stale domain candidates.

IME must not directly trust `W3cHints.curDomainUrl` as a freshness signal.

### ImeManualSelectionPolicy

Responsibilities:

- Treat user-selected search results as explicit intent.
- Prevent automatic candidates from overriding a manual selection during the
  same target package session.
- Clear manual selection when the user starts a new search, the database locks,
  the IME is destroyed, or the target package changes.

### ImeKeyboardPreferences

Responsibilities:

- Read the "keyboard haptic feedback" setting from default shared preferences.
- Default the setting to enabled.
- Apply haptic feedback only to the new lightweight keyboard keys and search
  control keys.
- Do not add haptic feedback to existing username/password/TOTP/other-field
  fill buttons.

Use `View.performHapticFeedback(...)` so system haptic settings are respected.
No sound feedback is included.

## UI Design

The IME panel is a fixed vertical layout:

1. Top search area.
2. Horizontal candidate/result area.
3. Existing fill action area.
4. Lightweight keyboard area.

### Top Search Area

Ordinary mode:

- Show a discoverable search entry with a search icon and a short hint.
- Tapping it enters search mode.

Search mode:

- Show the current search query.
- Provide clear and exit controls.
- Keyboard input edits the internal query, not the target app field.

### Candidate and Result Area

Automatic candidate mode:

- Preserve existing behavior where possible.
- Multi-entry automatic matches show the horizontal candidate list.
- Single automatic matches may remain hidden while selected, matching current
  behavior.

Search mode:

- Always show results, including a single result.
- Show a lightweight empty state for no results.
- Default-select the first result.
- User taps another result to change selection.

Result item:

- Fixed width to prevent layout expansion.
- Entry icon.
- Title.
- One auxiliary line:
  - username first,
  - URL or domain when username is empty.
- Password is never displayed.

### Fill Action Area

Preserve existing actions:

- Lock.
- Username.
- Password.
- Close keyboard.
- Change input method.
- TOTP.
- Other fields.
- Backspace.
- Enter.

Search mode behavior:

- If a search result is selected, username/password/TOTP/other-field buttons
  fill that selected entry and then exit search mode.
- If no result is selected, prompt the user to select an entry first.

The input-method switch button remains in the existing fill action area. It is
not duplicated in the keyboard function row.

### Lightweight Keyboard Area

The keyboard is visible in all app contexts, not only browsers.

Ordinary mode:

- Character, number, symbol, and space keys call `commitText`.
- Backspace deletes surrounding text.
- Enter performs the current editor action.

Search mode:

- Character, number, symbol, and space keys update the internal query.
- Backspace deletes from the internal query.
- Enter confirms the current search and does not submit to the target app.

First version constraints:

- No cursor movement.
- No text selection.
- No clipboard commands.
- No ordinary key long-press symbol popup.
- No sound feedback.
- No dedicated landscape layout.

Small-screen and landscape behavior:

- Prioritize portrait.
- Use stable row heights and ellipsized text.
- Keep landscape usable without a separate optimized layout.

## State Flow

### Opening the IME

- Default to ordinary input mode.
- Set the current target package from `EditorInfo.packageName`.
- If the target package changed, clear manual selection.
- If the database is open, load automatic candidates:
  - non-browser apps: package-based lookup,
  - browsers: domain-context lookup only when package and TTL match.
- If browser domain context is absent, stale, or for another package, do not
  show browser initial candidates.

### Entering Search Mode

- User taps the search entry.
- Search query starts empty.
- Ordinary key presses no longer write to the target field.
- Empty query shows no search results.

### Typing in Search Mode

- Update the in-memory query.
- Debounce search by 250 ms.
- Show up to 10 ranked results.
- Select the first result by default.
- Show empty state when there are no matches.

### Selecting a Result

- User tapping a result selects it.
- Search mode exits.
- The selected entry remains available for fill actions.
- The selected entry has priority over later automatic candidates in the same
  package session.

### Switching Target Input Fields

- Exit search mode.
- Clear the search query.
- Preserve a manual selected entry for the same package.
- Do not let automatic candidates override manual selection.

### Locking or Closing

- Locking the database clears visible database-backed results and manual
  selection.
- Ordinary keyboard input remains available while locked.
- Search and fill require unlock.
- IME destruction clears search state and manual selection.

## Settings

Add one switch under the safety settings group, directly below the existing
"secure keyboard" setting:

- Title: keyboard haptic feedback.
- Default: enabled.
- Scope: new lightweight keyboard keys and search control keys only.
- No sound setting.
- Not part of the autofill settings group.

## Security and Privacy Rules

- Ordinary typed text is never retained.
- Search query exists only in memory.
- Search query is cleared on search exit, database lock, and IME destruction.
- No ordinary typed text, search query, username, password, TOTP, or protected
  field value is logged.
- Password fields do not participate in IME search.
- Protected fields do not participate in IME search.
- TOTP secrets do not participate in IME search.
- Result UI never displays passwords.
- Browser initial candidates require package-bound, TTL-bound domain context.

## Test Plan

### JVM Unit Tests

`ImeKeyboardStateTest`:

- Ordinary mode and search mode transitions.
- Search query input, spaces, backspace, clear, and exit.
- Shift temporary uppercase, lock uppercase, and reset behavior.
- Alphabet and symbol page switching.

`ImeKeyboardLayoutTest`:

- QWERTY keys are defined.
- Number/symbol characters are defined.
- Function keys map to the correct actions.

`ImeEntrySearchEngineTest`:

- Title exact, prefix, and contains ranking.
- URL/domain outranks username.
- Username outranks notes and other allowed fields.
- Ties preserve original order.
- Result count is capped at 10.
- Password fields are excluded.
- Protected fields are excluded.
- TOTP secrets are excluded.

`ImeBrowserDomainContextTest`:

- Same package plus valid TTL returns a domain.
- Different package returns no domain.
- Expired TTL returns no domain.
- Clear logic works.

`ImeManualSelectionPolicyTest`:

- Manual selection survives same-package input-field switches.
- Manual selection is not overridden by automatic candidates.
- Manual selection clears on new search, database lock, IME destruction, or
  package change.

### Source Structure Tests

`InputIMEServiceSourceTest`:

- `InputIMEService` uses the new component boundaries.
- Search mode does not directly call `commitText` for query input.
- Ordinary input does not mutate search query state.
- Existing fill buttons remain wired.

`AppSettingImeKeyboardPreferenceTest`:

- Keyboard haptic feedback setting is in the safety group.
- The setting appears directly below the secure keyboard item.
- The default value is enabled.
- The setting is not in the autofill group.

### Manual ADB Verification

- Install debug APK.
- Enable KeePassA IME.
- Open a browser input field and switch to KeePassA IME.
- Verify ordinary English, number, symbol, space, backspace, and enter input.
- Enter search mode and type a query; verify the browser input field is not
  modified.
- Verify search results appear horizontally, first result is selected, and
  tapping another result changes selection.
- Fill username and password from the selected result.
- Verify database locked state still allows ordinary keyboard input but requires
  unlock for search and fill.
- Verify browser initial candidates do not appear from stale domains.
- Verify haptic feedback follows the new setting.

## Risks and Mitigations

- IME layout height may cover too much browser content.
  - Mitigation: fixed rows, horizontal results, ellipsized text, no vertical
    result list in the first version.
- Search ranking may differ from the full app search.
  - Mitigation: IME search has intentionally narrower privacy semantics and
    dedicated tests.
- Browser domain context can become stale.
  - Mitigation: package-bound TTL context; no direct trust in the old global
    domain string.
- `InputIMEService` can grow too large.
  - Mitigation: split state, layout, search, domain context, and preferences
    into dedicated components with JVM tests.
- Device IME behavior varies by ROM.
  - Mitigation: keep core logic in pure Kotlin tests and use manual ADB
    verification on target devices.

## Implementation Sequence

1. Add pure Kotlin state and policy classes with tests.
2. Add IME search engine and ranking tests.
3. Add browser-domain context and connect it to autofill parsing.
4. Add keyboard haptic setting under the safety group.
5. Update IME layouts and adapters for the search area, result item auxiliary
   text, and keyboard rows.
6. Wire `InputIMEService` to the new components.
7. Run focused unit/source tests.
8. Install and verify manually with ADB in browser and non-browser fields.
