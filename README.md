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

```shell script
git config --local include.path ../.gitconfig
ln -s ../../scripts/git-hooks/machete-pre-rebase .git/hooks/machete-pre-rebase
ln -s ../../scripts/git-hooks/machete-status-branch .git/hooks/machete-status-branch
ln -s ../../scripts/git-hooks/post-commit .git/hooks/post-commit
```

### Run/Debug

To run an instance of IDE with Git Machete IntelliJ Plugin execute `:runIde` Gradle task (`Gradle panel | Tasks | intellij | runIde` or `./gradlew runIde`).

### Generate plugin zip

To generate a plugin archive run `:buildPlugin` Gradle task (`Gradle panel | Tasks | intellij | buildPlugin` or `./gradlew buildPlugin`).
The resulting file will be available under `frontend/build/distributions/`.

### Install snapshot build of the plugin

Download the zip file from the artifacts of the given build in [CircleCI](https://app.circleci.com/pipelines/github/VirtusLab/git-machete-intellij-plugin).
Go to `File | Settings | Plugins | (gear icon) | Install Plugin from Disk...`, select the zip and restart IDE.
