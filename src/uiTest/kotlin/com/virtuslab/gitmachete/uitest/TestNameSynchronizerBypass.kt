package com.virtuslab.gitmachete.uitest

import com.intellij.driver.client.Driver
import com.intellij.driver.sdk.waitFor
import com.intellij.ide.starter.driver.engine.BackgroundRun
import kotlin.time.Duration.Companion.minutes

// TODO (#2288): drop the entire bypass once 2026.1 support is removed
/**
 * `BackgroundRun.driver` (lazy) starts a `TestNameSynchronizer` that, on the very first listener
 * fire, calls `Driver.setActiveTestNameInTestIde(...)` -> remote `com.jetbrains.performancePlugin.TestContext`.
 *
 * That class is bundled with the `Performance Testing` plugin only since 2026.2; on 2026.1 the
 * call lands in `Invoker.getTargetClass` and surfaces as
 * `DriverIllegalStateException: No such class 'com.jetbrains.performancePlugin.TestContext'`.
 *
 * We don't actually rely on test-name reporting (we're not on TeamCity), so just resolve the
 * underlying `Driver` directly via reflection - bypassing `TestNameSynchronizer.start()` - and
 * substitute a pre-resolved [Lazy] for `BackgroundRun.driver`'s delegate, so subsequent
 * `bgRun.driver` accesses return the same instance without re-entering the original initializer.
 *
 * The reflection is fragile, but localized: it only touches `BackgroundRun.driver$delegate`
 * (Kotlin-generated field name for `by lazy`) and the `kotlin.SynchronizedLazyImpl#initializer`
 * field (stable across recent Kotlin stdlib versions).
 */
internal fun BackgroundRun.alsoBypassTestNameSynchronizer(): BackgroundRun {
  val delegateField = BackgroundRun::class.java.getDeclaredField("driver\$delegate")
    .apply { isAccessible = true }
  val lazy = delegateField.get(this) as? Lazy<*>
    ?: error("BackgroundRun#driver\$delegate is not a Lazy; ide-starter internals likely changed.")

  val initializerField = lazy.javaClass.getDeclaredField("initializer").apply { isAccessible = true }
  val initializer = checkNotNull(initializerField.get(lazy)) {
    "BackgroundRun#driver lazy was already initialized; " +
      "alsoBypassTestNameSynchronizer must be called before any access to bgRun.driver"
  }

  val capturedDriverField = initializer.javaClass.declaredFields
    .singleOrNull { Driver::class.java.isAssignableFrom(it.type) }
    ?: error(
      "Could not find a captured `Driver` field on BackgroundRun#driver's lazy initializer " +
        "(${initializer.javaClass.name}); ide-starter internals likely changed.",
    )
  capturedDriverField.isAccessible = true
  val driver = capturedDriverField.get(initializer) as? Driver
    ?: error("Captured field on BackgroundRun#driver's lazy initializer was not a Driver instance.")

  if (!driver.isConnected) {
    waitFor("Driver is connected", 3.minutes) {
      check(process.isAlive) {
        "Couldn't wait for the driver to connect, IDE process has already exited (pid=${process.id})"
      }
      driver.isConnected
    }
  }

  delegateField.set(this, lazyOf(driver))
  return this
}
