# Development

## Table of contents

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
<!-- To install doctoc run `npm install -g doctoc`, to use it run `doctoc <this-file-path>` -->

- [Prerequisites](#prerequisites)
  - [Optional](#optional)
- [Set up git config/hooks](#set-up-git-confighooks)
- [Build](#build)
- [Run & debug](#run--debug)
- [Run UI tests](#run-ui-tests)
- [Check dependency updates](#check-dependency-updates)
- [Generate plugin zip](#generate-plugin-zip)
- [Install snapshot build of the plugin from CI](#install-snapshot-build-of-the-plugin-from-ci)
- [Logging](#logging)
- [Coding conventions](#coding-conventions)
- [Rebuild the CI base image](#rebuild-the-ci-base-image)
- [Versioning](#versioning)
  - [Sample sequence of versions between releases](#sample-sequence-of-versions-between-releases)
  - [IDE supported versions](#ide-supported-versions)
- [PRs & releases](#prs--releases)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Prerequisites

* git
* latest IntelliJ Community Edition/Ultimate

  * Install [Lombok plugin](https://plugins.jetbrains.com/plugin/6317-lombok/)
  * Enable annotation processing (for Lombok):
    `File > Settings > Build, Execution, Deployment > Compiler > Annotation Processors > Enable Annotation Processing`
  * Set Project SDK to JDK 11: `Project Structure > Project`

Consider increasing maximum heap size for the IDE (the default value is 2048 MB) under `Help > Change Memory Settings`.

For running `./gradlew` from the command line, make sure that `java` and `javac` are in `PATH` and point to Java 11.

For running tests of `backendImpl` (which are also run by `./gradlew test` task, which is in turn itself run by `./gradlew build` task),
install [`git-machete` CLI](https://github.com/VirtusLab/git-machete#install) (preferably via `pip install git-machete==<version>`)
in any of the versions listed in [backendImpl/src/test/resources/reference-cli-version.properties](../backendImpl/src/test/resources/reference-cli-version.properties).

### Optional

[Grammar-Kit IntelliJ plugin](https://plugins.jetbrains.com/plugin/6606-grammar-kit) can be used instead of Gradle plugin
to manually generate grammar and lexer code from `.bnf` and `.flex` files.

When running IntelliJ instance with a plugin loaded then [PsiViewer IntelliJ plugin](https://plugins.jetbrains.com/plugin/227-psiviewer)
can be helpful to see parsing result on the `machete` file.


## Set up git config/hooks

From the main project folder, run the following commands:

```
git config --local include.path ../.gitconfig
ln -s ../../scripts/git-hooks/machete-pre-rebase .git/hooks/machete-pre-rebase
ln -s ../../scripts/git-hooks/machete-status-branch .git/hooks/machete-status-branch
ln -s ../../scripts/git-hooks/post-commit .git/hooks/post-commit
ln -s ../../scripts/run-pre-build-checks .git/hooks/pre-commit
```


## Build

To build the project, run `./gradlew build`.

Currently, very generous maximum heap size options are applied for Gradle's Java compilation tasks (search for `-Xmx` in [build.gradle](../build.gradle)). <br/>
To overwrite them, use `compileJavaJvmArgs` Gradle project property
(e.g. `./gradlew -PcompileJavaJvmArgs='-Xmx2g -XX:+HeapDumpOnOutOfMemoryError' build`,
or equivalently with an env var: `ORG_GRADLE_PROJECT_compileJavaJvmArgs='-Xmx2g -XX:+HeapDumpOnOutOfMemoryError' ./gradlew build`).

By default, Lombok's annotation processor runs on the fly and Delomboked sources are not saved to <subproject>/build/delombok/...<br/>
To enable Delombok, set `useDelombok` Gradle project property (e.g. `./gradlew -PuseDelombok build`).

Local (non-CI) builds by default skip most of [Checker Framework's](https://checkerframework.org/manual/) checkers to speed up Java compilation.<br/>
To make local builds more aligned with CI builds (at the expense of ~2x longer compilation from scratch),
set `runAllCheckers` Gradle project property (e.g. `./gradlew -PrunAllCheckers build`).

In case of spurious cache-related issues with Gradle build, try one of the following:
* `./gradlew --stop` to shut down gradle daemon
* `./gradlew clean` and re-run the failing `./gradlew` command with `--no-build-cache`
* remove .gradle/ directory in the project directory
* remove ~/.gradle/caches/ (or even the entire ~/.gradle/) directory


## Run & debug

To run an instance of IDE with Git Machete IntelliJ Plugin installed from the current source,
execute `:runIde` Gradle task (`Gradle panel > Tasks > intellij > runIde` or `./gradlew runIde`).

To watch the logs of this IntelliJ instance, run `tail -f build/idea-sandbox/system/log/idea.log`.


## Run UI tests

```
./scripts/run-ui-tests [<intellij-version>]
```

See [Gradle Intellij plugin docs](https://github.com/JetBrains/gradle-intellij-plugin/tree/master/examples/ui-test-example)
for more details.


## Check dependency updates

`./gradlew dependencyUpdates`


## Generate plugin zip

To generate a plugin archive run `:buildPlugin` Gradle task (`Gradle panel > Tasks > intellij > buildPlugin` or `./gradlew buildPlugin`).<br/>
The resulting file will be available under `build/distributions/`.


## Install snapshot build of the plugin from CI

Download the zip file from the artifacts of the given build in [CircleCI](https://app.circleci.com/pipelines/github/VirtusLab/git-machete-intellij-plugin). <br/>
Go to `File > Settings > Plugins > (gear icon) > Install Plugin from Disk...`, select the zip and restart IDE.


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

The standard practice in Java logging is to use fully-qualified name of each class as a category;
in our case, however, we're only ever using the categories provided above.
FQCN (and method name), however, is always a part of the log message itself.

By default, IntelliJ logs everything with level `INFO` and above into `idea.log` file. <br/>
The exact location depends on a specific IntelliJ installation; check `Help > Show Log in Files` to find out. <br/>
Tip: use `tail -f` to watch the log file as it grows.

To enable logging in `DEBUG` level, add selected categories to list in `Help > Diagnostic Tools > Debug Log Settings`. <br/>
A relatively small amount of `TRACE`-level logs is generated as well.


## Coding conventions

Most non-standard/project-specific conventions are enforced by:

* [pre-commit hook](../scripts/run-pre-build-checks)
* [Spotless](https://github.com/diffplug/spotless/tree/master/plugin-gradle) for Java code formatting (see [Eclipse-compatible config](../config/spotless/formatting-rules.xml))
* [Checkstyle](https://checkstyle.sourceforge.io/) for code style/detecting basic smells (see [top-level config](../config/checkstyle/checkstyle.xml))
* [Checker Framework](https://checkerframework.org/manual/) for formal correctness, esp. wrt. null safety and UI thread handling
  (most config in [build.gradle](../build.gradle), stubs in [config/checker/](../config/checker))

Other coding conventions include:

* Don't write nullary lambdas in `receiver::method` notation, use explicit `() -> receiver.method()` notation instead. <br/>
  `::` notation is confusing when applied to parameterless lambdas, as it suggests a unary lambda.
* Use `get...` method names for pure methods that only return the value without doing any heavy workload like accessing git repository. <br/>
  Use `derive...` method names for methods that actually compute their result and/or can return a different value every time when accessed.
* Non-obvious method params that have values like `false`, `true`, `0`, `1`, `null`, `""` should be preceded with a `/* comment */ `
  containing the name of the param.
* Avoid running code outside of IDE-managed threads.
  Use either UI thread (for lightweight operations) or `Task.Backgroundable` (for heavyweight operations).


## Rebuild the CI base image

To push the rebuilt image, you need write access to [`gitmachete` organization on Docker Hub](https://hub.docker.com/orgs/gitmachete).

```
docker build -t gitmachete/intellij-plugin-ci:<SEMANTIC-VERSION> .
docker push gitmachete/intellij-plugin-ci:<SEMANTIC-VERSION>
```


## Versioning

We follow [Semantic versioning](https://semver.org/) for the plugin releases:

* MAJOR version must be bumped for each plugin release that stops supporting any IDEA build (typically when `sinceBuild` is increased). <br/>
  This does not apply to 0->1 major version transition, which is going to happen when the plugin's compatibility range is considered stable.
* MINOR version must be bumped for each plugin release that either adds a new user-facing feature
  or starts supporting a new quarterly (`year.number`) IDEA build (typically when `untilBuild` is increased).
* PATCH version must be bumped for each plugin release that adds no new user-facing features
  and doesn't change the range of supported IDEA builds.

### Sample sequence of versions between releases

After a release e.g. `1.0.3`, subsequent PRs merged to `develop` might change `PROSPECTIVE_RELEASE_VERSION`
in [version.gradle](../version.gradle) in the following way:
1. `1.0.4` (bugfix PR)  - the first PR merged to develop after the release must bump `PROSPECTIVE_RELEASE_VERSION` since of course the prospective release won't be `1.0.3` anymore
1. `1.0.4` (bugfix PR)  - even if a new set of patch-level changes has been added on the PR, the released version is still going to be `1.0.4` (not `1.0.5`)
1. `1.1.0` (feature PR) - since we've just added a new feature, the new release won't be a PATCH-level anymore, but MINOR-level one
1. `1.1.0` (bugfix PR)  - even if a new feature has been added on the PR, the released version is still going to be `1.1.0` (not `1.2.0`)
1. `2.0.0` (breaking change PR)
1. `2.0.0` (feature PR) - again, still `2.0.0` and not e.g. `2.1.0`
1. `2.0.0` (bugfix PR)
1. `2.0.0` (release PR) - finally releasing as a major release; as a consequence, `1.0.4` and `1.1.0` never actually gets released

### IDE supported versions

Since we cannot skip `untilBuild` field in a plugin build configuration
(see related [issue](https://github.com/VirtusLab/git-machete-intellij-plugin/issues/460)
and [YouTrack ticket](https://youtrack.jetbrains.com/issue/IJSDK-888)),
the most reasonable approach is to bump `untilBuild` to `X.*` when the new `X` EAP or RC version is released.
Once stable (non-EAP/RC) `X` is released, we should verify ASAP that our plugin is compatible with `X`.
There is a rather little risk that the plugin which is compatible with `X - 1` and does **not** use any `X EAP/RC`-specific API turns out to be **not** compatible with stable `X` release of IDE.

For instance:
1. our plugin in version `0.6.0` is compatible with IntelliJ `2020.2`
2. then IntelliJ `2020.3-EAP` is released
3. we check if `0.6.0` is compatible with IntelliJ `2020.3-EAP` (if not we must refactor it)
4. we extend `untilBuild` in our plugin to `2020.3.*` and release it as `0.7.0`
5. once stable `2020.3` is released, we verify ASAP that `0.7.0` is binary compatible with `2020.3` as well

## PRs & releases

Each regular (non-hotfix, non-release, non-backport) PR is ultimately merged to `develop`. <br/>
Stacked PRs (Y -> X -> `develop`) must never be merged until their base is finally changed to `develop`.
They must instead be retargeted to its base's base once their base branch is merged itself (Y -> X -> `develop` => X gets merged => Y -> `develop`).

To create a release:
* create a branch `release/v<version>` out of the current develop
* fill up [CHANGE-NOTES.html](../CHANGE-NOTES.html) file with the updated change notes:
    * for major/minor release - wipe existing file content and replace with a new one
    * for patch release - append to the existing change notes
      (although if a significant amount of time passed from the latest minor/major release, wiping out the existing notes is preferred as well)
* commit the changes with the `Release v<version>` message
* open PR from `release/v<version>` to `master`

Once the release PR is merged, `master` is built. <br/>
After manual approval, the `master` build:
* pushes a tag (`v<version>`) back to the repository
* opens a backport PR from `backport/v<version>` branch (created on the fly from `master`) to `develop`
* **publishes the plugin to JetBrains marketplace**

Backport PRs are recognized by the `backport/*` branch name.
They must have `develop` as its base.

Hotfix PRs are PRs to `master` but NOT from `develop` commit.
They always introduce a non-linear history on `develop` since after a hotfix PR is merged,
a backport PR from hotfixed `master` to `develop` is opened, and it cannot be fast-forward merged.
The alternative that would preserve linear history is to rebase the `develop` history
since the latest release over the hotfixed `master`.
This would mean, however, that the commits referenced from PRs previously merged to `develop` will no longer be part of `develop`'s history,
which is rather unacceptable.
