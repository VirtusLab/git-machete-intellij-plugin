# Git Machete IntelliJ Plugin [![CircleCI](https://circleci.com/gh/VirtusLab/git-machete-intellij-plugin/tree/master.svg?style=shield&circle-token=3ba295982e665ead39e6d097bc3859d5a2e2b124)](https://circleci.com/gh/VirtusLab/git-machete-intellij-plugin/tree/master)

This is a port of [git-machete](https://github.com/VirtusLab/git-machete) into IntelliJ plugin.

## Development

### Requirements
1. IntelliJ 2019.1+ Community Edition/Ultimate 
2. [Lombok plugin](https://plugins.jetbrains.com/plugin/6317-lombok/)
3. Enabled annotation processing (for Lombok): `File | Settings | Build, Execution, Deployment | Compiler | Annotation Processors | Enable Annotation Processing`
4. Set SDK 11: `Project Structure | Project`
5. git

### Run/Debug

To run an instance of IDE with Git Machete IntelliJ Plugin execute 
`intellijPlugin:runIde` gradle task.

### Generating plugin zip

To generate a plugin archive run `intellijPlugin:buildPlugin` gradle task. The resulting file will be available under `intellijPlugin/build/distributions`.