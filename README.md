# Git Machete IntelliJ Plugin [![CircleCI](https://circleci.com/gh/VirtusLab/git-machete-intellij-plugin/tree/master.svg?style=shield)](https://circleci.com/gh/VirtusLab/git-machete-intellij-plugin/tree/master)

![](src/main/resources/META-INF/pluginIcon.svg)

This is a port of [git-machete](https://github.com/VirtusLab/git-machete) into an IntelliJ plugin.

This plugin is available on [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/14221-git-machete).


## Logging

SLF4J logging in this plugin has the following categories:

```
binding
branchlayout
gitcore
gitmachete.backend
gitmachete.frontend.actions
gitmachete.frontend.ui
```

The standard practice in Java logging is to use fully-qualified name of each class as category;
in our case, however, we're only ever using the categories provided above.

By default, IntelliJ logs everything with level `INFO` and above into `idea.log` file. <br/>
The exact location depends on IntelliJ version and OS, check `Help` -> `Show Log in Files` to find out. <br/>
Tip: use `tail -f` to watch the log file as it grows.

To enable logging in `DEBUG` level, add selected categories to list in `Help` -> `Diagnostic Tools` -> `Debug Log Settings`. <br/>


## Development

### Prerequisites

* git
* IntelliJ 2019.3+ Community Edition/Ultimate

  * Install [Lombok plugin](https://plugins.jetbrains.com/plugin/6317-lombok/)
  * Enable annotation processing (for Lombok):
    `File` -> `Settings` -> `Build`, `Execution`, `Deployment` -> `Compiler` -> `Annotation Processors` -> `Enable Annotation Processing`
  * Set Project SDK to JDK 11: `Project Structure` -> `Project`

Consider increasing maximum heap size for the IDE (the default value is 2048 MB) under `Help` -> `Change Memory Settings`.

For running `./gradlew` from command line, make sure that `java` and `javac` are in `PATH` and point to Java 11.


### Build

To build the project, run `./gradlew build`.

Currently, very generous maximum heap size options are applied for Gradle's Java compilation tasks (search for `-Xmx` in [build.gradle](build.gradle)). <br/>
To overwrite them, use `GRADLE_COMPILE_JAVA_JVM_ARGS` environment variable (e.g. `GRADLE_COMPILE_JAVA_JVM_ARGS=-Xmx2g ./gradlew build`).


### Run & debug

To run an instance of IDE with Git Machete IntelliJ Plugin installed from the current source,
execute `:runIde` Gradle task (`Gradle panel` -> `Tasks` -> `intellij` -> `runIde` or `./gradlew runIde`).

To watch the logs of this IntelliJ instance, run `tail -f build/idea-sandbox/system/log/idea.log`.


### Run UI tests

This repo requires artifacts of [ide-probe](https://github.com/VirtuslabRnD/ide-probe) to be available in the local Maven repository
to perform non-headless UI tests of the plugin. <br/>
Unfortunately, as for now we can't make the repository public and/or publish those artifacts to JCenter/Bintray due to VirtusLab's client's policy. <br>
Kudos for [@lukaszwawrzyk](https://github.com/lukaszwawrzyk) for making ide-probe available for use in this project.

To run UI tests locally, first:
* install [sbt](https://www.scala-sbt.org/download.html)
* clone [ide-probe](https://github.com/VirtuslabRnD/ide-probe)
* inside ide-probe project folder, checkout `master` as of 2020-05-14: `git checkout 674acde5e17d9479733dd55faf880ea162248e37`
* execute `sbt '; api/publishM2; probePlugin/publishM2; driver/publishM2; junitDriver/publishM2'`; <br>
  this should publish the artifacts to a local Maven repo under `~/.m2/repository`.

UI tests are by default excluded from `./gradlew test` unless `enableUiTests` property is set:

```
./gradlew -PenableUiTests --info test
```

Note that the first execution might take a couple of minutes since IntelliJ zips need to be downloaded (~500MB each). <br/>
Use `ls -thor /tmp` to monitor the progress while they're downloading.


### Generate plugin zip

To generate a plugin archive run `:buildPlugin` Gradle task (`Gradle panel` -> `Tasks` -> `intellij` -> `buildPlugin` or `./gradlew buildPlugin`).
The resulting file will be available under `build/distributions/`.


### Install snapshot build of the plugin from CI

Download the zip file from the artifacts of the given build in [CircleCI](https://app.circleci.com/pipelines/github/VirtusLab/git-machete-intellij-plugin). <br/>
Go to `File` -> `Settings` -> `Plugins` -> `(gear icon)` -> `Install Plugin from Disk...`, select the zip and restart IDE.


### Contributing

For more details on contributing to the project, see the [guideline](CONTRIBUTING.md).
