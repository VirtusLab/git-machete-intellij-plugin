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
* [Spotless](https://github.com/diffplug/spotless/tree/master/plugin-gradle) for Java code formatting ([config](config/spotless/formatting-rules.xml))
* [Checkstyle](https://checkstyle.sourceforge.io/) for code style/detecting basic smells ([config](config/checkstyle/checkstyle.xml))
* [Checker Framework](https://checkerframework.org/manual/) for formal correctness, esp. wrt. null safety and UI thread handling (most config in [build.gradle](build.gradle), stubs in [config/checker/](config/checker))

Other coding conventions include:

* Don't write nullary lambdas in `receiver::method` notation, use explicit `() -> receiver.method()` notation instead.
  `::` notation is confusing when applied to parameterless lambdas, it suggests a unary lambda.


## Rebuild the CI base image

To push the rebuilt image, you need write access to [`gitmachete` organization on Docker Hub](https://hub.docker.com/orgs/gitmachete).

```
DOCKER_BUILDKIT=1 BUILDKIT_PROGRESS=plain docker build -t gitmachete/intellij-plugin-ci .
docker push gitmachete/intellij-plugin-ci
```
