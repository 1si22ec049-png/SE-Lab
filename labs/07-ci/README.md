# Lab 07 — Continuous Integration with GitHub Actions

This lab asks GitHub to build and test the Notes backend whenever changes are pushed to `main` or a pull request targets `main`. It repeats the Maven test command in a clean runner and starts a temporary PostgreSQL service for the API integration tests.

## Learning objectives

- Explain continuous integration (CI) in everyday language.
- Identify the local command that CI repeats.
- Read a GitHub Actions workflow, job, steps, runner, and trigger.
- Understand why the PostgreSQL integration tests need a database in CI.
- Find logs for a failed workflow run and identify the failing step.
- Practice a failing test on a separate branch without changing `main`.

## What CI solves

Before CI, a developer could make changes, run tests on their own computer, and discover only later that something failed elsewhere. Different tool versions, missing setup steps, or forgotten tests can make a change unreliable.

**Continuous integration** (CI) automatically checks proposed code changes. In this lab, GitHub Actions checks out the repository, prepares Java 17, starts PostgreSQL, and runs the same Maven tests used locally. CI does not prove that software has no bugs, but it makes these repeatable checks visible to the team before changes are accepted.

The current manual backend test command, run from the backend folder, is:

```powershell
mvn test
```

That command runs the JUnit unit tests and the Spring Boot API integration tests. The API tests use the `test` profile and require a PostgreSQL database named `selab_notes_test`; their cleanup guard refuses to clear any other database. The workflow provides this dedicated database as a temporary PostgreSQL service for the job. It does not use the developer database or require any repository secrets.

To run the complete suite locally from PowerShell, first configure the test variables as described in [Lab 05](../05-git-testing/README.md#2-test-safety-and-database-setup), then:

```powershell
Set-Location .\backend
mvn test
```

For the unit tests alone, which do not need PostgreSQL:

```powershell
mvn -Dtest=NotesControllerTest test
```

## What the workflow terms mean

- **Workflow:** The automated instructions in `.github/workflows/backend-ci.yml`.
- **Trigger:** The Git event that starts the workflow. This workflow runs for pushes to `main` and pull requests whose target is `main`.
- **Runner:** A temporary GitHub-hosted Ubuntu machine that performs the job.
- **Job:** A group of related steps. The `Test backend` job has a PostgreSQL service and runs the Maven test suite.
- **Step:** One action within the job, such as checking out files, installing Java, or running Maven.
- **Service container:** A temporary PostgreSQL container that the job can connect to at `localhost:5432`.
- **Log:** The output from a step. Maven's test report shows which tests ran and whether they passed.
- **Pass/fail status:** A green check means all required steps completed successfully. A red X means a step failed; open its log to find the error.

## Read the workflow

The workflow is [backend-ci.yml](../../.github/workflows/backend-ci.yml).

1. `on` selects pushes to `main` and pull requests targeting `main`.
2. `jobs.test.runs-on` asks GitHub for an Ubuntu runner.
3. `services.postgres` creates `selab_notes_test`, sets a CI-only username/password, publishes PostgreSQL port 5432, and waits for `pg_isready`.
4. `defaults.run.working-directory: backend` makes shell commands run from the Maven project directory.
5. `actions/checkout` retrieves the submitted source.
6. `actions/setup-java` installs Temurin Java 17 and caches dependencies using `backend/pom.xml`.
7. `mvn --batch-mode --no-transfer-progress test` runs the full tests, including integration tests; it does not skip tests.

The database password in the workflow is a disposable test value, not a production secret. It is scoped to the temporary service and test job. Do not put real credentials in workflow files.

## Inspect a workflow run on GitHub

These steps require the repository and a commit containing the workflow to be available on GitHub. Do not commit or push without following the repository owner's approval process.

1. Open the repository page on GitHub.
2. Select the **Actions** tab.
3. Select **Backend CI** in the workflows list.
4. Open the run for the push or pull request you want to inspect.
5. Expand **Test backend** to see its steps.
6. A green check beside a step means it completed successfully. A red X marks the failed step. Open that step and read the first meaningful error, then compare it with the Maven test name and expected/actual values.
7. If Maven fails, inspect the end of the **Run backend tests** log for the Surefire summary, such as `Tests run`, `Failures`, and `Errors`. Distinguish a failed assertion from a PostgreSQL startup/connection failure.
8. After fixing the cause, push the fix to the same branch or update its pull request. A new Actions run should appear. Confirm every required step is green before merging.

If **Backend CI** does not appear in Actions, confirm the workflow is on the repository's default branch and has been pushed. If no run appears for a change, check whether the push was to `main` or the pull request targets `main`.

## Controlled failure exercise on a separate branch

This experiment changes one test expectation, not application code. It must be done on a separate branch and must never be merged while failing. The current workspace has no Git commits or configured remote, so first make sure an approved baseline exists on GitHub before following the branch/pull-request steps. Do not initialize, commit, or push as part of this lab without approval.

### Prepare an isolated branch

Once a baseline commit exists and your working tree is safe to branch:

```powershell
git status
git switch -c lab-07-ci-failing-test
```

If `git status` shows unrelated changes, stop and decide how to preserve them before branching. Do not use reset or discard commands.

### Introduce and observe the failure

Open `backend/src/test/java/com/selab/notes/NotesControllerTest.java`. In `createsNoteWithTrimmedText`, temporarily change the expected value from `"Learn testing"` to `"This expectation is intentionally wrong"` in the assertion for the response body. Do not edit the `main` branch.

Run the unit test locally:

```powershell
Set-Location .\backend
mvn -Dtest=NotesControllerTest test
```

**Expected result:** Maven reports one failed assertion in `createsNoteWithTrimmedText`; the actual text remains `Learn testing`. This demonstrates that the test catches a violated expectation before the change can be considered green.

Restore the expected value to `"Learn testing"` and run:

```powershell
mvn -Dtest=NotesControllerTest test
mvn test
```

The unit test should pass. The full suite needs the disposable `selab_notes_test` database configured as described above.

### Optionally observe the failure in Actions

GitHub Actions can only run this experiment after the branch and workflow are on GitHub. After obtaining approval to publish the test-only change:

1. Review `git diff` and confirm that only the deliberate test expectation changed.
2. Commit and push only the exercise branch using the repository's approved process; do not push the deliberate failure to `main`.
3. Open a pull request targeting `main`. The `pull_request` trigger starts **Backend CI**.
4. Open the failed **Run backend tests** log and find the assertion mismatch.
5. Correct the test expectation, review the diff, then update the same pull request after approval.
6. Confirm the updated workflow run is green. Do not merge a branch while its required test is failing.

The local branch and the intentional failure are learner actions. This guide does not create a branch, modify tests, commit, or push automatically.

## Common failures

| Symptom | Likely cause | How to investigate |
| --- | --- | --- |
| Workflow does not start | Workflow is not on GitHub yet, or event does not target `main` | Check the Actions tab, branch, and pull request base branch. |
| Java setup fails | Runner/tool download issue or a Java version mismatch | Expand **Set up Java 17 and cache Maven dependencies**; confirm the POM still targets Java 17. |
| PostgreSQL health check fails | Service did not become ready or its environment settings changed | Inspect the service startup output and health-check command; verify database name and username match the test variables. |
| Spring cannot connect to PostgreSQL | Wrong test URL, credentials, port, or service readiness | Expand the test log and verify `TEST_DB_URL` uses `localhost:5432/selab_notes_test`. |
| A specific assertion fails | Application behavior changed or a test expectation is incorrect | Read the test name and expected/actual values, then inspect that assertion and implementation. |
| Tests pass locally but fail in CI | Local state or configuration differs from the clean runner | Recheck test isolation, environment variables, supported Java version, and PostgreSQL setup. |
| Maven reports no tests | Wrong working directory or test selector | Confirm the Maven command runs from `backend` and does not use a misspelled selector. |

Never “fix” a red workflow by skipping tests or changing a correct assertion merely to make CI green. Identify and correct the underlying issue.

## Glossary

- **Continuous integration:** Automatically running repeatable checks when code changes are proposed or shared.
- **GitHub Actions:** GitHub's service for running workflows.
- **Workflow:** A YAML file describing triggers, jobs, and steps.
- **Runner:** The temporary machine that executes a workflow job.
- **Job:** A collection of steps that runs on a runner.
- **Step:** A command or reusable action inside a job.
- **Trigger:** An event, such as a push or pull request, that starts a workflow.
- **Service container:** A supporting service, such as PostgreSQL, started for a job.
- **Cache:** Reusable downloaded dependencies that can make later builds faster; it does not replace tests.
- **Regression:** A previously working behavior that breaks after a change.

## Interview questions

**What does CI do?**

It automatically runs repeatable checks on code changes so failures are found before the changes are accepted.

**Why does this workflow start PostgreSQL?**

The API integration tests use Spring Boot and JPA against PostgreSQL. A temporary test database makes those tests reproducible on the runner.

**What is the difference between a workflow, job, and step?**

A workflow is the whole automation, a job is a group of work on a runner, and a step is one action or command within that job.

**Does a green CI run prove the app has no bugs?**

No. It means the configured checks passed for that run; tests cannot cover every possible behavior.

**Why use a separate branch for a deliberately failing test?**

It isolates the learning experiment from `main` and lets the learner observe a failure without leaving the stable branch broken.

## Completion checklist

- [ ] I can explain the CI workflow in plain language.
- [ ] The workflow uses Java 17 and runs Maven tests from `backend`.
- [ ] PostgreSQL is healthy before the integration tests execute.
- [ ] I can find a run and failing step in GitHub Actions logs.
- [ ] I reproduced the deliberate assertion failure on a separate branch or can explain the safe steps.
- [ ] I restored the test expectation and verified the suite.
- [ ] I did not commit, push, or merge without approval.

## Learning path

- Previous: [Lab 06 — Docker and reproducible environments](../06-docker/).
- Earlier: [Labs 01–05](../README.md).
- Next: no later lab is included here.
