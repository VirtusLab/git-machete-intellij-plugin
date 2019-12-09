# Git Machete IntelliJ Plugin
[![CircleCI](https://circleci.com/gh/VirtusLab/git-machete-intellij-plugin/tree/master.svg?style=svg)](https://circleci.com/gh/VirtusLab/git-machete-intellij-plugin/tree/master)

This is a port of [git-machete](https://github.com/VirtusLab/git-machete) into IntelliJ plugin.

## Development

### Requirements
1. IntelliJ 2019.1+ Community Edition/Ultimate 
2. [Lombok plugin](https://plugins.jetbrains.com/plugin/6317-lombok/)
3. Enabled annotation processing (for Lombok): `File | Settings | Build, Execution, Deployment | Compiler | Annotation Processors | Enable Annotation Processing`
4. git

### Run/Debug

To run an instance of IDE with Git Machete IntelliJ Plugin execute 
`intellijPlugin:runIde` gradle task.
