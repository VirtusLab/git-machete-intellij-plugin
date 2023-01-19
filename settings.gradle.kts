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
  "frontend:actions",
  "frontend:file",
  "frontend:resourcebundles",
  "frontend:graph:api",
  "frontend:graph:impl",
  "frontend:ui:api",
  "frontend:ui:impl"
)
