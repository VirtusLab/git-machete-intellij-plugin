# AGENTS.md

Guidelines for AI coding agents working in this repository.

## Code comments

- Explain why the code is the way it is, not what it used to be.
  - Bad: `// Today this also happens accidentally, because the scripts used to live under .../resources`
  - Bad: `// Previously we did X, but switched to Y because ...`
  - Bad: `// This was added to fix the regression introduced by commit abc1234`
  - Good: `// Y is required because Z`
- Refer to the current state of the codebase, the runtime contract, or the
  invariants being enforced. The reader has `git log` and `git blame` for the
  history; comments should not duplicate that.
- One exception: when the current code intentionally guards against a
  recurrence of a specific bug, it's fine to name the bug, but phrase it
  forward-looking ("guards against X" rather than "fixes the X regression").
- Do not narrate the change in code comments (e.g. "Added the X parameter
  to ..."). Write comments that would still make sense to a reader who has
  never seen the previous revision.
- Do not add obvious, redundant comments that just restate the next line of
  code (e.g. `// Increment the counter` above `counter++`). Comments should
  carry information the code itself cannot convey.
