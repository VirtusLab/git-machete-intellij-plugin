import com.virtuslab.gitmachete.buildsrc.*
import org.gradle.kotlin.dsl.support.serviceOf

plugins {
  `java-library`
  `java-test-fixtures`
}

dependencies {
  betterStrings("testFixtures")
  commonsIO("testFixtures")
  junitApi("testFixtures")
  lombok("testFixtures")
}

// Pre-runs every test-fixture setup script once into `build/test-fixture-repos/<scriptName>.tar`,
// so that downstream tests can `tar -xf` it into the per-test temp dir instead of paying the ~3s
// cost of re-running the script for every `@BeforeEach`. The output is wired onto consumer Test
// tasks via the `testFixtures.prebuiltTemplatesDir` system property (see root build.gradle.kts).
//
// Why tar instead of plain directories? Gradle's output snapshotting inherits Ant's default
// excludes (notably `**/.git/**`), which silently strip the per-fixture `.git` dir when the
// output is restored from the build cache - breaking every test that reads the prebuilt repo.
// A single tarball per script is opaque to the snapshotter and round-trips through the cache
// without losing any files.
//
// Inputs are limited to `*.sh` under the test-fixtures resources; the task is up-to-date and
// build-cacheable as long as none of those scripts changes.
val fixtureResourcesDir = file("src/testFixtures/resources")
val testRepoTemplatesDir = layout.buildDirectory.dir("test-fixture-repos")

tasks.register("prepareTestRepoTemplates") {
  description = "Pre-builds git sandbox repos for tests by running each *.sh fixture once."
  group = "build"

  inputs.files(fileTree(fixtureResourcesDir) { include("*.sh") }).withPathSensitivity(PathSensitivity.RELATIVE)
  outputs.dir(testRepoTemplatesDir)
  outputs.cacheIf { true }

  val execOps = serviceOf<org.gradle.process.ExecOperations>()
  val fsOps = serviceOf<org.gradle.api.file.FileSystemOperations>()

  doLast {
    val outDir = testRepoTemplatesDir.get().asFile
    fsOps.delete { delete(outDir) }
    outDir.mkdirs()

    val commonSh = fixtureResourcesDir.resolve("common.sh")
    val scripts = fixtureResourcesDir.listFiles { f -> f.isFile && f.name.endsWith(".sh") && f.name != "common.sh" }
      ?: emptyArray()

    for (script in scripts) {
      val workDir = outDir.resolve(script.name + ".work")
      workDir.mkdirs()

      // common.sh and the script itself need to live next to where the fixture data is written,
      // so that `source "$self_dir"/common.sh` resolves and the script can `cd $sandboxDir` inside
      // the workdir.
      commonSh.copyTo(workDir.resolve("common.sh"), overwrite = true)
      script.copyTo(workDir.resolve(script.name), overwrite = true)

      execOps.exec {
        workingDir(workDir)
        commandLine("bash", script.name)
      }

      // Strip the bootstrap scripts before archiving - tests don't need them, and keeping them
      // out makes the per-test extraction step a no-op for tooling that filters them.
      workDir.resolve("common.sh").delete()
      workDir.resolve(script.name).delete()

      // BSD/GNU `tar` is available everywhere we run (macOS + Linux CI + Windows 10+). The `-C`
      // flag keeps paths relative to the workdir so the archive untars cleanly into any target.
      val tarball = outDir.resolve(script.name + ".tar")
      execOps.exec {
        workingDir(outDir)
        commandLine("tar", "-cf", tarball.absolutePath, "-C", workDir.absolutePath, ".")
      }

      fsOps.delete { delete(workDir) }
    }
  }
}
