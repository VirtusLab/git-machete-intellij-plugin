#!/usr/bin/env bash

set -e -o pipefail -u

self_dir=$(cd "$(dirname "$0")" &>/dev/null; pwd -P)
source "$self_dir"/common.sh

create_repo machete-sandbox-remote --bare

create_repo machete-sandbox
(
  cd machete-sandbox
  git remote add origin ../machete-sandbox-remote

  create_branch root
    commit Root
  create_branch develop
    commit Develop commit
    push
  create_branch allow-ownership-link # not added to definition file
    commit Allow ownership links
  create_branch build-chain
    commit Build arbitrarily long chains
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
