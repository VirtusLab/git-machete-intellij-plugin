package com.virtuslab.gitmachete.frontend.ui.impl.errorreport;

class DummyPlatformInfoProvider extends PlatformInfoProvider {
  @Override
  String getOSName() {
    return "Mock OS X";
  }

  @Override
  String getOSVersion() {
    return "Hehe";
  }

  @Override
  String getIdeApplicationName() {
    return "mocked IntelliJ idea";
  }

  @Override
  String getPluginVersion() {
    return "mock plugin version";
  }
}
