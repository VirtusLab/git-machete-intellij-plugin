import java.io.File

// buildSrc is a separate settings build: root pluginManagement does not apply here (gradle#29610).

rootProject.name = "buildSrc"

pluginManagement {
  repositories {
    mavenLocal()
    val mavenProxyUrl = run {
      val f = File(settingsDir.parentFile, "gradle-local.properties")
      if (!f.isFile) {
        return@run null
      }
      java.util.Properties().apply { f.reader().use { load(it) } }
        .getProperty("mavenProxyUrl")
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
    }
    if (mavenProxyUrl != null) {
      maven(url = uri(mavenProxyUrl))
    }
    if (providers.gradleProperty("includePublicMavenReposForPlugins").orNull != "false") {
      gradlePluginPortal()
      if (mavenProxyUrl == null) {
        mavenCentral()
      }
    }
  }
}
