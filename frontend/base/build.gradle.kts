import com.virtuslab.gitmachete.buildsrc.*

dependencies {
  api(project(":qual"))
  api(project(":backend:api"))
}

jetbrainsAnnotations()
junit()
lombok()
slf4jLambdaApi()
vavr()

// Pull in the version constant baked at build time by the root project, so `PlatformInfoProvider`
// in the error reporter can include it in bug reports without hitting any internal IDE accessor.
sourceSets["main"].java.srcDir(rootProject.tasks.named("generatePluginVersionSource"))

// Restrict Spotless to project-local sources; the wiring above pulls in a generated source
// directory under the root project's `build/`, and Spotless's safety check rejects targets
// outside the project dir before any `targetExclude` pattern can take effect.
spotless {
  java {
    target("src/**/*.java")
  }
}
