#!/usr/bin/env bash

set -e -o pipefail -u

self_dir=$(cd "$(dirname "$0")" &>/dev/null; pwd -P)
source "$self_dir"/common.sh

newrepo $1 machete-sandbox-remote --bare
newrepo $1 machete-sandbox

gituserdata

git remote add origin $1/machete-sandbox-remote

newb root
  cmt Root
newb develop
  cmt Develop commit
  push
newb allow-ownership-link # not added to definition file
  cmt Allow ownership links
newb build-chain
  cmt Build arbitrarily long chains
  push

# Let's skip allow-ownership-link on purpose so that develop and build-chain are connected with a yellow edge
cat >.git/machete <<EOF
develop
    build-chain
EOF

git branch -d root

git config machete.overrideForkPoint.build-chain.to "$(git rev-parse allow-ownership-link)"
git config machete.overrideForkPoint.build-chain.whileDescendantOf "$(git rev-parse build-chain)"
