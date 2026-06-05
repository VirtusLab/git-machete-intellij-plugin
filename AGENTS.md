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

## Test code

- Assert on the *full* output rather than substring presence.
  Compare the whole rendering with an equality assertion (e.g.
  `assertEquals(expectedOutput, actualOutput)`) so the entire output is
  pinned down at once.
  Avoid `assertThat(output).contains("<phrase>")` / "this phrase is not in
  the output" checks for command-output testing - a substring match silently
  tolerates stray extra lines, misordered sections, the same phrase landing
  on the wrong row, or new (unintended) labels appearing elsewhere, all of
  which a full-output equality assertion would catch on the first run.
  Exception: substring checks are legitimate when the asserted invariant is
  genuinely scoped to one fragment of the output (e.g. "this warning text
  appears somewhere") and the rest of the output is either non-deterministic
  or already covered by another test.
- Don't add code comments that narrate test-harness internals or explain why
  an assertion's expected value was massaged to match the harness's output
  (e.g. "the harness lower-cases this, so we compare against lower-case",
  "the formatter strips trailing whitespace, so the expected string omits
  it", etc.).
  If the actual and expected values match, the assertion already documents
  itself; if they don't, fix the production code or the harness, don't
  annotate the workaround.
  This generalizes the broader code-comments rule: avoid comments that
  exist solely to justify a specific literal in a test - the test name and
  the assertion are the contract.

## Git

- Don't `git commit` or `git push` unless explicitly asked.

## Formatting

- No trailing whitespace on any line.
  (Enforced by `scripts/prohibit-trailing-whitespace`.)
- Always leave a single newline at the end of every file.
  (Enforced by `scripts/enforce-newline-at-eof`.)
- Always use American English spelling (`color`, `behavior`, `honor`,
  `organize`, `modeled`, `normalized`, `unrecognized`, ...) - never the
  British variants (`colour`, `behaviour`, `honour`, `organise`, `modelled`,
  `normalised`, `unrecognised`, ...).
  Applies to code, identifiers, comments, Javadoc/KDoc, Markdown, commit
  messages and PR descriptions.
- Don't hard-wrap prose (code comments, Javadoc/KDoc, Markdown, commit
  messages, PR descriptions) at 80 or so columns.
  Break on sentence or clause boundaries instead, so each line carries one
  thought rather than a fragment chopped by column count.
  Prefer one sentence per line; for a sentence that exceeds the project's
  line-length limit, split at a natural clause boundary (semicolons,
  parentheticals, conjunctions, ...).
- This matters especially for PR descriptions (and commit message bodies):
  never insert mid-sentence line breaks at a fixed column.
  Write them in natural flow - one sentence per line, exactly like code
  comments - and let GitHub / the git client wrap them for display.
  Note that a PR opened from a single commit inherits that commit's message
  verbatim, so a hard-wrapped commit body produces a hard-wrapped PR
  description; keep the commit body unwrapped too.
