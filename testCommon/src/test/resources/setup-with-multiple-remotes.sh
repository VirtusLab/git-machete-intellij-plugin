#!/usr/bin/env bash

set -e -o pipefail -u

self_dir=$(cd "$(dirname "$0")" &>/dev/null; pwd -P)
source "$self_dir"/common.sh

newrepo $1 machete-sandbox-remote1 --bare
newrepo $1 machete-sandbox-remote2 --bare
newrepo $1 machete-sandbox

gituserdata

git remote add origin $1/machete-sandbox-remote1
git remote add remote-repo $1/machete-sandbox-remote2

newb root
  cmt Root
newb develop
  cmt Develop commit
newb allow-ownership-link
  cmt Allow ownership links
  push origin
newb build-chain
  cmt Build arbitrarily long chains
git checkout allow-ownership-link
  cmt 1st round of fixes
git checkout develop
  cmt Other develop commit
  push origin
newb call-ws
  cmt Call web service
  cmt 1st round of fixes
  push origin
  git branch --unset-upstream  # let's remove tracking config
newb drop-constraint # not added to definition file
  cmt Drop unneeded SQL constraints
git checkout call-ws
  cmt 2nd round of fixes

git checkout root
newb master
  cmt Master commit
  push remote-repo
newb hotfix/add-trigger
  cmt HOTFIX Add the trigger
  push remote-repo
  git branch --unset-upstream  # let's remove tracking config
  git commit --amend -m 'HOTFIX Add the trigger (amended)'

cat >.git/machete <<EOF
develop
    allow-ownership-link PR #123
        build-chain PR #124
    call-ws
master
    hotfix/add-trigger
EOF

git branch -d root
