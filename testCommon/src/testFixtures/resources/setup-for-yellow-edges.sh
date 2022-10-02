#!/usr/bin/env bash

set -e -o pipefail -u

self_dir=$(cd "$(dirname "$0")" &>/dev/null; pwd -P)
source "$self_dir"/common.sh

create_repo machete-sandbox-remote --bare

create_repo machete-sandbox
cd machete-sandbox
  git remote add origin ../machete-sandbox-remote

  create_branch root
    commit Root
  create_branch develop
    commit Develop commit
  create_branch allow-ownership-link # not added to the machete file
    commit Allow ownership links
    push
  create_branch build-chain
    commit Build arbitrarily long chains
  create_branch call-ws
    commit Call web service
    commit 1st round of fixes
    push
  create_branch drop-constraint-02 # not added to the machete file
    commit Drop unneeded SQL constraints
  git checkout call-ws
    commit 2nd round of fixes

  git checkout root
  create_branch master
    commit Master commit
    push
  create_branch hotfix/add-trigger
    commit HOTFIX Add the trigger
    push
    git commit --amend -m 'HOTFIX Add the trigger (amended)'
  git branch behind/hotfix/add-trigger HEAD~1

  git branch -d root

  # Let's skip allow-ownership-link on purpose so that develop and build-chain are connected with a yellow edge
  machete_file='
  develop
      build-chain
      call-ws PR #124
  master
      hotfix/add-trigger
          behind/hotfix/add-trigger
  '
  sed 's/^  //' <<< "$machete_file" > .git/machete
cd -
