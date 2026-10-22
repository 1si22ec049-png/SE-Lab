# Lab 05 — Git, Automated Testing, and Regression Debugging

This lab practices protecting working code, checking behavior with tests, and tracing a regression to a violated expectation. It does not add features to the Notes App or modify its database schema.

## Learning objectives

By the end of the lab, you can:

- Describe expected behavior, actual behavior, assertions, and test cases.
- Distinguish unit tests from API integration tests.
- Run the Notes API tests against a dedicated PostgreSQL test database.
- Reproduce and fix an ordering regression in a disposable project copy.
- Read Git status and diffs, stage selected files, and understand what a commit records.
- Explain why a local commit is different from pushing to a remote.

## Prerequisites

- Java 17 or newer and Apache Maven 3.6.3 or newer.
- PostgreSQL from Lab 04, running locally.
- The `selab` database role and password from [Lab 04 setup](../../README.md#postgresql-setup-on-windows).
- PowerShell and Git.

The application remains the Lab 04 PostgreSQL-backed app. This lab uses its existing JUnit, Mockito, and Spring Boot testing dependencies.

## 1. Testing concepts

A **test** runs a small, repeatable check of a program. A **test case** describes one situation, such as “creating a note with surrounding spaces stores the trimmed text.” An **assertion** compares the actual result with the expected result. If they differ, the test fails and prints the violated expectation.

For this API, an expected result might be “GET returns notes in ascending ID order.” The actual result is the HTTP response produced by the running controller and repository. A passing assertion shows those values match for that test case; it does not prove that every possible case works.

- A **unit test** checks a small part of a program in isolation. `NotesControllerTest` supplies a mocked repository, so it can check request validation and controller decisions without starting Spring Boot or connecting to PostgreSQL.
- An **integration test** checks that parts work together. `NotesApiTest` sends mock HTTP requests through Spring MVC and JPA to PostgreSQL. It verifies routes, JSON/status behavior, persistence operations, and input errors.
- **Manual testing** is useful for seeing the app, but people forget steps and may test different data each time. Automated tests repeat the same checks quickly.
- **Regression testing** reruns a test after a change to check that behavior that used to work still works. The separate regression exercise below intentionally changes list ordering to make the expected failure visible.
- **Test isolation** means one test starts with known data and does not depend on leftovers from another test.
- **Reproducibility** means another learner can follow the same setup and get the same check and result.

There is no separate business service in this small app. The controller validates and trims input, then delegates storage to `NoteRepository`. We test that controller behavior with a mock, and use integration tests for the real PostgreSQL repository. We do not add a service layer just to have another class to unit-test.

## 2. Test safety and database setup

The API tests use the `test` Spring profile and a separate database named `selab_notes_test`. They do **not** use the development database by default. Before and after each API test, the test fixture clears rows from its database. Before cleanup, it checks the connected database name against the allowlist `selab_notes_test` and `selab_notes_regression`; it refuses to clear notes from any other database. Keep `TEST_DB_URL` pointed at one of these disposable test databases, and never point tests at a database containing data you care about.

In `psql`, connected as a PostgreSQL administrator, create the dedicated test database once:

```sql
CREATE DATABASE selab_notes_test OWNER selab;
\q
```

If this database already exists, do not create it again. Set test-only connection variables in the PowerShell window where you will run tests:

```powershell
$env:TEST_DB_URL = "jdbc:postgresql://localhost:5432/selab_notes_test"
$env:TEST_DB_USERNAME = "selab"
$env:TEST_DB_PASSWORD = Read-Host "Enter the local PostgreSQL password for selab"
```

The test profile defaults to the database name `selab_notes_test` and role `selab`; the password has no project-file default. The PostgreSQL server must be running. Tests fail at startup if PostgreSQL or the configured test database is unavailable. No embedded or in-memory database silently replaces PostgreSQL.

Run the tests from the repository folder:

```powershell
Set-Location .\backend
mvn test
```

To run only the mocked controller unit tests:

```powershell
mvn -Dtest=NotesControllerTest test
```

To run only the PostgreSQL API integration tests:

```powershell
mvn -Dtest=NotesApiTest test
```

The current test cases check that the controller trims valid text, rejects missing/blank text without saving, asks for ascending ordering, and delegates deletion. The API tests check creation, retrieval, deletion, generated IDs, database access, ordering, blank and malformed JSON, deletion of a missing ID, and the existing CORS origin. Deleting a missing ID currently returns `204 No Content`, matching the controller's idempotent delete behavior.

### Results observed while building this lab

The tests were run from a temporary copy of the backend against a disposable PostgreSQL 18.6 cluster, using Java 21.0.12.1 and Maven 3.9.16:

- Baseline: **9 tests passed, 0 failures, 0 errors**.
- Deliberate descending-sort bug in the temporary copy: the API test class reported **5 tests, 1 failure**; the ordering assertion expected `First` but received `Second`.
- After restoring ascending order: **5 API tests passed**, followed by **9 full-suite tests passed**, with no failures or errors.

The temporary project and database were removed after validation. These are observed results, not a guarantee about another machine; reproduce them with the commands above.

### Interpret a failure

Maven prints the failing test name and an assertion such as `expected: <...> but was: <...>`, plus a source line. Read the expected and actual values first. Then use the named test to find the exact request or controller behavior; compare it with the relevant route and inspect the backend logs only if the failure is a connection or startup error.

For example:

```text
expected: <[First, Second]> but was: <[Second, First]>
```

This means the endpoint responded, but the actual note ordering disagreed with the API's expected ascending-ID order. It is not a PostgreSQL connection failure.

## 3. Regression debugging in an isolated copy

The deliberate bug belongs only in a temporary copy under your Windows temporary directory. It does not edit the root application or its tests. Close the editor tab for any copied file when finished; the copied directory is disposable.

### Prepare a separate database

Create a second disposable database for this experiment, so the exercise cannot clear the regular test or development notes:

```sql
CREATE DATABASE selab_notes_regression OWNER selab;
\q
```

If it already exists from an earlier exercise, it can be reused: each test clears its own rows. Set its credentials:

```powershell
$env:TEST_DB_URL = "jdbc:postgresql://localhost:5432/selab_notes_regression"
$env:TEST_DB_USERNAME = "selab"
$env:TEST_DB_PASSWORD = Read-Host "Enter the local PostgreSQL password for selab"
```

### Make and observe the regression

From the repository folder, copy only the Maven project descriptor and source files to a uniquely named temporary directory:

```powershell
$exercise = Join-Path $env:TEMP ("se-lab-regression-" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $exercise | Out-Null
Copy-Item .\backend\pom.xml -Destination $exercise
Copy-Item .\backend\src -Destination $exercise -Recurse
```

Run the passing baseline tests in this isolated copy:

```powershell
mvn -f (Join-Path $exercise "pom.xml") -Dtest=NotesApiTest test
```

Expected: `BUILD SUCCESS`. The integration test `getNotesReturnsNotesInAscendingIdOrder` creates two notes and asserts the returned order.

Open the **copied** controller (not the root project's controller):

```powershell
code (Join-Path $exercise "src\main\java\com\selab\notes\NotesController.java")
```

In `getNotes`, change `Sort.Direction.ASC` to `Sort.Direction.DESC`. Save the copy, then run the test again:

```powershell
mvn -f (Join-Path $exercise "pom.xml") -Dtest=NotesApiTest test
```

Expected: the ordering test fails, reporting `[First, Second]` as expected and `[Second, First]` as actual. This proves that the test detects the behavior it asserts. It does not mean the bug reached the working application: only the temporary copy was changed.

Change `DESC` back to `ASC` in the copied controller. Run the focused integration test and then the full copy test suite:

```powershell
mvn -f (Join-Path $exercise "pom.xml") '-Dtest=NotesApiTest' test
mvn -f (Join-Path $exercise "pom.xml") test
```

Expected: both finish with `BUILD SUCCESS`. The API-class run proves the fix restores the expected behavior; the full run also checks the mocked controller tests. The main `backend` directory was never edited by this experiment.

**Troubleshooting:** confirm PostgreSQL is running, `selab_notes_regression` exists, and the `TEST_DB_*` variables still point to that database. A connection failure is different from the expected ordering assertion. If the test still passes after changing the copy, verify that the edited file is under `$exercise`, that it was saved, and that Maven is using that copy's `pom.xml`.

## 4. Git fundamentals and safe practice

Git records versions of files. These are separate stages:

1. **Working tree:** files as they currently appear on disk.
2. **Staging area:** selected changes prepared for the next commit.
3. **Commit:** a local, named snapshot of staged changes.
4. **Remote:** another repository, often hosted elsewhere. A local commit is not uploaded; `git push` sends commits to a remote.

Useful commands:

```powershell
git status
git diff
git add -- path\to\one-file
git diff --cached
git commit -m "Describe one focused change"
git log --oneline
git switch -c experiment/note-order
```

`git diff` shows unstaged changes to tracked files. `git diff --cached` shows what is staged. A new untracked file does not appear in `git diff` until it is staged (or marked intent-to-add with `git add -N`). Always inspect status and the staged diff before committing.

To unstage one file while keeping its working copy:

```powershell
git restore --staged -- path\to\one-file
```

To restore one **tracked** file from the current commit, first save any work you need and inspect its diff; then, only if you intentionally want to discard that file's changes:

```powershell
git diff -- path\to\one-file
git restore --worktree -- path\to\one-file
```

This discards that file's unstaged edits. Do not use it on work you need to keep. A branch provides an isolated line of history once a repository has a commit; switch back with `git switch main` after the experiment, but only with a clean working tree or safely saved changes. Do not reset the repository to move between branches.

After a baseline commit exists, a branch experiment can be started and left safely with:

```powershell
git status --short
git switch -c experiment/note-order
# Make and test the isolated change; commit it or restore only the specific file if appropriate.
git status --short
git switch main
```

Do not switch away with uncommitted work unless you have confirmed Git can carry it safely or have otherwise saved it.

### Git state in this checkout

Git is initialized, but this checkout currently has no commits and the discovered Git root is `D:\`, which is broader than the SE-Lab project. As a result, the project files show as untracked rather than as edits to a previous version. There is no existing history to restore from, and `git diff` cannot show an untracked file's contents as a change against a commit. Do not run `git add .` from `D:\`, do not initialize another repository there, and do not create a commit or push from this lab automatically.

### Practical Git experiment — use a temporary Git playground

This experiment initializes Git only in a newly created temporary copy, never in `D:\` or the working project. It reuses the temporary project from the regression exercise. First complete its setup, fix the copied controller back to `ASC`, and confirm the copied tests pass. If you no longer have `$exercise`, repeat the copy steps above. In the same PowerShell session:

```powershell
$gitPractice = Join-Path $env:TEMP ("se-lab-git-practice-" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $gitPractice | Out-Null
Copy-Item (Join-Path $exercise "pom.xml") -Destination $gitPractice
Copy-Item (Join-Path $exercise "src") -Destination $gitPractice -Recurse
git -C $gitPractice init
git -C $gitPractice status --short
git -C $gitPractice add -- pom.xml src
git -C $gitPractice diff --cached
```

Inspect the staged files; they should contain only the copied Maven project, not passwords, logs, or build output. Make the first local baseline commit **yourself**, after review:

```powershell
git -C $gitPractice commit -m "Add isolated notes test project"
git -C $gitPractice log --oneline
```

If Git says your author identity is missing, stop and ask the repository owner how they want it configured; do not change Git identity as part of this lab.

Now make a harmless comment-only change to the copied controller in `$gitPractice` (for example, add `// Return notes in stable ID order.` above `getNotes`). Save, then inspect it:

```powershell
git -C $gitPractice status --short
git -C $gitPractice diff -- src/main/java/com/selab/notes/NotesController.java
mvn -f (Join-Path $gitPractice "pom.xml") test
git -C $gitPractice add -- src/main/java/com/selab/notes/NotesController.java
git -C $gitPractice diff --cached
```

The diff should show only the comment in that file. The copied full test suite should pass against the regression database. After reviewing the staged diff, create a local commit:

```powershell
git -C $gitPractice commit -m "Clarify stable note ordering"
git -C $gitPractice log --oneline
git -C $gitPractice status --short
```

No remote is configured or required, and you do not run `git push`. This exercise leaves the main project, its Git settings, and repository history untouched.

### If you later work in a project checkout with existing history

Run Git commands from that checkout and use `git status` before editing. To inspect only SE-Lab files when the repository root is `D:\`, scope commands to the project path and stage paths explicitly:

```powershell
git -C D:\ status --short -- Downloads/Understand-System
git -C D:\ diff -- Downloads/Understand-System/backend
git -C D:\ add -- Downloads/Understand-System/README.md Downloads/Understand-System/backend/src/test/java/com/selab/notes/NotesApiTest.java
git -C D:\ diff --cached -- Downloads/Understand-System
```

Replace the example paths with only the files you intend to stage. Do not use `git add .` from the broad `D:\` root. Do not commit or push without your own review and approval.

If you create a fresh clone with no history, follow the Git owner’s repository workflow. Initialize Git only in a deliberate, new project directory after checking that no `.git` directory already exists. This lab does not initialize, reconfigure, commit, or push the SE-Lab checkout.

For a genuinely new, dedicated project directory only, check its Git root before initializing:

```powershell
$newProject = "C:\path\to\your-new-project"
New-Item -ItemType Directory -Path $newProject
git -C $newProject rev-parse --show-toplevel
```

If `rev-parse` succeeds, stop: the directory already belongs to a repository. If it reports that it is not a repository, initialize only after confirming this is the intended new project directory and following its owner's workflow:

```powershell
git -C $newProject init
git -C $newProject status
```

Do not use these initialization commands on `D:\` or the current SE-Lab folder: this checkout already discovers `D:\` as its repository root.

### Avoid accidental commits

Before staging, look for passwords, `.env` files, local database settings, generated `target` output, logs, and unrelated files. `.gitignore` excludes the local Maven distribution, Maven build output, logs, and local environment files; it is a safety net, not a substitute for inspecting `git status` and `git diff --cached`. Stage named paths only. Never stage database credentials or PostgreSQL data directories.

## Common mistakes

- **Tests cannot connect:** start PostgreSQL, verify the `selab_notes_test` database exists, and check `TEST_DB_URL`, `TEST_DB_USERNAME`, and `TEST_DB_PASSWORD` in the same PowerShell session.
- **Tests changed development notes:** stop; confirm `@ActiveProfiles("test")` and that `TEST_DB_URL` points to `selab_notes_test` or the disposable regression database. Do not run tests against important data.
- **Expected failure does not happen:** verify the copied controller is set to `DESC`, the file was saved, and Maven received the copied `pom.xml`.
- **A test fails with expected/actual values:** compare the assertion and response first; a test failure may indicate an app regression rather than an infrastructure issue.
- **Git diff is empty:** untracked files have no committed baseline. Check `git status`; inspect staged new files with `git diff --cached`.
- **Too many Git files appear:** check `git rev-parse --show-toplevel`. In this checkout it is `D:\`; scope status and add commands to `Downloads/Understand-System` and never stage the drive root.
- **Git commit asks for identity:** do not configure it for this exercise; ask the repository owner.

## Glossary

| Term | Meaning |
| --- | --- |
| Test case | One input/setup and the behavior the program should produce. |
| Assertion | A check that actual behavior matches the expected result. |
| Unit test | A fast test of one small component, usually with dependencies mocked. |
| Integration test | A test of connected components, such as HTTP handling plus PostgreSQL persistence. |
| Regression | A previously working behavior that breaks after a change. |
| Test isolation | Keeping tests independent of other tests' data and state. |
| Working tree | Files currently on disk. |
| Staging area | The selected changes prepared for a commit. |
| Commit | A named local snapshot of staged changes. |
| Branch | A movable name for a line of commits, useful for isolated work. |
| Remote | Another Git repository used for sharing or backup. |

## Interview questions

**What is the difference between a unit and integration test?** A unit test checks a small component in isolation; an integration test checks that multiple components work together, such as the API and database.

**What is a regression test?** A test that protects behavior that previously worked and can be rerun after code changes.

**Why isolate integration-test data?** So tests are repeatable and cannot erase or depend on development or personal data.

**What does `git diff --cached` show?** The staged changes that would be included in the next commit.

**Does a commit upload code?** No. A commit is local; sending it to a configured remote requires a separate push.

## Completion checklist

- [ ] I can run the mocked controller unit tests and PostgreSQL integration tests separately.
- [ ] I can explain a failing assertion using its expected and actual values.
- [ ] I reproduced the ordering bug only in a temporary copy and fixed it there.
- [ ] I confirmed the full test suite passes after the fix.
- [ ] I reviewed a working-tree diff and a staged diff in the temporary Git playground.
- [ ] I did not stage secrets, build output, logs, or unrelated files.
- [ ] I can explain why the SE-Lab checkout itself has no history yet and why I did not initialize or commit it.

Next, continue learning only after these behaviors are clear; no later lab technology is introduced here.
