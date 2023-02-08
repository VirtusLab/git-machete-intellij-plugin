#!/usr/bin/env bash

set -e -o pipefail -u

self_dir=$(cd "$(dirname "$0")" &>/dev/null; pwd -P)
source "$self_dir"/common.sh

create_repo machete-sandbox
cd machete-sandbox
  # Let's add more than 10 branches so that some of them (the least recently checked out ones)
  # should be skipped from the discovered layout
  create_branch root
    commit Root
  create_branch develop
    commit Develop commit
  create_branch allow-ownership-link
    commit Allow ownership links
  create_branch build-chain
    commit Build arbitrarily long chains
  git checkout allow-ownership-link
    commit 1st round of fixes
  git checkout develop
    commit Other develop commit
  create_branch call-ws
    commit Call web service
    commit 1st round of fixes
  create_branch drop-constraint
    commit Drop unneeded SQL constraints
  git checkout call-ws
    commit 2nd round of fixes
  create_branch evict-deps
    commit Evict conflicting dependencies
  create_branch fix/component-labels
    commit Fix component labels
  create_branch global-context
    commit Introduce global context
  create_branch interop-scala-python
    commit Enable Scala/Python interoperability
  create_branch java-11-enforce
    commit Enforce the use of Java 11
  git checkout allow-ownership-link
  create_branch kill-process
    commit Kill the process after 10 seconds
  git checkout develop

  machete_file='
  develop
      allow-ownership-link PR #123
          build-chain
      call-ws PR #124
          drop-constraint
  '
  sed 's/^  //' <<< "$machete_file" > .git/machete

  # Let's remove the reflog for allow-ownership-link, so that fork point for build-chain
  # can't be determined based purely on its reflog;
  # common ancestor of allow-ownership-link and build-chain needs to be used as the fork point.
  rm -f .git/logs/refs/heads/allow-ownership-link
cd -
