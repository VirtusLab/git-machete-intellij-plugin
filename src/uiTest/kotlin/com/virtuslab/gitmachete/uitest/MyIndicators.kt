// Inlined and slightly adapted from com.intellij.driver.sdk's Indicators.kt:
//   - extra logging while waiting (helps diagnose stalls; see #2194),
//   - activity is detected via status-bar progress indicators only;
//     DumbService.isDumb() is intentionally not consulted (see comment in
//     indicatorState below).

package com.virtuslab.gitmachete.uitest

import com.intellij.driver.client.Driver
import com.intellij.driver.client.service
import com.intellij.driver.sdk.*
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

fun Driver.getProgressIndicators(project: Project): List<Pair<TaskInfo?, ProgressModel?>> {
  return withContext {
    val ideFrame = service<WindowManager>().getIdeFrame(project)
    val statusBar = ideFrame?.getStatusBar() ?: return@withContext emptyList()
    statusBar.getBackgroundProcessModels()
  }
}

private sealed interface IndicatorState {
  data class Running(val processes: List<Pair<TaskInfo?, ProgressModel?>>) : IndicatorState
  object None : IndicatorState
}

private fun Driver.indicatorState(project: Project): IndicatorState {
  // Activity is judged solely from status-bar progress indicators.
  // DumbService.isDumb() is NOT used as a gate here: a known platform race
  // (issue #2194) can leave PushedFilePropertiesUpdaterImpl$MyDumbModeTask
  // (reason: "Push on VFS changes") paused via the TaskSuspender mechanism
  // and never resumed. The paused task contributes no progress indicator
  // but keeps isDumb() true indefinitely, which would stall this wait until
  // the per-step timeout. Every legitimate long-running scanning/indexing
  // task surfaces as a progress indicator, and the git/VCS/UI actions
  // exercised by these tests do not depend on PSI indexes being ready, so
  // the indicator list is a sufficient signal.
  val processes = getProgressIndicators(project)
  return if (processes.isEmpty()) IndicatorState.None else IndicatorState.Running(processes)
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

  waitFor("Indicators", timeout) {
    val project = runCatching { projectGet.invoke() }.getOrNull()
    if (project == null) {
      smartLongEnoughStart = null
      println("Driver.myWaitForIndicators: waiting more since project is null")
      return@waitFor false
    }
    if (!isProjectOpened(project)) {
      smartLongEnoughStart = null
      println("Driver.myWaitForIndicators: waiting more since project ($project) is not opened")
      return@waitFor false
    }
    when (val state = indicatorState(project)) {
      is IndicatorState.Running -> {
        smartLongEnoughStart = null
        println("Driver.myWaitForIndicators: waiting more since background processes are running: ${state.processes}")
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
