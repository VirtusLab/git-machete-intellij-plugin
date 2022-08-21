#!/usr/bin/env bash

set -e -o pipefail -u

if [[ ${GID-} && ${UID-} ]]; then
  if ! getent group $GID &>/dev/null; then
    groupadd --gid=$GID docker
  fi
  useradd --create-home --gid=$GID --uid=$UID docker
  chown $UID:$GID /home/docker/
  sudo --preserve-env --user=docker bash -c "$*"
else
  bash -c "$@"
fi
