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
