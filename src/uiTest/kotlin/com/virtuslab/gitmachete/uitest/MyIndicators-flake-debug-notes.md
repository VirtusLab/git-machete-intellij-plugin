# UI test flake: `myWaitForIndicators` 2-minute timeout (issue #2194)

Self-contained notes for the next debugging session. Aimed at being re-consumable
by an LLM so we don't have to re-derive the same conclusions every time.

## Symptom

A spurious failure that happens every few dozen CI runs:

```text
UITestSuite > testPullBranch() FAILED
    com.intellij.driver.sdk.WaitForException: Timeout(2m): Failed: Indicators
        at com.intellij.driver.sdk.WaitsKt.waitFor-FbhrOv8(waits.kt:130)
        ...
        at com.virtuslab.gitmachete.uitest.MyIndicatorsKt.myWaitForIndicators-exY8QGI(MyIndicators.kt:66)
        at com.virtuslab.gitmachete.uitest.BaseUITestSuite.doAndAwait(BaseUITestSuite.kt:138)
        at com.virtuslab.gitmachete.uitest.BaseUITestSuite.checkoutBranch(BaseUITestSuite.kt:205)
        at com.virtuslab.gitmachete.uitest.UITestSuite.testPullBranch(UITestSuite.kt:170)
```

So far reproduced on `:uiTest_2025.3.5` (IntelliJ Ultimate, `IU-253.33514.17`)
and `:uiTest_262.6228.19` (`IU-262.6228.19`, upcoming-major-EAP, also Ultimate).
Community (`:uiTest_2025.2.6.2`, `IC-...`) does not exhibit it.

The action that triggers the wait is irrelevant - it just happens to be the
first `doAndAwait` call after a project open / branch checkout in whichever
test runs into the race. `testPullBranch`'s `checkoutBranch('allow-ownership-link')`,
`testSkipNonExistentBranches_toggleListingCommits_slideOutRoot`'s
`checkoutBranch('master')`, `testFastForwardParentOfBranch`'s
`checkoutBranch('call-ws')` etc. are all candidates.

## Root cause (proven, 2026-05-27 run, IU-262.6228.19)

The IDE is wedged in **dumb mode (indexing)** for the entire 2-minute timeout,
because a single `PushedFilePropertiesUpdaterImpl$MyDumbModeTask
(reason: Push on VFS changes)` task is **paused via a
`CoroutineSuspender` / `TaskSuspender` and never resumed**.

The same task instance (`MyDumbModeTask@5bdd0f95`) appears 13+ times in the
same `idea.log`; healthy occurrences complete in 2-60 ms. The failing
occurrence "finishes" only when the test teardown cancels it via PCE exactly
2 minutes after it started.

Smoking-gun stack (`threadDump-4`, 33 s into the freeze, identical in
`threadDump-5` at 93 s in - same `BlockingCoroutine@4fdb7328` instance):

```text
- "com.intellij.openapi.project.MergingQueueGuiExecutor$ScopeHolder":
   BlockingCoroutine{Active}@4fdb7328, state: SUSPENDED
   [...,
    com.intellij.platform.ide.progress.suspender.TaskSuspenderElement,
    com.intellij.openapi.progress.CoroutineSuspenderElement,
    ...,
    BlockingEventLoop]
    at com.intellij.openapi.progress.CoroutineSuspenderImpl.checkPaused(suspender.kt:118)
    at com.intellij.openapi.roots.impl.PushedFilePropertiesUpdaterImpl.performDelayedPushTasks(PushedFilePropertiesUpdaterImpl.kt:241)
    at com.intellij.openapi.roots.impl.PushedFilePropertiesUpdaterImpl$MyDumbModeTask$performInDumbMode$1$1$1.invokeSuspend(PushedFilePropertiesUpdaterImpl.kt:380)
```

Key facts from the same thread dumps:
- The EDT (`AWT-EventQueue-0`) is **idle**: `EventQueue.getNextEvent` parked.
  Nothing is blocking it.
- The dumb-mode task's worker thread (`DefaultDispatcher-worker-15`) is
  parked in `BlockingCoroutine.joinBlocking` from the `runBlockingCancellable`
  wrapper inside `PushedFilePropertiesUpdaterImpl$MyDumbModeTask.performInDumbMode`.
  The inner coroutine is `SUSPENDED` in `CoroutineSuspenderImpl.checkPaused`.
- The pre-freeze dump (`threadDump-3`, 13:42:20) does **not** contain any
  `checkPaused` frame - i.e. the suspender is healthy until the moment the
  task gets paused at the start of the hang.

### Previous hypothesis (REFUTED)

The earlier version of this file claimed `ProvenanceDatabaseService` was
blocking the EDT for ~60 s and the dumb-mode task was queued behind it.
This is **wrong, at least for the 2026-05-27 / IU-262.6228.19 occurrence**:
- `ProvenanceDatabaseService - Creating ProvenanceDatabase` for the failing
  project (`ee3c87f6`) logs at `13:42:36,376` and completes - **11 s before**
  the hang starts at `13:42:47,992`.
- The EDT is idle throughout the freeze, not in any `ProvenanceDatabaseService`
  frame.

The 2026-05-07 / 2025.3.5 run did show `Creating ProvenanceDatabase` ~at the
start of the hang, but no thread dump from inside that freeze is available,
so the correlation may have been coincidental (the service initializes ~60 s
after every project open). Treat that earlier hypothesis as unverified.

## Open question: who paused the suspender?

The remaining unknown is **which producer paused the `TaskSuspender` and never
resumed it**. The stack tells us the suspender is owned by
`MergingQueueGuiExecutor$ScopeHolder` and that the task was attached via
`TaskSuspenderImpl.attachTask`. Plausible callers:

- VCS heavy-operation hooks (`HeavyAwareListener`-style code in `git4idea` /
  `intellij.platform.vcs`). The hang starts immediately after a
  `git checkout master --` (line 1044-1052 of the failing `idea.log`), so
  the VCS pause-resume protocol is the prime suspect.
- The IDE's `MergingQueueGuiSuspender` itself (visible on the
  `setCurrentSuspenderAndSuspendIfRequested` frame), but that one is the
  *propagator* of the pause, not the original requester.

This level of detail cannot be extracted from a stack trace alone - we need
either an upstream YouTrack with the stack above, or extra debug code (see
"Next-step debug ideas" below) on the *next* CI occurrence.

## Evidence trail

### 1. `myWaitForIndicators` log pattern that exposed the issue

Before the `isDumb()` gate was removed, the failure printed:

```text
Driver.myWaitForIndicators: waiting more since DumbService.isDumb() == true (project is indexing)
...repeated ~140 times for 2 minutes...
```

This was the early-return branch in the old `indicatorState`:
`if (service<DumbService>(project).isDumb()) return IndicatorState.Dumb`.
Crucially, no `background processes are running` line ever appeared in that
window - the EDT-visible progress UI was empty, but `DumbService` still
reported the project as dumb because the `MyDumbModeTask` was technically
still in the queue (just paused). That asymmetry is what makes the current
gate-on-progress-indicators-only mitigation safe.

### 2. `idea.log` timeline of the failing project (2026-05-27, project hash `ee3c87f6`)

```text
13:42:36,376  Creating ProvenanceDatabase ...ee3c87f6/provenance  (clean,
              completes; not in the freeze window)

--- 11 s of normal activity ---

13:42:47,929  git checkout master --
13:42:47,933  Switched to branch 'master'
13:42:47,992  enter dumb mode  [machete-sandbox-worktree]
13:42:47,994  Running task: (dumb mode task) MyDumbModeTask@5bdd0f95
              (reason: Push on VFS changes)        <-- never logs "Task finished"
              while alive

!!! NO LOG ENTRIES (only periodic heartbeat screenshots from JBR) FOR 120s !!!

13:44:48,661  Task canceled (PCE): MyDumbModeTask@5bdd0f95   <-- test teardown
13:44:48,661  Task finished:     MyDumbModeTask@5bdd0f95
13:44:48,669  Project ... is being disposed
```

### 3. `MyDumbModeTask` finishes promptly under normal conditions

In the same `idea.log`, the same task type ran 13+ times before the freeze
and completed in 2-60 ms each:

```text
13:39:36,289  Running    MyDumbModeTask@13939e95
13:39:36,312  Task finished                              (~23 ms)
13:39:49,497  Running    MyDumbModeTask@3cfa85be
13:39:49,500  Task finished                              (~3 ms)
13:40:05,762  Running    MyDumbModeTask@56743675
13:40:05,767  Task finished                              (~5 ms)
... etc ...
```

### 4. Thread-dump availability for the failing run

CircleCI job 14652 (2026-05-27) artifacts contain:
- `threadDump-3-...13-42-20.txt` - 27 s before freeze (clean baseline)
- `threadDump-4-...13-43-20.txt` - 33 s into the freeze (paused at `checkPaused`)
- `threadDump-5-...13-44-20.txt` - 93 s into the freeze (same instance, still
  paused at `checkPaused`)
- `threadDump-6-...13-45-21.txt` - 33 s after the freeze (failing project
  already torn down, next test started)

All four are downloaded under `/tmp/issue2194/` during the most recent
investigation.

## Next-step debug ideas

If we want to confirm WHICH suspender is holding the pause on the next
occurrence, the cheapest move is to extend `MyIndicators.kt`:

- When `DumbService.isDumb()` has been `true` for more than ~30 s, instead
  of just logging "still dumb", reflectively peek at:
    - `DumbServiceImpl.getCurrentTask()` - confirms the task is
      `PushedFilePropertiesUpdaterImpl$MyDumbModeTask`.
    - The `CoroutineSuspender` attached to that task (private; needs
      reflection on `MergingQueueGuiSuspender` or
      `TaskSuspenderImpl.isPaused`).
- Log the result. If `isPaused == true`, we have proof on the same
  occurrence, no need to download thread dumps.

Filing upstream is also a reasonable parallel step: a YouTrack with the
`checkPaused` stack above (plus the fact that the EDT is idle and the
suspender never resumes) is enough for the platform team to investigate.

## Files involved

- `src/uiTest/kotlin/com/virtuslab/gitmachete/uitest/MyIndicators.kt`
  Inlined-from-platform copy of `Indicators.kt` with extra logging; this is
  the spot to add more diagnostics if needed. The TODO at the top references
  this issue (#2194).
- `src/uiTest/kotlin/com/virtuslab/gitmachete/uitest/BaseUITestSuite.kt`
  `doAndAwait` (line ~141) calls `myWaitForIndicators(2.minutes)` - the
  effective per-step timeout.
- `src/uiTest/kotlin/com/virtuslab/gitmachete/uitest/UITestSuite.kt`
  Each test method (`testPullBranch`, `testSkipNonExistentBranches_...`,
  `testFastForwardParentOfBranch`, ...).

## Mitigation in place

`MyIndicators.indicatorState` **no longer consults `DumbService.isDumb()`**;
the wait is gated solely on the list of status-bar progress indicators
(`StatusBar.getBackgroundProcessModels()`). This is enough because:

- The paused `MyDumbModeTask` does NOT contribute a progress indicator
  (we never see a `background processes are running` line during the freeze
  window in `idea.log`), so it is now invisible to the wait.
- Healthy occurrences of the same task finish in 2-60 ms, far below any
  test-meaningful threshold; they would be missed by polling anyway.
- Every legitimate long-running scanning/indexing operation (initial
  project indexing, "Updating Git Machete status...", "Closing attached
  shared indexes...", VCS log refresh, etc.) is visible in
  `getBackgroundProcessModels()`, so the test still waits for them.
- The git/VCS/UI actions exercised by these tests do not depend on PSI
  indexes being ready, so we don't need smart-mode protection.

Side effect: if a future regression introduces a legitimate dumb-mode task
that lacks a progress indicator AND that the test actually needs to wait
for, this wait will no longer catch it. None of the current actions look
like they would suffer; if a future test does, prefer adding a dedicated
`Driver.waitForSmartMode(timeout)` call at that site rather than
reintroducing the global `isDumb()` gate.

## Other options considered (not applied)

- **Bump the per-step timeout** (e.g. from `2.minutes` to `4.minutes`). The
  paused task is paused *indefinitely* (never auto-resumes within 120 s),
  so a larger budget would only delay the failure rather than fix it.

- **Reflectively call `CoroutineSuspender.resume()` on the stuck task**
  after N seconds of dumb mode. Workable but masks the platform bug and
  requires reflection on private internals.

- **Disable the originator service in tests**. Plausible candidates are
  the VCS heavy-operation chain (`vcs.heavyOperationSuspender`,
  `HeavyAwareListener`); needs targeted experimentation. Not pursued
  because we don't yet know the originator.

- **Report upstream**. A YouTrack with the `checkPaused` stack, the
  "EDT idle, task paused 120 s after `git checkout`" observation, and the
  thread-dump evidence in this file is still worth filing as a parallel
  track - the mitigation above keeps our CI green but the platform bug
  remains.

## How to reproduce / re-investigate

The flake is timing-dependent and may not reproduce locally. To investigate
the next failure:

1. Pull the gradle output (`uiTest_2025.3.5` / `uiTest_262.6228.19` task) -
   usually attached to the failed CircleCI job as `0.txt` or similar.
2. Look for the `myWaitForIndicators` log pattern. With the current logging
   the line will tell you immediately whether it is dumb mode, a hung
   progress task, or the smart-quiescence timer.
3. From the same job's CircleCI artifacts, pull:
   - `out/ide-tests/tests/IU-.../ui-test/log/idea.log`
   - `out/ide-tests/tests/IU-.../ui-test/log/monitoring-thread-dumps-ide/threadDump-N-...txt`
     (the one whose timestamp falls between the start of the hang and the
     2-minute timeout fire).
   Artifact list URL pattern:
   `https://circleci.com/api/v2/project/gh/VirtusLab/git-machete-intellij-plugin/<job-id>/artifacts`
   Each `url` in the response needs `curl -L` (follows the S3 redirect).
4. In `idea.log`, search for `enter dumb mode` followed by *no* matching
   `exit dumb mode` until project disposal - that's the hang. Cross-check
   that the same `MyDumbModeTask@<hash>` does NOT have a `Task finished`
   log until ~2 minutes later.
5. The "smoking gun" stack frame to look for in the thread dump:
   any `BlockingCoroutine` parked at `CoroutineSuspenderImpl.checkPaused`,
   inside `PushedFilePropertiesUpdaterImpl.performDelayedPushTasks`.
   If that stack is present, the diagnosis is confirmed without further work.
