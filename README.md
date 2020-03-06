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

To run an instance of IDE with Git Machete IntelliJ Plugin execute `:runIde` gradle task.

### Generate plugin zip

To generate a plugin archive run `:buildPlugin` gradle task.
The resulting file will be available under `build/distributions`.

### Install snapshot build of the plugin

Download the zip file from the artifacts of the given build in CircleCI
(URL of the form `https://app.circleci.com/jobs/github/VirtusLab/git-machete-intellij-plugin/<<<BUILD_NUMBER>>>/artifacts`).
Go to `File | Settings | Plugins | (gear icon) | Install Plugin from Disk...`, select the zip and restart IDE.

### Rebuild the CI base image

```shell script
DOCKER_BUILDKIT=1 BUILDKIT_PROGRESS=plain docker build -t micpiotrowski/git-machete-intellij-plugin .
```
