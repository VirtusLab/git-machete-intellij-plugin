package com.virtuslab.gitmachete.buildsrc

class IntellijVersionHelper {
  static String getFromBuildNumber(String buildNumber) {
    return '20' + buildNumber.substring(0, 2) + '.' + buildNumber.substring(2, 3)
  }

  static String toBuildNumber(String version) {
    return version.substring(2, 6).replace('.', '')
  }

  static String getMajorPart(String version) {
    return version.substring(0, 6)
  }

  static Properties getProperties() {
    Properties properties = new Properties()
    properties.load(getFile().newDataInputStream())
    return properties
  }

  static void storeProperties(Properties properties, String comment = null) {
    properties.store(getFile().newWriter(), comment);
  }

  private static File getFile() {
    return new File("intellijVersions.properties")
  }
}
