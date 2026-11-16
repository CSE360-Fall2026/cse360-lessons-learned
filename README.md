# CSE 360 Lessons Learned System

Team project for CSE 360 (Fall 2026), built on the Foundations code by Lynn Robert Carter.

**Team:** Emilia Davey, Virgil Jones, David Nevarez, Daniel Martinez Julio, Alex Meyers

## What it does
A JavaFX desktop application with an H2 database. Admins manage users and roles. Contributors create,
view, search, update, and delete their own lessons learned, record experience entries, and see which
fields a curator has locked.

## Run it in Eclipse
1. File > Import > General > Existing Projects into Workspace, and select this folder.
2. Make sure JavaFX is on the module path (same setup as HW1/HW2).
3. Run `applicationMain.FoundationsMain`.

The database is created in your home folder (`~/FoundationDatabase.mv.db`), not in this repository.

## Tests
Close the app first (H2 allows one connection), then run:
- `lessonTests.AllContributorTests` (66 requirement tests; each test class in `lessonTests` can also run on its own)
- `inputValidation.LessonRecognizerTest` (58 input validation tests)
- `lessonTests.ManualGuiTests` (manual GUI tests M1-M7; run with `setup USERNAME` first to add sample lessons)

`Requirements-Test Traceability.pdf` lists every requirement and its tests.

## Aspect 6 prototype (TP3-1)
Run `.\scripts\Test-Aspect6.ps1 -RunDemo` in PowerShell to compile and verify the
AI editing/refining model, related regression tests, and a deterministic terminal
demo. The script uses disposable databases. See `docs/aspect6-prototype.md` for
the AI-provider boundary, Eclipse setup, story traceability, and existing baseline failures.

## Working with Jira
Put the Jira work item key at the start of every commit message, for example
`TP2-25 Relabel Role1 as Contributor on admin pages`, so the commit shows up on that card.
