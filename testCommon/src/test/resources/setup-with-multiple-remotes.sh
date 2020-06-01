#!/usr/bin/env bash

set -e -o pipefail -u

self_dir=$(cd "$(dirname "$0")" &>/dev/null; pwd -P)
source "$self_dir"/common.sh

newrepo machete-sandbox-remote1 --bare
newrepo machete-sandbox-remote2 --bare

newrepo machete-sandbox
(
  cd machete-sandbox
  git remote add remote1 ../machete-sandbox-remote1
  git remote add remote2 ../machete-sandbox-remote2

  newb root
    cmt Root
  newb develop
    cmt Develop commit
  newb allow-ownership-link
    cmt Allow ownership links
    push remote1
  newb build-chain
    cmt Build arbitrarily long chains
  git checkout allow-ownership-link
    cmt 1st round of fixes
  git checkout develop
    cmt Other develop commit
    push remote1
  newb call-ws
    cmt Call web service
    cmt 1st round of fixes
    push remote1
    git branch --unset-upstream  # let's remove tracking config
  newb drop-constraint # not added to definition file
    cmt Drop unneeded SQL constraints
  git checkout call-ws
    cmt 2nd round of fixes

  git checkout root
  newb master
    cmt Master commit
    push remote2
  newb hotfix/add-trigger
    cmt HOTFIX Add the trigger
    push remote2
    git branch --unset-upstream  # let's remove tracking config
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
