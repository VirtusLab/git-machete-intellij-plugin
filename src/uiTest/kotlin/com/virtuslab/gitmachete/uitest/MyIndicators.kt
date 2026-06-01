// Inlined and slightly adapted from com.intellij.driver.sdk's Indicators.kt:
//   - extra logging while waiting (the offending indicator's title is printed
//     so freezes can be diagnosed straight from the gradle output; see #2194),
//   - activity is detected via status-bar progress indicators only;
//     DumbService.isDumb() is intentionally not consulted,
//   - any single indicator that stays visible for STALL_THRESHOLD without
//     ever disappearing is treated as a platform stall and ignored
//     (the same #2194 race - paused TaskSuspender that never resumes -
//     re-surfaced in IU-262.6653.22 as a never-completing background task).

package com.virtuslab.gitmachete.uitest

import com.intellij.driver.client.Driver
import com.intellij.driver.client.service
import com.intellij.driver.sdk.*
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private val STALL_THRESHOLD = 30.seconds

fun Driver.getProgressIndicators(project: Project): List<Pair<TaskInfo?, ProgressModel?>> {
  return withContext {
    val ideFrame = service<WindowManager>().getIdeFrame(project)
    val statusBar = ideFrame?.getStatusBar() ?: return@withContext emptyList()
    statusBar.getBackgroundProcessModels()
  }
}

// Driver-side snapshot of one status-bar background task, taken once per
// polling iteration to avoid repeated remote calls into the IDE for the same
// data within a single wait check (every getTitle() hop crosses the
// driver <-> IDE boundary).
//
// The driver-sdk proxy wrapping the remote TaskInfo reports a stable
// System.identityHashCode across polls within a single wait (verified in CI
// logs - see #2194), so identity hash plus title is enough to recognise "the
// same task again" even if proxy identity ever changes between versions.
private data class IndicatorSnapshot(val identityHash: Int, val title: String) {
  val identityKey: String get() = "$identityHash|$title"
  val displayLabel: String get() = "$title#$identityHash"
}

private fun Pair<TaskInfo?, ProgressModel?>.snapshot(): IndicatorSnapshot {
  val task = first
  return IndicatorSnapshot(
    identityHash = task?.let { System.identityHashCode(it) } ?: 0,
    title = task?.getTitle() ?: "?",
  )
}

private sealed interface IndicatorState {
  data class Running(val processes: List<IndicatorSnapshot>) : IndicatorState
  object None : IndicatorState
}

private class StallTracker {
  private val firstSeen = HashMap<String, Instant>()

  // Returns the subset of `snapshots` that have not yet exceeded STALL_THRESHOLD.
  // Side-effect: bookkeeps first-seen timestamps and evicts entries no longer present.
  fun freshOnly(snapshots: List<IndicatorSnapshot>, now: Instant): List<IndicatorSnapshot> {
    firstSeen.keys.retainAll(snapshots.mapTo(HashSet()) { it.identityKey })
    val fresh = mutableListOf<IndicatorSnapshot>()
    for (s in snapshots) {
      val firstSeenAt = firstSeen.getOrPut(s.identityKey) { now }
      val ageSecs = java.time.Duration.between(firstSeenAt, now).seconds
      if (ageSecs >= STALL_THRESHOLD.inWholeSeconds) {
        println(
          "Driver.myWaitForIndicators: ignoring stale indicator ${s.displayLabel} " +
            "(visible for ${ageSecs}s without completing - suspected platform stall, see #2194)",
        )
      } else {
        fresh += s
      }
    }
    return fresh
  }

  fun clear() = firstSeen.clear()
}

// Activity is judged solely from status-bar progress indicators.
// DumbService.isDumb() is NOT used as a gate here: a known platform race
// (issue #2194) can leave PushedFilePropertiesUpdaterImpl$MyDumbModeTask
// (reason: "Push on VFS changes") paused via the TaskSuspender mechanism
// and never resumed. The paused task keeps isDumb() true indefinitely,
// which would stall this wait until the per-step timeout. Every legitimate
// long-running scanning/indexing task surfaces as a progress indicator, and
// the git/VCS/UI actions exercised by these tests do not depend on PSI
// indexes being ready, so the indicator list is a sufficient signal.
//
// The same race can also surface as a visible background task that never
// completes (observed on IU-262.6653.22: the platform's wrapper
// MergingQueueGuiExecutor.startBackgroundProcess registers a
// withBackgroundProgress(...) for the dumb-mode task, and that wrapper
// stays visible for the full 2-minute timeout while the inner suspender
// is paused). The stall tracker filters such never-completing indicators.
private fun Driver.indicatorState(project: Project, tracker: StallTracker): IndicatorState {
  val snapshots = getProgressIndicators(project).map { it.snapshot() }
  val fresh = tracker.freshOnly(snapshots, Instant.now())
  return if (fresh.isEmpty()) IndicatorState.None else IndicatorState.Running(fresh)
}

/**
 * !!! ATTENTION !!!
 *
 * The only guarantee you have after this method is that reference to Project won't be null.
 * The UI, Project View, services and everything else might not be yet initialized.
 * Use [myWaitForIndicators] instead if you're not 100% sure otherwise you might get flaky test.
 * Also, you can avoid calling this method before [myWaitForIndicators] since it also waits for an open project.
 */
fun Driver.waitForProjectOpen(timeout: Duration = 1.minutes) {
  waitFor("Project is opened", timeout) {
    isProjectOpened()
  }
}

/**
 * Method waits till a project is opened and there are no indicators for 10 seconds.
 */
fun Driver.myWaitForIndicators(project: Project, timeout: Duration, waitSmartLongEnough: Boolean = true) {
  myWaitForIndicators({ project }, timeout, waitSmartLongEnough = waitSmartLongEnough)
}

/**
 * Method waits till a project is opened and there are no indicators for 10 seconds.
 */
fun Driver.myWaitForIndicators(timeout: Duration, waitSmartLongEnough: Boolean = true) {
  waitForProjectOpen(timeout)
  myWaitForIndicators(::singleProject, timeout, waitSmartLongEnough = waitSmartLongEnough)
}

/**
 * Method waits till a project is opened and there are no indicators for 10 seconds.
 */
internal fun Driver.myWaitForIndicators(projectGet: () -> Project?, timeout: Duration, waitSmartLongEnough: Boolean = true) {
  var smartLongEnoughStart: Instant? = null
  // Per-wait stall tracker: ages out indicators that never disappear.
  // Scoped to a single myWaitForIndicators call so that a slow-but-progressing
  // task doesn't get pre-aged by tracking from earlier waits.
  val stallTracker = StallTracker()

  waitFor("Indicators", timeout) {
    val project = runCatching { projectGet.invoke() }.getOrNull()
    if (project == null) {
      smartLongEnoughStart = null
      stallTracker.clear()
      println("Driver.myWaitForIndicators: waiting more since project is null")
      return@waitFor false
    }
    if (!isProjectOpened(project)) {
      smartLongEnoughStart = null
      stallTracker.clear()
      println("Driver.myWaitForIndicators: waiting more since project ($project) is not opened")
      return@waitFor false
    }
    when (val state = indicatorState(project, stallTracker)) {
      is IndicatorState.Running -> {
        smartLongEnoughStart = null
        val labels = state.processes.joinToString(prefix = "[", postfix = "]") { it.displayLabel }
        println("Driver.myWaitForIndicators: waiting more since background processes are running: $labels")
        return@waitFor false
      }

      IndicatorState.None -> { /* fall through to smart-quiescence check */ }
    }

    if (waitSmartLongEnough) {
      val start = smartLongEnoughStart
      if (start == null) {
        smartLongEnoughStart = Instant.now()
        println("Driver.myWaitForIndicators: no indicators visible; starting 10s smart-quiescence timer")
      } else {
        val now = Instant.now()
        if (start.plusSeconds(10).isBefore(now)) {
          return@waitFor true // we are smart long enough
        }
        println("Driver.myWaitForIndicators: still no indicators; smart-quiescence timer running (${java.time.Duration.between(start, now).toSeconds()}s/10s)")
      }
      false
    } else {
      true
    }
  }
}
