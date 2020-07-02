#!/usr/bin/env bash

set -e -o pipefail -u

self_dir=$(cd "$(dirname "$0")" &>/dev/null; pwd -P)
source "$self_dir"/common.sh

create_repo machete-sandbox
(
  cd machete-sandbox
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

  git branch -d root

  machete_file='
  develop
      allow-ownership-link PR #123
          build-chain PR #124
      call-ws
          drop-constraint
  '
  sed 's/^  //' <<< "$machete_file" > .git/machete
)
