package com.virtuslab.gitmachete.uitest

import org.junit.{AfterClass, BeforeClass}
import org.virtuslab.ideprobe.RunningIntelliJPerSuiteBase

import scala.collection.Seq

trait RunningIntelliJPerSuite extends RunningIntelliJPerSuiteBase {
  @BeforeClass override final def setup(): Unit = super.setup()

  private val ignoredErrorMessages = Seq(
    // Spurious errors in the IDEA itself (probably some race conditions)
    "com.intellij.diagnostic.PluginException: Cannot create class com.intellij.uast.UastMetaLanguage"
  )

  @AfterClass override final def teardown(): Unit = {
    try {
      super.teardown()
    } catch {
      case e: Exception =>
        val filteredSuppressed = e.getSuppressed.filterNot { s =>
          ignoredErrorMessages.exists(s.getMessage.contains)
        }
        if (filteredSuppressed.nonEmpty) {
          // Following the approach taken by org.virtuslab.ideprobe.reporting.AfterTestChecks.apply
          val e = new Exception("Test failed due to postcondition failures")
          filteredSuppressed.foreach(e.addSuppressed)
          throw e
        }
    }
  }
}
