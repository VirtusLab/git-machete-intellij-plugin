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
  create_branch allow-ownership-link
    commit Allow ownership links
    commit 1st round of fixes
    push
    # Let's clean up the upstream so that we can't rely on `git config` for finding out the remote branch;
    # we'll need to match the local branch to its remote tracking branch by name.
    git branch --unset-upstream
  create_branch build-chain
    commit Build arbitrarily long chains
  git checkout allow-ownership-link
    git reset --keep HEAD~1
  git checkout develop
    commit Other develop commit
    push
  create_branch call-ws
    commit Call web service
    commit 1st round of fixes
    push
  create_branch drop-constraint # not added to the machete file
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
    # Let's clean up the upstream so that we can't rely on `git config` for finding out the remote branch;
    # we'll need to match the local branch to its remote tracking branch by name.
    git branch --unset-upstream
  git branch behind/hotfix/add-trigger HEAD~1

  git branch -d root

  machete_file='
  develop
      allow-ownership-link PR #123
          build-chain
      call-ws PR #124
  master
      hotfix/add-trigger
          behind/hotfix/add-trigger
  '

  sed 's/^  //' <<< "$machete_file" > .git/machete
cd -
