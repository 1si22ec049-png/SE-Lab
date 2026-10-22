# Isolated regression exercise

Follow the **Regression debugging in an isolated copy** section in [Lab 05](../README.md#3-regression-debugging-in-an-isolated-copy).

The exercise copies the backend sources to a uniquely named directory under `$env:TEMP` and uses the separate `selab_notes_regression` PostgreSQL database. The deliberate ordering bug is made only in that temporary copy. It is not present in the root application, and the exercise does not change the root Git repository.
