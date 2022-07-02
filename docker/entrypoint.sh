#!/usr/bin/env bash

set -e -o pipefail -u

if [[ $GID && $UID ]]; then
  if ! getent group $GID &>/dev/null; then
    groupadd --gid=$GID docker
  fi
  useradd --create-home --gid=$GID --uid=$UID docker
  sudo --user=docker bash -c "cd && $*"
else
  bash -c "$@"
fi
