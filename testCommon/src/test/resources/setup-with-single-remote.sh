#!/usr/bin/env bash

set -e -o pipefail -u

self_dir=$(cd "$(dirname "$0")" &>/dev/null; pwd -P)
source "$self_dir"/common.sh

newrepo machete-sandbox-remote --bare

newrepo machete-sandbox
(
  cd machete-sandbox
  git remote add origin ../machete-sandbox-remote

  newb root
    cmt Root
  newb develop
    cmt Develop commit
  newb allow-ownership-link
    cmt Allow ownership links
    push
  newb build-chain
    cmt Build arbitrarily long chains
  git checkout allow-ownership-link
    cmt 1st round of fixes
  git checkout develop
    cmt Other develop commit
    push
  newb call-ws
    cmt Call web service
    cmt 1st round of fixes
    push
  newb drop-constraint # not added to definition file
    cmt Drop unneeded SQL constraints
  git checkout call-ws
    cmt 2nd round of fixes

  git checkout root
  newb master
    cmt Master commit
    push
  newb hotfix/add-trigger
    cmt HOTFIX Add the trigger
    push
    git commit --amend -m 'HOTFIX Add the trigger (amended)'

  git branch -d root

  machete_file='
  develop
      allow-ownership-link PR #123
          build-chain PR #124
      call-ws
  master
      hotfix/add-trigger
  '

  sed 's/^  //' <<< "$machete_file" > .git/machete
)
