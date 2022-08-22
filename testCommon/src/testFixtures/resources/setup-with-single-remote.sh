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
  create_branch update-icons
    commit Use new icons
    commit Resize icons
    push
    git reset --keep HEAD~1
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
    commit HOTFIX Add the trigger - fixes
    push
    git reset --keep HEAD~1
    set_fake_git_date 2020-01-$((commit_day_of_month-2))
    commit HOTFIX Add the trigger - fixes
    # Let's clean up the upstream so that we can't rely on `git config` for finding out the remote branch;
    # we'll need to match the local branch to its remote tracking branch by name.
    git branch --unset-upstream

  git branch -d root

  machete_file='
  develop
      allow-ownership-link PR #123
          update-icons
          build-chain
      call-ws PR #124
  master
      hotfix/add-trigger
  '

  sed 's/^  //' <<< "$machete_file" > .git/machete

  # Let's specify HEAD as the revision to use; otherwise a new `machete-sandbox-worktree` branch would be created.
  git worktree add ../machete-sandbox-worktree HEAD
  # git doesn't allow for any branch to be checked out in more than one worktree at any moment.
  # Let's force-switch to detached HEAD state in the main repository folder
  # so that we have a freedom to check out any branch in the worktree.
  git checkout "$(git rev-parse HEAD)"
cd -
