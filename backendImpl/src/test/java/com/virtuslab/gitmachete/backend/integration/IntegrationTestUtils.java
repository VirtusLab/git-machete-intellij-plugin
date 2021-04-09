package com.virtuslab.gitmachete.backend.integration;

import static com.virtuslab.gitmachete.testcommon.TestProcessUtils.runProcessAndReturnStdout;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import lombok.SneakyThrows;
import lombok.val;
import org.junit.Assert;

class IntegrationTestUtils {

  @SneakyThrows
  static void ensureExpectedCliVersion() {
    Properties prop = new Properties();
    try (val inputStream = IntegrationTestUtils.class.getResourceAsStream("/reference-cli-version.properties")) {
      prop.load(inputStream);
    }
    List<String> referenceCliVersions = Arrays.asList(prop.getProperty("referenceCliVersions").split(","));

    String version = runProcessAndReturnStdout(/* timeoutSeconds */ 5,
        /* command */ "git", "machete", "--version")
            .trim()
            .replace("git-machete version ", "");
    if (!referenceCliVersions.contains(version)) {
      Assert.fail("git-machete is expected in one of versions ${referenceCliVersions}, found ${version}");
    }
  }
}
