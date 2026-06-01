#!/usr/bin/env bash

set -e -o pipefail -u

self_dir=$(cd "$(dirname "$0")" &>/dev/null; pwd -P)
source "$self_dir"/common.sh

create_repo machete-sandbox
cd machete-sandbox
  create_branch master
    commit Master commit

  create_branch develop
    commit Develop commit
  create_branch feature-a
    commit Feature A commit

  git checkout develop
  create_branch feature-b
    commit Feature B commit

  git checkout master

  machete_file='
  master
  develop
      feature-a
      feature-b
  '
  sed 's/^  //' <<< "$machete_file" > .git/machete

  # NB: this script intentionally does NOT do `git worktree add ...`.
  # The matching `TestGitRepository#applyPostScriptSetup` runs that step in Java once the (possibly
  # pre-built and copied) template is in its final location - otherwise the absolute paths baked
  # into `.git/worktrees/<wt>/gitdir` and `<wt-root>/.git` would point at the pre-build directory
  # rather than the per-test temp dir.
cd -
