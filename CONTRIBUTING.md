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

Other coding conventions include:

* Don't write nullary lambdas in `receiver::method` notation, use explicit `() -> receiver.method()` notation instead. <br/>
  `::` notation is confusing when applied to parameterless lambdas, as it suggests a unary lambda.


## Check dependency updates

`./gradlew dependencyUpdates`


## Rebuild the CI base image

To push the rebuilt image, you need a write access to [`gitmachete` organization on Docker Hub](https://hub.docker.com/orgs/gitmachete).

```
DOCKER_BUILDKIT=1 BUILDKIT_PROGRESS=plain docker build -t gitmachete/intellij-plugin-ci .
docker push gitmachete/intellij-plugin-ci
```


## Versioning

We follow [Semantic versioning](semver.org):

* MAJOR version must be bumped for each plugin release that stops supporting any IDEA build (typically when `sinceBuild` is increased). <br/>
  This does not apply to 0->1 major version transition, which is going to happen when the plugin's compatibility range is considered stable.
* MINOR version must be bumped for each plugin release that either adds a new user-facing feature
  or starts supporting a new quarterly (`year.number`) IDEA build (typically when `untilBuild` is increased).
* PATCH version must be bumped for each plugin release that adds no new user-facing features
  and doesn't change the range of supported IDEA builds.
* Pre-release version (`d` in `a.b.c-d`) must be bumped for each PR merged to `develop` (see PRs & releases below). <br/>
  Released plugin versions must **never** have a pre-release version indicator (must be of the form `a.b.c`). <br/>
  Non-released plugin versions must **always** have a pre-release version indicator.

### Sample sequence of versions between releases

After a release e.g. `1.0.3`, we might have the following sequence of versions:
* `1.0.4-1` - note that pre-release identifiers start with one, not zero
* `1.0.4-2`
* `1.0.4-3`
* `1.1.0-1` - coz we've just adding new feature, the new release won't be a PATCH-level anymore, but MINOR-level one
* `1.1.0-2`
* `1.1.0` - finally releasing as minor release; as a consequence, `1.0.4` never actually gets released


## PRs & releases

Each PR must bump the version (see [version.gradle](version.gradle)) comparing to its base.

Each regular (non-hotfix, non-release) PR is ultimately merged to `develop` and must have a non-empty pre-release version. <br/>
Stacked PRs (Y -> X -> `develop`) are never merged until their base is finally changed to `develop`.
They must instead be retargeted to its base's base once their base branch is merged itself (Y -> X -> `develop` => X gets merged => Y -> `develop`).

Each release PR (from `develop` to `master`) must not have a pre-release version.
Once the release PR is merged, `master` is built. <br/>
**TODO (#261): ** After a manual approval, the `master` build publishes the plugin to JetBrains marketplace.

TBD: flow for hotfix PRs (PRs to `master` but NOT from `develop`).
They are likely going to require us to allow either of:
* non-linear history on `master`
  (during a release, `develop` gets merged to the hotfixed `master` instead of `master` getting FFed to match `develop`) OR
* rebasing the `develop` history since the latest release over the hotfixed `master`
  (so that `develop` commit remains a descendant of hotfixed `master`, and thus `master` can be FFed to match `develop` during a release)
