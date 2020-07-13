package com.virtuslab.gitmachete.backend.integration;

import java.util.concurrent.TimeUnit;

import lombok.SneakyThrows;
import org.junit.Assert;

class IntegrationTestUtils {

  @SneakyThrows
  static void ensureCliVersionIs(String cliReferenceVersion) {
    var process = new ProcessBuilder().command("git", "machete", "--version").start();
    process.waitFor(1, TimeUnit.SECONDS);
    var exitValue = process.exitValue();
    if (exitValue != 0) {
      Assert.fail("git-machete CLI is not installed");
    }
    var version = new String(process.getInputStream().readAllBytes())
        .stripTrailing()
        .replace("git-machete version ", "");
    if (!version.equals(cliReferenceVersion)) {
      Assert.fail("git-machete is expected in version ${cliReferenceVersion}, found ${version}");
    }
  }
}
