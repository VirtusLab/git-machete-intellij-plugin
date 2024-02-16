#!/usr/bin/env bash

set -e -o pipefail -u

self_dir=$(cd "$(dirname "$0")" &>/dev/null; pwd -P)
source "$self_dir"/common.sh

create_repo machete-sandbox-remote --bare

create_repo machete-sandbox
cd machete-sandbox
  git remote add origin ../machete-sandbox-remote

  create_branch develop
    commit Develop commit
  create_branch allow-ownership-link
    commit Allow ownership links
    commit 1st round of fixes
  # Let's simulate a squash-merge that would be performed by GitHub...
  # actually using the `git machete squash`'s secret technique under the hood:
  squashed_commit_hash=$(git commit-tree "HEAD^{tree}" -p develop -m 'Allow ownership links - squashed 2 commits')
  git update-ref refs/heads/develop "$squashed_commit_hash"

  create_branch master
    commit Master commit
    push
cd -

# Let's now push a commit to develop from another repo.
# See https://github.com/VirtusLab/git-machete/issues/1166 and its corresponding PR.
create_repo other-local-repo
cd other-local-repo
  git remote add origin ../machete-sandbox-remote
  git fetch
  git checkout master
    commit Other master commit
  create_branch build-chain
    commit Build arbitrarily long chains
    push
  git checkout master
    commit Yet another master commit
    push
cd -

cd machete-sandbox
  git fetch
  git checkout master
  git reset --keep origin/master
  git checkout build-chain

  machete_file='
  develop
      allow-ownership-link PR #123
      build-chain
  '

  sed 's/^  //' <<< "$machete_file" > .git/machete
cd -
