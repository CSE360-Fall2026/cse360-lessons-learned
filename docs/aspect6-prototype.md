# TP3-1: Aspect 6 prototype — AI editing and refining (E3)

Daniel Martinez Julio · November 13, 2026

Built on team `main` at `a5e467b` (Curator home/role, merged lock prototype,
and the shared AI response cleaner). This is the code/JUnit prototype for
the overdue **B-01 / TP3-1** card. The production page and live D2 integration
remain part of **B-15 / TP3-19**, due November 18.

## What this prototype demonstrates

1. A logged-in Curator opens an immutable snapshot of a selected lesson.
2. The model checks the stored role, selected field, current revision, and
   explicit consent before invoking its suggestion-provider adapter.
3. The result contains the original and cleaned suggestion for side-by-side
   review. Requesting/viewing a suggestion writes nothing.
4. The Curator can accept, edit, discard, retry, or continue manually after
   an AI failure. Only an explicit save passes the reviewed values to the model.
5. The normal lesson recognizer validates each selected field before saving.
6. One prepared SQL update saves the title/description, preserved first
   originals, Curator username, and timestamp together. It independently checks
   the current stored role and review revision at the write boundary.
7. A Curator can refine fields locked against Contributors. Every lock stays
   enabled; existing Contributor edit/delete refusals still work.
8. An attribution-note formatter supplies “Edited by curator” and the save time
   to an already authorized view. It does not grant access to a lesson.

The update does not overwrite ownership, tags/category, E2 search terms,
E4/E5 summary settings, Contributor coaching fields, or E6 locks.

## Scope of the AI adapter

Team `SharedAiService` currently implements `cleanAiSuggestion`, with six tests;
it does not yet expose a network request or persisted consent workflow.
`ModelCuratorRefine.SuggestionProvider` is the E3 integration boundary for that
shared D2 service. It receives only the chosen stored field and optional goal.
The caller supplies the accepted-consent decision; this prototype does not
claim to implement D2's one-time consent notice, settings, credentials, provider
requests, or GUI. No external AI service is contacted by the tests or demo.

`CuratorRefinementDemo` labels its response as a deterministic fixture. This lets
the team reproduce and test the risky review/save behavior before D2 is ready.

## Run from a PowerShell terminal in this repository

```powershell
.\scripts\Test-Aspect6.ps1 -RunDemo
```

The script compiles all sources and runs the new Aspect 6 tests plus the
existing Curator home, lock, and shared-AI JUnit suites. It also runs the 66
existing Contributor tests. Each run uses a separate `build/aspect6/run-*`
folder, disposable database home, logs, and JUnit XML reports. It never opens
the normal user-home database or starts a GUI.

The defaults match the team's Windows JDK 25 / JavaFX 25 / H2 setup; use
`-JdkBin`, `-JavaFxLib`, `-H2Jar`, or `-JUnitJar` for other installations.
If no console JUnit JAR is supplied, the script downloads the fixed JUnit
Platform 1.14.4 standalone runner from Maven Central into ignored build output.
No dependency JAR, database, credential, or compiled class belongs in the commit.

Other useful runs:

```powershell
.\scripts\Test-Aspect6.ps1 -Scope Aspect6
.\scripts\Test-Aspect6.ps1 -Scope All
```

In Eclipse, run `curatorRefinementTests.CuratorRefinementTests` as a **JUnit Test**.
Set its VM argument `-Duser.home=C:\path\to\a\disposable-test-folder` first.
The application must be closed if using the same database home as another run.

## Traceability to the reviewed user stories

| Story | Prototype behavior | Main test IDs |
|---|---|---|
| US-C-E3-01 received E3 | Open → request → review → explicit save | RF-01, 05, 06, 09, 38 |
| US-C-E3-02 authorization | D1 session rule plus current stored role; SQL role guard | RF-02, 28, 29, 34, 36, 42 |
| US-C-E3-03 request selection/consent | Title/description, optional goal, no call without consent | RF-04–08, 26 |
| US-C-E3-04 review/control | Original plus suggestion; no automatic save; cancel/no-op | RF-06, 25, 33, 38 |
| US-C-E3-05 validation/save | Normal validation, atomic update, retained locks | RF-14–17, 27, 30–32, 37–40 |
| US-C-E3-06 originals/audit | First originals, latest Curator/time, stale-review guard | RF-09, 10, 13, 15–17, 31 |
| US-C-E3-07 attribution | Stored attribution and formatter for an authorized view | RF-11, 12; production-view wiring remains |
| US-C-E3-08 provider failures | Safe message, retained review, manual fallback/retry | RF-18–24, 35, 41 |

RF-14 checks the agreed E3/E6 policy in both directions: Curator refinement
succeeds while Contributor edits/deletion fail and the stored locks remain set.

## Existing failures on the starting main branch

Verified locally on November 13, 2026:

- Aspect 6: **42/42 JUnit tests passed**.
- Aspect 6 plus existing Curator/lock/shared-AI suites: **137/137 passed**.
- Existing Contributor console checks: **66/66 passed**.
- Complete JUnit scan: **212 tests, 203 passed, nine existing failures**.
  The failed test identities exactly match the starting baseline; no new failures.
- Both class-path and Eclipse module-path compilation passed. The new prototype
  Javadoc generated successfully with full doclint enabled.

A baseline full JUnit scan before this change found 170 tests: 161 passed and
nine failed. Those failures are outside Aspect 6 and are not suppressed:

- `hw3Tests.UserNameRecognizerTest`: `un23_invalidCharacterEarly`,
  `un24_invalidCharacterEarlyInLongName`, `un25_tooLongAfterSeparator`,
  `un26_nullInput`.
- `hw3Tests.PasswordRecognizerTest`: `pw23_flagsAfterEarlyExit`, `pw24_nullInput`,
  `pw25_errorMessageField`.
- `database.DatabaseAuthenticationTest`: `testPasswordNotStoredAsPlaintext`,
  `testUpdatedPasswordNotStoredAsPlaintext`.

`-Scope All` includes those tests and therefore reports a failure until their
separate fixes land. The default regression selection is the complete set of
Aspect 6 and directly related Curator/Contributor/D2 checks, not a disabled
version of the full suite.

## Remaining TP3 integration (November 18 feature task)

- Wire the model to the Curator home page's refinement view, original/suggestion
  comparison, editable final draft, explicit save, and cancel controls.
- Supply Virgil's shared D2 provider and consent workflow. Keep asynchronous
  network work off the JavaFX application thread.
- Display the attribution note in authorized Contributor/Curator/Viewer views.
- Keep the reviewed E3/E6 policy aligned with Emilia's lock page and combine
  these stories with the team's Staff User Stories document.
