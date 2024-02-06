pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenLocal()
  }
}

rootProject.name = "git-machete-intellij-plugin"
// Note: please keep the projects in a topological order
include(
  "qual",
  "testCommon",
  "gitCore:api",
  "gitCore:jGit",
  "branchLayout:api",
  "branchLayout:impl",
  "backend:api",
  "backend:impl",
  "frontend:base",
  "frontend:resourcebundles",
  "frontend:file",
  "frontend:graph:api",
  "frontend:graph:impl",
  "frontend:ui:api",
  "frontend:ui:impl",
  "frontend:actions",
)
