// TODO (#2194): this is inlined from package com.intellij.driver.sdk + slightly modified
//  to debug why waiting on indicators stalls every now and then

package com.virtuslab.gitmachete.uitest

import com.intellij.driver.client.Driver
import com.intellij.driver.client.service
import com.intellij.driver.sdk.*
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

fun Driver.getProgressIndicators(project: Project): List<StatusBar.TaskInfoPair> {
  return withContext {
    val ideFrame = service<WindowManager>().getIdeFrame(project)
    val statusBar = ideFrame?.getStatusBar() ?: return@withContext emptyList()
    val processes = statusBar.getBackgroundProcessModels()
    if (processes.isNotEmpty()) {
      println("Driver.getProgressIndicators: background processes = $processes")
    }
    processes
  }
}

fun Driver.areIndicatorsVisible(project: Project): Boolean {
  if (service<DumbService>(project).isDumb()) return true

  return getProgressIndicators(project).isNotEmpty()
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
    if (areIndicatorsVisible(project)) {
      smartLongEnoughStart = null
      println("Driver.myWaitForIndicators: waiting more since indicators in the project are visible, or not enough time passed without indicators")
      return@waitFor false
    }

    if (waitSmartLongEnough) {
      val start = smartLongEnoughStart
      if (start == null) {
        smartLongEnoughStart = Instant.now()
      } else {
        val now = Instant.now()
        if (start.plusSeconds(10).isBefore(now)) {
          return@waitFor true // we are smart long enough
        }
      }
      false
    } else {
      true
    }
  }
}
