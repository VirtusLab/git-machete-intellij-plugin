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

Always (so far) on `:uiTest_2025.3.5` (IntelliJ Ultimate, `IU-253.33514.17`).
Community (`:uiTest_2025.2.6.2`, `IC-...`) does not exhibit it.

The action that triggers the wait is irrelevant - it just happens to be the
first `doAndAwait` call after a project open in whichever test runs into the
race. `testPullBranch`'s `checkoutBranch('allow-ownership-link')` is one
example; `testResetBranchToRemote`, `testFastForwardParentOfBranch` etc. are
all candidates.

## Root cause (proven from one failing run)

The IDE is wedged in **dumb mode (indexing)** for the entire 2-minute timeout,
because a single `PushedFilePropertiesUpdaterImpl$MyDumbModeTask
(reason: Push on VFS changes)` task hangs. Same task type normally completes
in 2-8 ms (multiple healthy occurrences in the same `idea.log`).

Concurrently with the hang,
**`ProvenanceDatabaseService` is creating its project-scoped database**
(`com.intellij.code.provenance.core.editor.synchronizer.service.ProvenanceDatabaseService`,
Ultimate-only). The platform's heartbeat freeze detector
(`-Dide.performance.screenshot=heartbeat`) starts taking screenshots in the
exact same second.

Strong (but not yet 100%-confirmed) hypothesis: `ProvenanceDatabaseService`
init blocks the EDT for ~60 s, so the dumb-mode task queued behind it never
completes within the budget.

To finalize the diagnosis we need a thread dump from inside the freeze (we
don't have one yet - see "What's still missing" below).

## Evidence trail

### 1. `myWaitForIndicators` log pattern during the failure

In the gradle output (`uiTest_2025.3.5` task), only the dumb-mode branch of
`indicatorState` keeps firing for ~140 iterations. With the OLD log line
(pre this PR) the message was misleading and conflated three states:

```text
Driver.myWaitForIndicators: waiting more since indicators in the project are visible, or not enough time passed without indicators
...repeated ~140 times for 2 minutes...
```

No `Driver.getProgressIndicators: background processes = ...` log line in
that window - which is only possible if the early return
`if (service<DumbService>(project).isDumb()) return true` in `MyIndicators.kt`
is the one being taken.

After this PR you will see one of three distinct messages instead:

```text
Driver.myWaitForIndicators: waiting more since DumbService.isDumb() == true (project is indexing)
Driver.myWaitForIndicators: waiting more since background processes are running: [...]
Driver.myWaitForIndicators: still no indicators; smart-quiescence timer running (Ns/10s)
```

### 2. `idea.log` timeline of the failing project (`testPullBranch`, project hash `72227246`)

```text
00:22:37,668  enter dumb mode  [machete-sandbox-worktree]   <- shared-indexes attach
00:22:37,757  exit  dumb mode  [machete-sandbox-worktree]   <- healthy

00:22:48,135  Progress indicator:started:Updating Git Machete status...
00:22:48,165  Progress indicator:started:File System Synchronization
00:22:48,169  enter dumb mode  [machete-sandbox-worktree]   <-- *** the stuck one ***
00:22:48,179  Running task: (dumb mode task) PushedFilePropertiesUpdaterImpl$MyDumbModeTask@19cf5d21
              (reason: Push on VFS changes)                 <-- never logs "Task finished"

00:23:12,564  CodeWithMeCleanup ...                          (24-second gap, otherwise nothing)
00:23:37,453  TakeScreenshotCommand: heartbeat/frame2        <-- heartbeat freeze screenshots
00:23:37,490  TakeScreenshotCommand: heartbeat/frame1
00:23:37,564  TakeScreenshotCommand: heartbeat/frame5
00:23:37,616  TakeScreenshotCommand: heartbeat/frame6
00:23:37,626  ProvenanceDatabaseService - Creating ProvenanceDatabase at:
              .../system/projects/machete-sandbox-worktree.72227246/provenance

!!! TOTAL LOG SILENCE FOR 60 SECONDS !!!

00:24:37,617  TakeScreenshotCommand: heartbeat (next 60s heartbeat)
00:24:37,831  TakeScreenshotCommand: heartbeat/frame6
00:24:49,004  Progress indicator:started:File System Synchronization     <-- project teardown
00:24:49,041  Progress indicator:started:Closing attached shared indexes...
00:24:49,053  Dirty file ids stored. Size: 0
00:24:49,129  Project ProjectId#otnn4ksq126lojs3q6ib is disposed
```

### 3. `ProvenanceDatabase` happens in every test, ~60 s after project open

```text
11226: 00:18:48,664  Creating ProvenanceDatabase  ...machete-sandbox-worktree.faec58bf/provenance   <- Test 1 OK
11448: 00:19:52,527  Creating ProvenanceDatabase  ...machete-sandbox-worktree.c667eeaf/provenance   <- Test 2 OK
11712: 00:21:11,878  Creating ProvenanceDatabase  ...machete-sandbox-worktree.347d86a8/provenance   <- Test 3 OK
12149: 00:23:37,626  Creating ProvenanceDatabase  ...machete-sandbox-worktree.72227246/provenance   <- Test 4 (testPullBranch) -- hangs
```

Race window: in tests 1-3 the dumb-mode "Push on VFS changes" task happens to
finish before the `ProvenanceDatabase` creation begins; in test 4 they
overlap.

### 4. Why Ultimate-only?

`ProvenanceDatabaseService` lives in `intellij.code.provenance.core` which
ships only with IntelliJ Ultimate (the bundled-plugins list at startup
confirms `Code Provenance` and the `ServerPortDumpService` writes
`/tmp/<hash>-provenance-port.txt`). It is not loaded under Community.

## What's still missing

The conclusive piece would be a thread dump *from inside the freeze*:

- `threadDump-6-2026-05-07-00-23-45.txt` (10 s into the freeze)
- `threadDump-7-2026-05-07-00-24-45.txt` (3 s before the timeout)

We have only `threadDump-8-2026-05-07-00-25-45.txt` which was taken AFTER
the failed project was disposed and a new one was opened, so its EDT is
already idle and unhelpful.

If/when these are obtainable from CircleCI artifacts, the EDT stack should
show frames inside `ProvenanceDatabaseService.createProvenanceDatabase`
(probably JDBC/SQLite connection setup) - that would close the loop.

## Files involved

- `src/uiTest/kotlin/com/virtuslab/gitmachete/uitest/MyIndicators.kt`
  Inlined-from-platform copy of `Indicators.kt` with extra logging; this is
  the spot to add more diagnostics if needed. The TODO at the top references
  this issue (#2194).
- `src/uiTest/kotlin/com/virtuslab/gitmachete/uitest/BaseUITestSuite.kt`
  `doAndAwait` (line ~135) calls `myWaitForIndicators(2.minutes)` - the
  effective per-step timeout.
- `src/uiTest/kotlin/com/virtuslab/gitmachete/uitest/UITestSuite.kt`
  Each test method; `testPullBranch` is line ~165.

## Mitigation options (in order of intrusiveness)

1. **Bump the per-step timeout** in `BaseUITestSuite.doAndAwait` from
   `2.minutes` to `4.minutes` (or `5.minutes`). Cheapest; just delays the
   failure. The freeze itself is ~60 s plus a few seconds for the dumb-mode
   task to drain afterwards, so 4 min should comfortably cover it.

2. **Separate the dumb-mode wait from the progress-indicator wait**:
   call `service<DumbService>(project).waitForSmartMode()` (or its `Driver`
   equivalent) before/inside `doAndAwait`, with its own timeout, so dumb-mode
   time isn't billed against the 2-minute budget meant for "wait for
   background tasks". Makes future timeouts unambiguous.

3. **Disable the misbehaving service in tests**. Either disable the bundled
   plugin via `com.intellij.ide.starter` plugin-disabling APIs in the
   `BaseUITestSuite.startIde` setup, or pass an `-D` flag (would have to be
   confirmed against the IDE source - name unknown as of this writing).

4. **Report upstream** on YouTrack with the `idea.log` excerpt above plus
   thread dumps once obtained. This is genuinely a platform issue: a
   long-running service freezes the EDT for ~60 s on first project open.

Recommended first step: option 1 (timeout bump). It eliminates the flake
while we wait for thread-dump confirmation and an upstream fix.

## How to reproduce / re-investigate

The flake is timing-dependent and may not reproduce locally. To investigate
the next failure:

1. Pull the gradle output (`uiTest_2025.3.5` task) - usually attached to the
   failed CircleCI job as `0.txt` or similar.
2. Look for the `myWaitForIndicators` log pattern. With the new logging
   (this PR onward) the line will tell you immediately whether it is
   dumb mode, a hung progress task, or the smart-quiescence timer.
3. From the same job's CircleCI artifacts, also pull:
   - `out/ide-tests/tests/IU-253.33514.17/ui-test/log/idea.log`
   - `out/ide-tests/tests/IU-253.33514.17/ui-test/log/monitoring-thread-dumps-ide/threadDump-N-...txt`
     (the one whose timestamp falls between the start of the hang and the
     2-minute timeout fire).
4. In `idea.log`, search for `enter dumb mode` followed by *no* matching
   `exit dumb mode` until project disposal - that's the hang. Cross-check
   with `Creating ProvenanceDatabase` and 60-second silent gaps.
5. The "smoking gun" stack frame to look for in the thread dump:
   `AWT-EventQueue-0` parked/blocked inside any of:
   `ProvenanceDatabaseService.createProvenanceDatabase`,
   `c.i.c.p.c.e.s.*`, JDBC/SQLite init, or under `runWriteAction`.
