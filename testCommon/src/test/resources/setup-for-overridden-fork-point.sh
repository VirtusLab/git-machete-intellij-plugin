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
    push
  newb allow-ownership-link # not added to definition file
    cmt Allow ownership links
  newb build-chain
    cmt Build arbitrarily long chains
    push

  git branch -d root

  git config machete.overrideForkPoint.build-chain.to "$(git rev-parse allow-ownership-link)"
  git config machete.overrideForkPoint.build-chain.whileDescendantOf "$(git rev-parse build-chain)"

  # Let's skip allow-ownership-link on purpose so that develop and build-chain are connected with a yellow edge
  machete_file='
  develop
      build-chain
  '
  sed 's/^  //' <<< "$machete_file" > .git/machete
)
