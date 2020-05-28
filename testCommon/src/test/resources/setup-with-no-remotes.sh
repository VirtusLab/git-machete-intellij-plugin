#!/usr/bin/env bash

set -e -o pipefail -u

self_dir=$(cd "$(dirname "$0")" &>/dev/null; pwd -P)
source "$self_dir"/common.sh

newrepo $1 machete-sandbox

gituserdata

newb root
  cmt Root
newb develop
  cmt Develop commit
newb allow-ownership-link
  cmt Allow ownership links
newb build-chain
  cmt Build arbitrarily long chains
git checkout allow-ownership-link
  cmt 1st round of fixes
git checkout develop
  cmt Other develop commit
newb call-ws
  cmt Call web service
  cmt 1st round of fixes
newb drop-constraint # not added to definition file
  cmt Drop unneeded SQL constraints
git checkout call-ws
  cmt 2nd round of fixes

cat >.git/machete <<EOF
develop
    allow-ownership-link PR #123
        build-chain PR #124
    call-ws
EOF

git branch -d root
