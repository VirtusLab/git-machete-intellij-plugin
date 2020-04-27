# Git Machete IntelliJ Plugin [![CircleCI](https://circleci.com/gh/VirtusLab/git-machete-intellij-plugin/tree/master.svg?style=shield&circle-token=3ba295982e665ead39e6d097bc3859d5a2e2b124)](https://circleci.com/gh/VirtusLab/git-machete-intellij-plugin/tree/master)

This is a port of [git-machete](https://github.com/VirtusLab/git-machete) into an IntelliJ plugin.

## Development

### Requirements

* git
* IntelliJ 2019.3+ Community Edition/Ultimate

  * Install [Lombok plugin](https://plugins.jetbrains.com/plugin/6317-lombok/)
  * Enable annotation processing (for Lombok): `File | Settings | Build, Execution, Deployment | Compiler | Annotation Processors | Enable Annotation Processing`
  * Set Project SDK to JDK 11: `Project Structure | Project`

### Set up project

From the main project folder, run the following commands:

```
git config --local include.path ../.gitconfig
ln -s ../../scripts/git-hooks/machete-pre-rebase .git/hooks/machete-pre-rebase
ln -s ../../scripts/git-hooks/machete-status-branch .git/hooks/machete-status-branch
ln -s ../../scripts/git-hooks/post-commit .git/hooks/post-commit
ln -s ../../scripts/run-pre-build-checks .git/hooks/pre-commit
```
---
Consider increasing maximum heap size for IDE (the default value is 2048MB). Go to `Help | Change Memory Settings` for this.

### Run/Debug

To run an instance of IDE with Git Machete IntelliJ Plugin execute `:runIde` Gradle task (`Gradle panel | Tasks | intellij | runIde` or `./gradlew runIde`).

#### Logging

Logging of this plugin has several categories:
* `backend`
* `backendRoot`
* `branchLayout`
* `frontendActions`
* `frontendGraph`
* `frontendUiRoot`
* `frontendUiTable`
* `gitCore`

By default, IntelliJ logs everything with level `INFO` and above into `idea.log` file (localization depends on IntelliJ version and OS). To find where this file is located, go to `Help` -> `Show Log in Files`.

To enable logging in `DEBUG` level, add selected categories to list in `Help` -> `Diagnostic Tools` -> `Debug Log Settings`.
For our classes, the categories are customized and provided above; by default, the category is a fully-qualified name of each class.

### Generate plugin zip

To generate a plugin archive run `:buildPlugin` Gradle task (`Gradle panel | Tasks | intellij | buildPlugin` or `./gradlew buildPlugin`).
The resulting file will be available under `build/distributions/`.

### Install snapshot build of the plugin

Download the zip file from the artifacts of the given build in [CircleCI](https://app.circleci.com/pipelines/github/VirtusLab/git-machete-intellij-plugin).
Go to `File | Settings | Plugins | (gear icon) | Install Plugin from Disk...`, select the zip and restart IDE.

### Rebuild the CI base image

To push the rebuilt image, you need write access to [`gitmachete` organization on Docker Hub](https://hub.docker.com/orgs/gitmachete).

```
DOCKER_BUILDKIT=1 BUILDKIT_PROGRESS=plain docker build -t gitmachete/intellij-plugin-ci .
docker push gitmachete/intellij-plugin-ci
```
