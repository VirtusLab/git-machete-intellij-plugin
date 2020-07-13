# Contributing

## Set up git config/hooks

From the main project folder, run the following commands:

```
git config --local include.path ../.gitconfig
ln -s ../../scripts/git-hooks/machete-pre-rebase .git/hooks/machete-pre-rebase
ln -s ../../scripts/git-hooks/machete-status-branch .git/hooks/machete-status-branch
ln -s ../../scripts/git-hooks/post-commit .git/hooks/post-commit
ln -s ../../scripts/run-pre-build-checks .git/hooks/pre-commit
```


## Coding conventions

Most non-standard/project-specific conventions are enforced by:

* [pre-commit hook](scripts/run-pre-build-checks)
* [Spotless](https://github.com/diffplug/spotless/tree/master/plugin-gradle) for Java code formatting (see [Eclipse-compatible config](config/spotless/formatting-rules.xml))
* [Checkstyle](https://checkstyle.sourceforge.io/) for code style/detecting basic smells (see [top-level config](config/checkstyle/checkstyle.xml))
* [Checker Framework](https://checkerframework.org/manual/) for formal correctness, esp. wrt. null safety and UI thread handling
  (most config in [build.gradle](build.gradle), stubs in [config/checker/](config/checker))

Note that certain Checker Framework's checkers (`Index`, `Optional`, `Regex`) are by default only run in the CI to speed up local compilation. <br/>
Pass `RUN_ALL_CHECKERS=true` env var to Gradle to enable all of them locally.

Other coding conventions include:

* Don't write nullary lambdas in `receiver::method` notation, use explicit `() -> receiver.method()` notation instead. <br/>
  `::` notation is confusing when applied to parameterless lambdas, as it suggests a unary lambda.
* Use `get...` method names for pure methods that only return the value without doing any heavy workload like accessing git repository.
  Use `derive...` method names for methods that actually compute their result and/or can return a different value every time when accessed.
* Non-obvious method params that have values like `false`, `true`, `0`, `1`, `null`, `""` should be preceded with a `/* comment */ `
  containing the name of the param.


## Check dependency updates

`./gradlew dependencyUpdates`


## Rebuild the CI base image

To push the rebuilt image, you need a write access to [`gitmachete` organization on Docker Hub](https://hub.docker.com/orgs/gitmachete).

```
DOCKER_BUILDKIT=1 BUILDKIT_PROGRESS=plain docker build -t gitmachete/intellij-plugin-ci .
docker push gitmachete/intellij-plugin-ci
```


## Versioning

We follow [Semantic versioning](semver.org) for the plugin releases:

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
1. `2.0.0` (release PR) - finally releasing as major release; as a consequence, `1.0.4` and `1.1.0` never actually gets released


## PRs & releases

Each regular (non-hotfix, non-release, non-backport) PR is ultimately merged to `develop`. <br/>
Stacked PRs (Y -> X -> `develop`) must never be merged until their base is finally changed to `develop`.
They must instead be retargeted to its base's base once their base branch is merged itself (Y -> X -> `develop` => X gets merged => Y -> `develop`).

To create a release:
* create a branch `release/v<version>` out of the current develop
* fill up `<change-notes>` in [plugin.xml](src/main/resources/META-INF/plugin.xml) with the updated change notes
  and commit the changes with the `Release v<version>` message
* open PR from `release/v<version>` to `master`

Once the release PR is merged, `master` is built. <br/>
After a manual approval, the `master` build:
* pushes a tag (`v<version>`) back to the repository
* opens a backport PR from `backport/v<version>` branch (created on the fly from `master`) to `develop`
* **publishes the plugin to JetBrains marketplace**

Backport PRs are recognized by the `backport/*` branch name.
They must have `develop` as its base.

TBD: flow for hotfix PRs (PRs to `master` but NOT from `develop`) and their corresponding backport PRs.
They are likely going to require us to allow either of:
* non-linear history on `master`
  (during a release, `develop` gets merged to the hotfixed `master` instead of `master` getting FFed to match `develop`) OR
* rebasing the `develop` history since the latest release over the hotfixed `master`
  (so that `develop` commit remains a descendant of hotfixed `master`, and thus `master` can be FFed to match `develop` during a release)
