# UI test flake: `myWaitForIndicators` 2-minute timeout (issue #2194)

Self-contained notes for the next debugging session. Aimed at being re-consumable
by an LLM so we don't have to re-derive the same conclusions every time.

## Symptom

A spurious failure that happens every few dozen CI runs:

```text
UITest > testPullBranch() FAILED
    com.intellij.driver.sdk.WaitForException: Timeout(2m): Failed: Indicators
        at com.intellij.driver.sdk.WaitsKt.waitFor-FbhrOv8(waits.kt:130)
        ...
        at com.virtuslab.gitmachete.uitest.MyIndicatorsKt.myWaitForIndicators-exY8QGI(MyIndicators.kt:66)
        at com.virtuslab.gitmachete.uitest.BaseUITest.doAndAwait(BaseUITest.kt:138)
        at com.virtuslab.gitmachete.uitest.BaseUITest.checkoutBranch(BaseUITest.kt:205)
        at com.virtuslab.gitmachete.uitest.UITest.testPullBranch(UITest.kt:170)
```

Long-running flake: ~12 distinct CI occurrences logged on
[#2194](https://github.com/VirtusLab/git-machete-intellij-plugin/issues/2194)
between 2025-09-25 and 2026-06-01, i.e. roughly one observed failure per
month on average - plus several reproductions hit while iterating on the
mitigation itself in early June. Reproduced on `:uiTest_2025.3.5`
(IntelliJ Ultimate, `IU-253.33514.17`), `:uiTest_262.6228.19`
(`IU-262.6228.19`, upcoming-major-EAP, also Ultimate) and
`:uiTest_262.6653.22` (`IU-262.6653.22`, upcoming-major-EAP, also
Ultimate). Community (`:uiTest_2025.2.6.2`, `IC-...`) does not exhibit it.

The action that triggers the wait is irrelevant - it just happens to be
whichever `doAndAwait` call gets unlucky after a `git` operation that
re-enters dumb mode. Observed triggers so far include `checkoutBranch(...)`
(by far the most common: `allow-ownership-link`, `master`, `call-ws`),
`syncSelectedToParentByMerge('call-ws')` and `syncSelectedToParentByRebase`.
The failing test is similarly varied: `testPullBranch`, `testSquashBranch`,
`testFastForwardParentOfBranch`, `testSyncToParentByRebaseAction`,
`testSkipNonExistentBranches_toggleListingCommits_slideOutRoot`.

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

### Second occurrence (2026-06-01 run, IU-262.6653.22, CircleCI job 14771)

Same root cause, **different visible symptom**: the paused `MyDumbModeTask`
this time DOES contribute a status-bar progress indicator, so the previous
"drop the `isDumb()` gate" mitigation no longer hides the hang. The wait
now spins for 2 minutes printing

```text
Driver.myWaitForIndicators: waiting more since background processes are running:
   [(com.intellij.openapi.progress.impl.PlatformTaskSupportKt$taskInfo$1@7ec76143,
     com.intellij.openapi.progress.ProgressIndicatorModel@3734a19)]
```

with the same `@7ec76143` `TaskInfo` instance for all ~120 polling iterations.

`idea.log` correlation (project hash `d3d300a2`):

```text
17:30:28,110  Progress indicator:started:Checking out call-ws...
17:30:28,145  git checkout call-ws --
17:30:28,152  Switched to branch 'call-ws'
17:30:28,255  enter dumb mode  [machete-sandbox-worktree]
17:30:28,259  Running task: (dumb mode task) MyDumbModeTask@5a204ebf
              (reason: Push on VFS changes)        <-- same flavour as 2026-05-27
17:30:28,283  Progress indicator:finished:Checking out call-ws...
--- 100 s gap, only periodic heartbeat screenshots ---
17:32:13  (test gives up - 2 min wait fires)
```

Smoking-gun stack (`threadDump-2-...17-31-10.txt`, ~57 s into the freeze;
identical in `threadDump-3-...17-32-10.txt` at ~117 s in - same
`BlockingCoroutine@530b5f18`):

```text
"DefaultDispatcher-worker-1" parked in:
  PushedFilePropertiesUpdaterImpl$MyDumbModeTask.performInDumbMode  (line 377)
    runBlockingCancellable
      MergingQueueGuiExecutor.runSingleTask
        MergingQueueGuiExecutor.processTasksWithProgress
          ... (MergingQueueGuiSuspender, SingleTaskExecutor) ...
            MergingQueueGuiExecutor$startBackgroundProcess$1$1.invokeSuspend
              PlatformTaskSupport.withBackgroundProgressInternal      <-- THIS
                ProgressPipeImpl.collectProgressUpdates                |
                  BlockingCoroutine.joinBlocking                       |
                    LockSupport.parkNanos                              |
                                                                      v
                                                  ...is what surfaces as the
                                                  visible status-bar TaskInfo
                                                  @7ec76143.

"...MergingQueueGuiExecutor$ScopeHolder":BlockingCoroutine@530b5f18,
   state: SUSPENDED
   [..., TaskSuspenderElement, CoroutineSuspenderElement, ...,
    BlockingEventLoop]
    at PushedFilePropertiesUpdaterImpl$MyDumbModeTask
       $performInDumbMode$1$1.invokeSuspend(PushedFilePropertiesUpdaterImpl.kt:379)
```

Identity:
- `PlatformTaskSupport$taskInfo$1` = the `withBackgroundProgress(title, ...)`
  wrapper that `MergingQueueGuiExecutor.startBackgroundProcess` opens for
  every dumb-mode task. It stays registered until the wrapped task returns.
- The wrapped task is `MyDumbModeTask`, suspended on the same
  `TaskSuspender`/`CoroutineSuspender` chain as the 2026-05-27 occurrence.
- The EDT is idle. The freeze is purely on the never-resumed suspender.

In other words, the platform's wrapper task is now what we see; previously
only the inner work was visible to `DumbService` while the wrapper stayed
hidden. Same bug, different surface.

Artifacts pulled under `/tmp/issue2194-v2/` (idea.log + threadDump-2 +
threadDump-3).

### Third occurrence (2026-06-01 run, IU-262.6653.22, CircleCI job 14776)

Failure in `testSyncToParentByRebaseAction` after `syncSelectedToParentByRebase`.
Same `MyDumbModeTask` suspender stack in the thread dump - third title for
the same upstream platform bug:

```text
"Progress: Analyzing project to enable smart features":ProducerCoroutine{Active},
  state: SUSPENDED [..., Dispatchers.Default]
    at com.intellij.openapi.progress.impl.PlatformTaskSupportKt$showIndicator$1.invokeSuspend
        (PlatformTaskSupport.kt:476)

"com.intellij.util.indexing.UnindexedFilesScannerExecutorImpl":supervisor:ChildScope
  "Scanning (root)":StandaloneCoroutine{Active}, state: SUSPENDED
    at UnindexedFilesScannerExecutorImpl$1.invokeSuspend(...:138)

[and, again, in the same dump:]
PushedFilePropertiesUpdaterImpl$MyDumbModeTask.performInDumbMode  (line 377)
  ... -> CoroutineSuspenderImpl ... SUSPENDED   <-- same upstream stall
```

What was new and important is the **logging** added in PR #2305: it printed
the offending title directly in the gradle output:

```text
Driver.myWaitForIndicators: waiting more since background processes are
  running: [Analyzing project to enable smart features#1592099291]
Driver.myWaitForIndicators: waiting more since background processes are
  running: [Analyzing project to enable smart features#1972950645]
Driver.myWaitForIndicators: waiting more since background processes are
  running: [Analyzing project to enable smart features#1105700164]
...
```

Two things this log proves at a glance:

1. The hung task title is `Analyzing project to enable smart features` -
   the wrapper started around `MyDumbModeTask` during smart-mode init.
2. The driver-sdk returns a **fresh `TaskInfo` proxy on every call**:
   `System.identityHashCode` is different on every line, even though
   logically it's the same task. The first iteration of the stall tracker
   (PR #2305) keyed on `identityHash + title`, so the stall age never
   accumulated and the 30 s threshold was never reached. The wait still
   spun for the full 2 minutes.

Fix: key the stall tracker on **title alone**, with the clock reset only
when the title disappears entirely from a poll snapshot. Identity hash is
retained in the log line for human forensics.

Artifacts pulled under `/tmp/issue2194-v3/` (idea.log + threadDump-2).

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

With the current mitigation, a hung wait that *still* trips the 2-minute
timeout means either (a) it's a different bug entirely, or (b) the stall
tracker mis-identified a healthy-but-changing task (e.g. two short tasks
with the same title that briefly alternate so the title never disappears
from a poll snapshot, yet neither one is actually stalled). In case (b)
the gradle log will show many distinct `<title>#<id>` suffixes for one
title, and the test will succeed for the wrong reason after the 30 s
mark. If that happens, consider also tracking the indicator's fraction /
text to detect "stuck" vs "merely repeated".

To dig deeper from the test process itself:

- Reflectively peek at `DumbServiceImpl.getCurrentTask()` and confirm the
  task is `PushedFilePropertiesUpdaterImpl$MyDumbModeTask`.
- Reflectively read `CoroutineSuspenderImpl.isPaused` for the suspender
  attached to that task (private; via `MergingQueueGuiSuspender` or
  `TaskSuspenderImpl`).
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
- `src/uiTest/kotlin/com/virtuslab/gitmachete/uitest/BaseUITest.kt`
  `doAndAwait` (line ~141) calls `myWaitForIndicators(2.minutes)` - the
  effective per-step timeout.
- `src/uiTest/kotlin/com/virtuslab/gitmachete/uitest/UITest.kt`
  Each test method (`testPullBranch`, `testSkipNonExistentBranches_...`,
  `testFastForwardParentOfBranch`, ...).

## Mitigation in place

Two layers in `MyIndicators.kt`:

1. **`DumbService.isDumb()` is no longer consulted.** Activity is gated on
   `StatusBar.getBackgroundProcessModels()` only. This handles the
   2026-05-27 flavour, where the paused `MyDumbModeTask` keeps `isDumb()`
   true but does not surface as a progress indicator.

2. **Per-title stall threshold (30 s).** `indicatorState` runs every visible
   `TaskInfo` through a `StallTracker` keyed on `getTitle()` alone. Any
   title that has been continuously visible across polls for >= 30 s is
   logged once and dropped from the "running" set; the moment a title
   disappears from a poll snapshot, its first-seen clock is reset. This
   handles every observed flavour of the platform stall - the
   `MergingQueueGuiExecutor.withBackgroundProgress(...)` wrapper title in
   `IU-262.6653.22 / testSquashBranch` and the
   `Analyzing project to enable smart features` wrapper title in
   `IU-262.6653.22 / testSyncToParentByRebaseAction`.

   The earlier identity-hash-based variant (`System.identityHashCode +
   title`, PR #2305) was defeated by the second of those occurrences: the
   driver-sdk does not cache the `TaskInfo` proxy across remote calls, so
   the identity hash churns on every poll even when the underlying server
   task is the same. Logs from a real failure showed dozens of distinct
   `#<id>` suffixes for one logical task.

Why 30 s is safe in practice:

- Healthy occurrences of the offending task wrapper finish in 2-60 ms (we
  have 13+ datapoints from the pre-freeze portion of the same `idea.log`s).
- The longest legitimate visible tasks in these tests are initial project
  scanning ("Updating Git Machete status...", "Closing attached shared
  indexes...", VCS log refresh) which complete well under 10 s in CI.
- The smart-quiescence step still requires a 10 s no-indicator window after
  the stall guard fires, so an erroneously aged-out task that immediately
  reappears will keep the wait honest as long as it churns.
- The git/VCS/UI actions exercised by these tests do not depend on PSI
  indexes being ready, so we don't need smart-mode protection.

The new logging also prints the task's title (via `TaskInfo.getTitle()`)
instead of the opaque `PlatformTaskSupportKt$taskInfo$1@<hash>` proxy
`toString()`. The next occurrence's gradle output should tell us at a glance
*which* task is hung, without needing a thread dump.

Side effect: if a future regression introduces a legitimate task that needs
to keep running for more than 30 s while a test waits for it, the wait will
prematurely succeed. None of the current actions look like they would
suffer; if a future test does, prefer adding a dedicated
`Driver.waitForSmartMode(timeout)` or per-call `waitFor("MyCondition") {}`
at that site rather than raising the global stall threshold.

## Other options considered (not applied)

- **Bump the per-step timeout** (e.g. from `2.minutes` to `4.minutes`). The
  paused task is paused *indefinitely* (never auto-resumes within 120 s),
  so a larger budget would only delay the failure rather than fix it.

- **Reflectively call `CoroutineSuspender.resume()` on the stuck task**
  after N seconds of dumb mode. Workable but masks the platform bug and
  requires reflection on private internals. The current stall-tracker
  approach is the lighter equivalent: rather than resuming the suspender,
  we just stop *waiting* for the wrapper indicator once it has clearly
  hung. The platform task remains paused but is harmless.

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

1. Pull the gradle output (`uiTest_2025.3.5` / `uiTest_262.6228.19` /
   `uiTest_262.6653.22` / ... task) - usually attached to the failed
   CircleCI job as `0.txt` or similar.
2. Look for the `myWaitForIndicators` log pattern. With the current logging
   the line will tell you immediately:
   - which indicator was hung (`<title>#<identityHash>`),
   - whether the stall guard already aged it out
     (`ignoring stale indicator ... visible for Ns without completing`),
   - or whether the smart-quiescence timer was running.
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
5. The "smoking gun" stack frames to look for in the thread dump (any of
   these three equivalent forms; they all map to the same upstream bug):
   - `BlockingCoroutine` parked at `CoroutineSuspenderImpl.checkPaused`,
     inside `PushedFilePropertiesUpdaterImpl.performDelayedPushTasks`
     (2026-05-27 / IU-262.6228.19 form), OR
   - `MergingQueueGuiExecutor$ScopeHolder:BlockingCoroutine, state:
     SUSPENDED` with both `TaskSuspenderElement` and
     `CoroutineSuspenderElement` in the coroutine context, parked at
     `MyDumbModeTask$performInDumbMode$1$1.invokeSuspend` (2026-06-01 /
     `testSquashBranch` / IU-262.6653.22 form), OR
   - `Progress: Analyzing project to enable smart features:
     ProducerCoroutine, state: SUSPENDED` plus
     `UnindexedFilesScannerExecutorImpl ... scanning task execution trigger
     ... SUSPENDED` plus the same `MyDumbModeTask` frame (2026-06-01 /
     `testSyncToParentByRebaseAction` / IU-262.6653.22 form).
   Any one is enough to confirm the diagnosis without further work.
