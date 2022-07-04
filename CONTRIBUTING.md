# Development

## Prerequisites

### IntelliJ

Use IntelliJ IDEA Community Edition/Ultimate.

1. (optional) Set up a shortcut for `Plugins` setting, you'll need to access it pretty often.
   Open `Search Actions` dialog with Ctrl+Shift+A (⌘⇧A on Mac),
   type `plugins`, press Alt+Enter (⌥↩ on Mac), then press the chosen shortcut (suggested: Ctrl+Alt+Shift+P, or ⌘⌥⇧P on Mac).

2. Make sure the following bundled plugins are enabled:
   * Git
   * Gradle
   * IntelliLang (for highlighting of language injections, e.g. JavaScript within Scala, or shell script within YAML)
   * Java Internationalization
   * JUnit
   * Lombok
   * Markdown
   * Plugin DevKit
   * Properties
   * Shell Script (also: agree to enable Shellcheck when asked)
   * TOML
   * YAML

3. (optional) If working on IntelliJ Ultimate, enable JavaScript and TypeScript plugin (for UI tests).

4. (optional) Install the following non-bundled plugins from Marketplace:
   * [Grammar-Kit IntelliJ plugin](https://plugins.jetbrains.com/plugin/6606-grammar-kit) can be used instead of Gradle plugin
     to manually generate grammar and lexer code from `.bnf` and `.flex` files.
   * [HOCON plugin](https://plugins.jetbrains.com/plugin/10481-hocon) for `.conf` file support in UI tests
   * [Kotlin plugin](https://plugins.jetbrains.com/plugin/6954-kotlin) will be useful for editing certain parts of UI, esp. dialogs.
   * [PsiViewer IntelliJ plugin](https://plugins.jetbrains.com/plugin/227-psiviewer) can be helpful to see parsing result on the `machete` file
     when running IntelliJ instance with the Git Machete plugin loaded.
   * [Scala plugin](https://plugins.jetbrains.com/plugin/1347-scala) might be useful for editing UI tests.

5. Enable annotation processing (for Lombok):
   `File > Settings > Build, Execution, Deployment > Compiler > Annotation Processors > Enable Annotation Processing`.
   Select `Obtain annotation processors from classpath` radio box.

6. Set Project SDK to JDK 11: `Project Structure > Project`

7. Consider increasing maximum heap size for the IDE (the default value is 2048 MB) under `Help > Change Memory Settings`.

8. For running `./gradlew` from the command line, make sure that `java` and `javac` are in `PATH` and point to Java 11.

9. Consider [enabling internal mode](https://plugins.jetbrains.com/docs/intellij/enabling-internal.html).
   It can be significantly useful while working with UI components (or tests).
   To investigate the UI you may want to use `Tools > Internal Actions > UI > UI Inspector`.

### Git config/hooks

From the main project folder, run the following commands:

```shell
git config --local include.path ../.gitconfig
ln -s ../../scripts/git-hooks/machete-status-branch .git/hooks/machete-status-branch
ln -s ../../scripts/git-hooks/post-commit .git/hooks/post-commit
ln -s ../../scripts/run-pre-build-checks .git/hooks/pre-commit
```

#### Windows
**The hooks do not work on Windows** (however their execution seems to be possible theoretically).
This is because one may not be emulating bash environment in any way or doing it in some specific way.

#### macOS
Some hooks use `grep`. The macOS version of `grep` (FreeBSD) differs from GNU `grep`.
In order to make `grep` and eventually the hooks working one must:
1. Install `grep` via `brew` (it will not override system's `grep` - it can be executed as `ggrep`)
2. Run `brew ls -v grep`; among the other a path like should be found `/opt/homebrew/Cellar/grep/3.7/libexec/gnubin/grep`
3. Prepend the found path without `/grep` suffix to `PATH` (`/opt/homebrew/Cellar/grep/3.7/libexec/gnubin` in that case).
You may want to add the following `PATH="/opt/homebrew/Cellar/grep/3.7/libexec/gnubin:$PATH"` to (`.zprofile`/`.zshrc`)

Also, some issues with `bash` itself has been reported. Make sure that the version you are using is 5.1 or later.

### (optional) Windows

Building this project on Windows has been tested under [Git Bash](https://gitforwindows.org/).

Additional setup:
1. Open the Registry Editor (`regedit.exe`).
2. Open path by clicking: `HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Control\FileSystem`.
3. Find key named: `LongPathsEnabled` and double click.
4. If the data value is 0, change it to 1.


## Build

To build the project, run `./gradlew build`.

Currently, very generous maximum heap size options are applied for Gradle's Java compilation tasks (search for `-Xmx` in [build.gradle](build.gradle)). <br/>
To overwrite them, use `compileJavaJvmArgs` Gradle project property
(e.g. `./gradlew -PcompileJavaJvmArgs='-Xmx2g -XX:+HeapDumpOnOutOfMemoryError' build`,
or equivalently with an env var: `ORG_GRADLE_PROJECT_compileJavaJvmArgs='-Xmx2g -XX:+HeapDumpOnOutOfMemoryError' ./gradlew build`).

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

```shell
./gradlew [-Pagainst=<e.g. 2021.2>] [-Pheadless] [-Ptests=<e.g. toggle>] uiTest
```

See [Robot plugin](https://github.com/JetBrains/intellij-ui-test-robot)
and [a preso on testing UI of IntelliJ Plugins](https://slides.com/plipski/intellij-plugin-ui-testing) for more details.


## Update version catalog

```shell
./gradlew versionCatalogUpdate
```

See [version catalog in Gradle docs](https://docs.gradle.org/current/userguide/platforms.html)
and [version catalog update plugin](https://github.com/littlerobots/version-catalog-update-plugin)
for more details.


## Generate and/or install snapshot build of the plugin

To generate a plugin archive (zip), run `:buildPlugin` Gradle task (`Gradle panel > Tasks > intellij > buildPlugin` or `./gradlew buildPlugin`).<br/>
The resulting file will be available under `build/distributions/`. <br/>
Alternatively, download the plugin zip from the artifacts of the given build
in [CircleCI](https://app.circleci.com/pipelines/github/VirtusLab/git-machete-intellij-plugin).

In either case (locally-built or CI-built), the zip can be installed via `File > Settings > Plugins > (gear icon) > Install Plugin from Disk...`.
Select the zip and restart the IDE.


## Logging

By default, IntelliJ logs everything with level `INFO` and above into `idea.log` file. <br/>
The exact location depends on a specific IntelliJ installation; check `Help > Show Log in Files` to find out. <br/>
Tip: use `tail -f` to watch the log file as it grows.

To enable logging of this plugin in `DEBUG` level, add `com.virtuslab` category to list in `Help > Diagnostic Tools > Debug Log Settings`. <br/>
A relatively small amount of `TRACE`-level logs is generated as well (`com.virtuslab:trace` to enable).


## Coding conventions

Most non-standard/project-specific conventions are enforced by:

* [pre-commit hook](scripts/run-pre-build-checks)
* [Spotless](https://github.com/diffplug/spotless/tree/master/plugin-gradle) for Java code formatting
  (see [Eclipse-compatible config](config/spotless/formatting-rules.xml))
* [Checkstyle](https://checkstyle.sourceforge.io/) for code style/detecting basic smells
  (see the [config](config/checkstyle/checkstyle.xml))
* [ArchUnit](https://www.archunit.org/userguide/html/000_Index.html) for forbidden method calls/class naming patterns etc.
  (see [tests in top-level project](src/test/java/com/virtuslab/archunit))
* [Checker Framework](https://checkerframework.org/manual/) for formal correctness, esp. wrt. null safety and UI thread handling
  (most config in [build.gradle](build.gradle), stubs in [config/checker/](config/checker))

Other coding conventions include:

* Don't write nullary lambdas in `receiver::method` notation, use explicit `() -> receiver.method()` notation instead. <br/>
  `::` notation is confusing when applied to parameterless lambdas, as it suggests a unary lambda.
* Use `get...` method names for pure methods that only return the value without doing any heavy workload like accessing git repository. <br/>
  Use `derive...` method names for methods that actually compute their result and/or can return a different value every time when accessed.
* Non-obvious method params that have values like `false`, `true`, `0`, `1`, `null`, `""` should be preceded with a `/* comment */ `
  containing the name of the param.
* Avoid running code outside IDE-managed threads.
  Use either UI thread (for lightweight operations) or `Task.Backgroundable` (for heavyweight operations).
* Properties in `GitMacheteBundle.properties` that use HTML should be wrapped in tags `<html>` ... `</html>`.
  Additionally, their keys should have a `.HTML` suffix.
* `@Tainted` and `@Untainted` annotations are used in the context of method parameters that may or may not use HTML. Those annotated with `@Untainted` should not contain HTML tags, whereas values annotated with
  `@Tainted` can contain HTML (but they don't have to).

## UI conventions

So far created UI conventions:

* Add `...`  at the end of an action name if it is not executed immediately after clicking e.g. `Sync to Parent by Rebase...` (after this operation the interactive rebase window opens)
* Toolbar name texts of a **toolbar** actions that refer to a branch should indicate the branch under action with the word `Current`.
  On the other hand, **context-menu** actions text names should be kept short (no `This`/`Selected`).

## Rebuild the CI base image

To push the rebuilt image, you need write access to [`gitmachete` organization on Docker Hub](https://hub.docker.com/orgs/gitmachete).

```shell
docker build -t gitmachete/intellij-plugin-ci:SEMANTIC-VERSION - < Dockerfile
docker push gitmachete/intellij-plugin-ci:SEMANTIC-VERSION
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
in [version.gradle](version.gradle) in the following way:
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
There is a rather little risk that the plugin which is compatible with `X - 1`
and does **not** use any `X EAP/RC`-specific API
turns out to be **not** compatible with stable `X` release of IDE.

For instance:
1. our plugin in version `0.7.0` is compatible with IntelliJ `2020.2`
2. then IntelliJ `2020.3-EAP` is released (see [snapshot repository](https://www.jetbrains.com/intellij-repository/snapshots/) -> Ctrl+F `idea`)
3. we check if `0.7.0` is compatible with IntelliJ `2020.3-EAP`:
   set `ext.intellijVersions.eapOfLatestSupportedMajor` in [build.gradle](build.gradle)
   and see if the CI pipeline passes (this will both check binary compatibility and run UI tests against the given EAP)
4. we release the plugin as `0.8.0` (`untilBuild` will extend automatically to `2020.3.*`
   via `ext.intellijVersions.latestSupportedMajor` in [build.gradle](build.gradle))
5. once the stable `2020.3` is released, we verify ASAP that `0.8.0` is binary compatible with `2020.3` as well;
   then, `latestStable` can be updated to `2020.3`, `eapOfLatestSupportedMajor` can be set to `null`,
   and `2020.2.x` can be added to `latestMinorsOfOldSupportedMajors`
6. since `latestStable` is used as the version to build against,
   a few _source_ incompatibilities might appear once `latestStable` is updated, even when the plugin was _binary_ compatible with the new IDE version.

## PRs & releases

The default branch of the repository is `master`, but each regular (non-hotfix, non-release, non-backport) PR must be merged to `develop`. <br/>
Because of that all regular PR branches should be derived from `develop` and not `master`. <br/>
Due to the fact that the default branch is not `develop`, merging of PRs does not close linked issues (you have to close the issues manually). <br/>
Stacked PRs (Y -> X -> `develop`) must never be merged until their base is finally changed to `develop`.
They must instead be retargeted to its base's base once their base branch is merged itself (Y -> X -> `develop` => X gets merged => Y -> `develop`).

To create a release:
* make sure [CHANGE-NOTES.html](CHANGE-NOTES.html) are updated
* open PR from `develop` to `master`

Once the release PR is merged, `master` is built. <br/>
After manual approval, the `master` build:
* pushes a tag (`v<version>`) back to the repository
* creates a [GitHub release](https://github.com/VirtusLab/git-machete-intellij-plugin/releases)
* **publishes the plugin to JetBrains marketplace**

Backport PRs are recognized by the `backport/*` branch name.
They must have `develop` as its base.

Hotfix PRs (`hotfix/*` branch name) are PRs to `master` but NOT from `develop` commit.
They always introduce a non-linear history on `develop` since after a hotfix PR is merged,
a backport PR from hotfixed `master` to `develop` is opened, and it cannot be fast-forward merged. <br/>
The alternative that would preserve linear history is to rebase the `develop` history
since the latest release over the hotfixed `master`.
This would mean, however, that the commits referenced from PRs previously merged to `develop` will no longer be part of `develop`'s history,
which is rather unacceptable.


## Scenario recordings

There is a test suite `com.virtuslab.gitmachete.uitest.UIScenarioSuite` that helps to re-record the scenario recordings.
The most effective way to record all scenarios (probably) is to record all scenarios and trim the video.
To record the screen you can use Quick Time Player (on macOS).
The scenario suite **is not** fully automatic.
The following steps shall be performed manually at the beginning:
- resize the tool window to ~4/5 of IDE window height (to show all toolbar actions and avoid the context-menu exceeding the window area)
- increase the font size to 16
- resize the rebase dialog (to fit inside the window area)
- sometimes an IDE internal error occurs - clear it

The suite covers only scenarios - recordings for [features](docs/features.md) must be updated manually.
Gifs can optimized with https://www.xconvert.com/ - over 50% size reduction.
