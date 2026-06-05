package com.virtuslab.gitmachete.frontend.errorreport;

import com.intellij.openapi.application.ApplicationInfo;
import org.apache.commons.lang3.SystemUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.BuildInfo;

class PlatformInfoProvider {
  @Nullable
  String getOSName() {
    return SystemUtils.OS_NAME;
  }

  @Nullable
  String getOSVersion() {
    return SystemUtils.OS_VERSION;
  }

  String getIdeApplicationName() {
    return ApplicationInfo.getInstance().getFullApplicationName();
  }

  @Nullable
  String getPluginVersion() {
    // Baked in at build time by the `generatePluginVersionSource` Gradle task,
    // so we don't need to query the IDE's plugin manager (all relevant accessors
    // there are flagged internal by IntelliJ compatibility verifier).
    return BuildInfo.PLUGIN_VERSION;
  }
}
