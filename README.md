# Git Machete IntelliJ Plugin

[![CircleCI](https://circleci.com/gh/VirtusLab/git-machete-intellij-plugin/tree/master.svg?style=shield)](https://circleci.com/gh/VirtusLab/git-machete-intellij-plugin/tree/master)
[![JetBrains Plugins](https://img.shields.io/jetbrains/plugin/v/14221-git-machete.svg)](https://plugins.jetbrains.com/plugin/14221-git-machete)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/14221-git-machete.svg)](https://plugins.jetbrains.com/plugin/14221-git-machete)

![](src/main/resources/META-INF/pluginIcon.svg)

This is a port of [git-machete](https://github.com/VirtusLab/git-machete#git-machete) into an IntelliJ plugin.

This plugin is available on [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/14221-git-machete).

![](docs/demo-workflow.gif)


## Logging

SLF4J logging in this plugin has the following categories:

```
binding
branchlayout
gitcore
gitmachete.backend
gitmachete.frontend.actions
gitmachete.frontend.externalsystem
gitmachete.frontend.graph
gitmachete.frontend.ui
```

The standard practice in Java logging is to use fully-qualified name of each class as category;
in our case, however, we're only ever using the categories provided above.
FQCN (and method name), however, is always a part of the log message itself.

By default, IntelliJ logs everything with level `INFO` and above into `idea.log` file. <br/>
The exact location depends on a specific IntelliJ installation; check `Help` -> `Show Log in Files` to find out. <br/>
Tip: use `tail -f` to watch the log file as it grows.

To enable logging in `DEBUG` level, add selected categories to list in `Help` -> `Diagnostic Tools` -> `Debug Log Settings`. <br/>
A relatively small amount of `TRACE`-level logs is generated as well.


## Development

### Prerequisites

* git
* IntelliJ 2020.1 Community Edition/Ultimate

  * Install [Lombok plugin](https://plugins.jetbrains.com/plugin/6317-lombok/)
  * Enable annotation processing (for Lombok):
    `File` -> `Settings` -> `Build`, `Execution`, `Deployment` -> `Compiler` -> `Annotation Processors` -> `Enable Annotation Processing`
  * Set Project SDK to JDK 11: `Project Structure` -> `Project`

Consider increasing maximum heap size for the IDE (the default value is 2048 MB) under `Help` -> `Change Memory Settings`.

For running `./gradlew` from command line, make sure that `java` and `javac` are in `PATH` and point to Java 11.

For running tests of `backendImpl` (which are included in `./gradlew test`, which is in turn itself included in `./gradlew build`),
install [`git-machete` CLI](https://github.com/VirtusLab/git-machete#install) in version **2.14.0**.

#### Optional

[Grammar-Kit IntelliJ plugin](https://plugins.jetbrains.com/plugin/6606-grammar-kit) can be used instead of Gradle plugin
to manually generate grammar and lexer code from `.bnf` and `.flex` files.

When running IntelliJ instance with a plugin loaded then [PsiViewer IntelliJ plugin](https://plugins.jetbrains.com/plugin/227-psiviewer)
can be helpful to see parsing result on machete file.


### Build

To build the project, run `./gradlew build`.

Currently, very generous maximum heap size options are applied for Gradle's Java compilation tasks (search for `-Xmx` in [build.gradle](build.gradle)). <br/>
To overwrite them, use `GRADLE_COMPILE_JAVA_JVM_ARGS` environment variable (e.g. `GRADLE_COMPILE_JAVA_JVM_ARGS=-Xmx2g ./gradlew build`).

By default, Lombok's annotation processor runs on the fly and Delomboked sources are not saved to {subproject}/build/delombok/...<br/>
To enable Delombok, set `GRADLE_COMPILE_JAVA_DELOMBOK` environment variable to `true` (e.g. `GRADLE_COMPILE_JAVA_DELOMBOK=true ./gradlew build`).

Local (non-CI) builds by default skip most of [Checker Framework's](https://checkerframework.org/manual/) checkers to speed up Java compilation.<br/>
To make local builds more aligned with CI builds (at the expense of ~2x slower compilation from scratch), set `RUN_ALL_CHECKERS` environment variable to `true`.

In case of spurious cache-related issues with Gradle build, try one of the following:
* `./gradlew clean` and re-run the failing `./gradlew` command with `--no-build-cache`
* remove .gradle/ directory in the project directory
* remove ~/.gradle/caches/ (or even the entire ~/.gradle/) directory


### Run & debug

To run an instance of IDE with Git Machete IntelliJ Plugin installed from the current source,
execute `:runIde` Gradle task (`Gradle panel` -> `Tasks` -> `intellij` -> `runIde` or `./gradlew runIde`).

To watch the logs of this IntelliJ instance, run `tail -f build/idea-sandbox/system/log/idea.log`.


### Run UI tests

```
./scripts/run-ui-tests [<intellij-version>]
```

See [Gradle Intellij plugin docs](https://github.com/JetBrains/gradle-intellij-plugin/tree/master/examples/ui-test-example)
for more details.


### Generate plugin zip

To generate a plugin archive run `:buildPlugin` Gradle task (`Gradle panel` -> `Tasks` -> `intellij` -> `buildPlugin` or `./gradlew buildPlugin`).<br/>
The resulting file will be available under `build/distributions/`.


### Install snapshot build of the plugin from CI

Download the zip file from the artifacts of the given build in [CircleCI](https://app.circleci.com/pipelines/github/VirtusLab/git-machete-intellij-plugin). <br/>
Go to `File` -> `Settings` -> `Plugins` -> `(gear icon)` -> `Install Plugin from Disk...`, select the zip and restart IDE.


### Contributing

For more details on contributing to the project, see the [guideline](CONTRIBUTING.md).
