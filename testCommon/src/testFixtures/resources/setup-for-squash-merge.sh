#!/usr/bin/env bash

set -e -o pipefail -u

self_dir=$(cd "$(dirname "$0")" &>/dev/null; pwd -P)
source "$self_dir"/common.sh

create_repo machete-sandbox
cd machete-sandbox
  create_branch develop
    commit Develop commit
  create_branch allow-ownership-link
    commit Allow ownership links
    commit 1st round of fixes
  # Let's simulate a squash-merge that would be performed by GitHub...
  # actually using the `git machete squash`'s secret technique under the hood:
  squashed_commit_hash=$(git commit-tree "HEAD^{tree}" -p develop -m 'Allow ownership links - squashed 2 commits')
  git update-ref refs/heads/develop "$squashed_commit_hash"

  machete_file='
  develop
      allow-ownership-link PR #123
  '

  sed 's/^  //' <<< "$machete_file" > .git/machete
cd -
