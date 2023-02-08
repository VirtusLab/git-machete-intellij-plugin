#!/usr/bin/env bash

set -e -o pipefail -u

self_dir=$(cd "$(dirname "$0")" &>/dev/null; pwd -P)
source "$self_dir"/common.sh

create_repo machete-sandbox-remote1 --bare
create_repo machete-sandbox-remote2 --bare

create_repo machete-sandbox
cd machete-sandbox
  git remote add remote1 ../machete-sandbox-remote1
  git remote add remote2 ../machete-sandbox-remote2

  create_branch root
    commit Root
  create_branch develop
    commit Develop commit
  create_branch allow-ownership-link
    commit Allow ownership links
    push remote1
  create_branch build-chain
    commit Build arbitrarily long chains
  git checkout allow-ownership-link
    commit 1st round of fixes
  git checkout develop
    commit Other develop commit
    push remote1
  create_branch call-ws
    commit Call web service
    commit 1st round of fixes
    push remote1
    git branch --unset-upstream  # let's remove tracking config
  create_branch drop-constraint  # not added to the machete file
    commit Drop unneeded SQL constraints
  git checkout call-ws
    commit 2nd round of fixes

  git checkout root
  create_branch master
    commit Master commit
    push remote2
  create_branch hotfix/add-trigger
    commit HOTFIX Add the trigger
    push remote2
    git branch --unset-upstream  # let's remove tracking config
    git commit --amend -m 'HOTFIX Add the trigger (amended)'

  git branch -d root

  machete_file='
  develop
      allow-ownership-link PR #123
          build-chain
      call-ws PR #124
  master
      hotfix/add-trigger
  '
  sed 's/^  //' <<< "$machete_file" > .git/machete

  # Let's remove all reflogs, so that a fork point for *NO* branch
  # can be determined based purely on reflogs of other branches;
  # common ancestor of each branch and its parent needs to be used as the fork point.
  rm -rf .git/logs/
cd -
