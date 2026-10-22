# SE-Lab learning path

Follow the labs in order. Each lab builds on the earlier lesson and has its own runnable project or code snapshot.

| Lab | Topic | What you learn | Project |
| --- | --- | --- | --- |
| 01 | In-memory notes | JavaScript arrays and temporary page memory | [labs/01-memory](./01-memory/) |
| 02 | Browser persistence | Saving strings as JSON in browser `localStorage` | [labs/02-localstorage](./02-localstorage/) |
| 03 | Why a backend? | REST requests and notes in a Java collection | [labs/03-backend](./03-backend/) |
| 04 | Why a database? | PostgreSQL, SQL, JPA, and persistence across application restarts | [Lab 04 guide in the repository README](../README.md) |
| 05 | Git, testing, and regressions | Inspect changes safely, test API behavior, and reproduce a bug in an isolated copy | [labs/05-git-testing](./05-git-testing/) |
| 06 | Docker and reproducible environments | Package the backend and run it with PostgreSQL, networking, and persistent storage | [labs/06-docker](./06-docker/) |
| 07 | Continuous integration | Run backend tests automatically on pushes and pull requests with GitHub Actions | [labs/07-ci](./07-ci/) |

Lab 04 is the current root application. Lab 05 adds tests and isolated learning exercises without changing its API. Lab 06 adds container workflows for the same app. Lab 07 runs those backend tests automatically on GitHub. Earlier lab directories preserve the implementations from when those labs were introduced.
