package com.virtuslab.gitmachete.frontend.errorreport;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.extensions.PluginId;
import org.apache.commons.lang3.SystemUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

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
    IdeaPluginDescriptor pluginDescriptor = PluginManagerCore.getPlugin(PluginId.getId("com.virtuslab.git-machete"));
    return pluginDescriptor != null ? pluginDescriptor.getVersion() : null;
  }
}
