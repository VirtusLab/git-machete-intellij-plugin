# Development

## Requirements

* git
* IntelliJ 2019.3+ Community Edition/Ultimate

  * Install [Lombok plugin](https://plugins.jetbrains.com/plugin/6317-lombok/)
  * Enable annotation processing (for Lombok): `File` -> `Settings` -> `Build`, `Execution`, `Deployment` -> `Compiler` -> `Annotation Processors` -> `Enable Annotation Processing`
  * Set Project SDK to JDK 11: `Project Structure` -> `Project`

## Set up project

From the main project folder, run the following commands:

```
git config --local include.path ../.gitconfig
ln -s ../../scripts/git-hooks/machete-pre-rebase .git/hooks/machete-pre-rebase
ln -s ../../scripts/git-hooks/machete-status-branch .git/hooks/machete-status-branch
ln -s ../../scripts/git-hooks/post-commit .git/hooks/post-commit
ln -s ../../scripts/run-pre-build-checks .git/hooks/pre-commit
```
---
Consider increasing maximum heap size for the IDE (the default value is 2048 MB). Go to `Help` -> `Change Memory Settings` for this.

## Run/Debug

To run an instance of IDE with Git Machete IntelliJ Plugin execute `:runIde` Gradle task (`Gradle panel` -> `Tasks` -> `intellij` -> `runIde` or `./gradlew runIde`).

### Logging

Logging of this plugin has several categories:
* `backend`
* `backendRoot`
* `binding`
* `branchLayout`
* `frontendActions`
* `frontendUiRoot`
* `frontendUiTable`
* `gitCore`

By default, IntelliJ logs everything with level `INFO` and above into `idea.log` file.
The exact location depends on IntelliJ version and OS, check `Help` -> `Show Log in Files` to find out.

To enable logging in `DEBUG` level, add selected categories to list in `Help` -> `Diagnostic Tools` -> `Debug Log Settings`.
The standard practice in Java logging is to use fully-qualified name of each class as category;
in our case, however, we're only ever using the categories provided above.

## Generate plugin zip

To generate a plugin archive run `:buildPlugin` Gradle task (`Gradle panel` -> `Tasks` -> `intellij` -> `buildPlugin` or `./gradlew buildPlugin`).
The resulting file will be available under `build/distributions/`.

## Install snapshot build of the plugin

Download the zip file from the artifacts of the given build in [CircleCI](https://app.circleci.com/pipelines/github/VirtusLab/git-machete-intellij-plugin).
Go to `File` -> `Settings` -> `Plugins` -> `(gear icon)` -> `Install Plugin from Disk...`, select the zip and restart IDE.

## Rebuild the CI base image

To push the rebuilt image, you need write access to [`gitmachete` organization on Docker Hub](https://hub.docker.com/orgs/gitmachete).

```
DOCKER_BUILDKIT=1 BUILDKIT_PROGRESS=plain docker build -t gitmachete/intellij-plugin-ci .
docker push gitmachete/intellij-plugin-ci
```

## Coding conventions

Most non-standard/project-specific conventions are enforced by:

* [pre-commit hook](scripts/run-pre-build-checks)
* [Spotless](https://github.com/diffplug/spotless/tree/master/plugin-gradle) for Java code formatting ([config](config/spotless/formatting-rules.xml))
* [Checkstyle](https://checkstyle.sourceforge.io/) for code style/detecting basic smells ([config](config/checkstyle/checkstyle.xml))
* [Checker Framework](https://checkerframework.org/manual/) for formal correctness, esp. wrt. null safety and UI thread handling (most config in [build.gradle](build.gradle), stubs in [config/checker/](config/checker))

Other coding conventions include:

* Don't write nullary lambdas in `receiver::method` notation, use explicit `() -> receiver.method()` notation instead.
  `::` notation is confusing when applied to parameterless lambdas, it suggests a unary lambda.
