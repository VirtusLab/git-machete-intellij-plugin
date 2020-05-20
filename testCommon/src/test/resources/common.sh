#!/usr/bin/env bash

set -x
status_branch_hook=$(cat <<'EOF'
#!/usr/bin/env bash
git log -1 --format=%cd  # commit timestamp of the branch tip
EOF
)

newrepo() {
  if (( $# < 2 )); then
    echo "${FUNCNAME[0]} needs at least 2 parameters, $# was given"
    exit 100
  fi
  local path=$1
  local dir=$2
  mkdir -p $path/$dir
  cd $path/$dir
  local opt=${3-}
  git init $opt
  mkdir -p .git/hooks/
  local hook_path=.git/hooks/machete-status-branch
  echo "$status_branch_hook" > $hook_path
  chmod +x $hook_path
}

clone() {
  if (( $# != 3 )); then
    echo "${FUNCNAME[0]} needs 3 parameters, $# was given"
    exit 100
  fi
  local path=$1
  local remote=$2
  local destinationDir=$3
  cd $path
  git clone $remote $destinationDir
  cd $destinationDir
}

gituserdata() {
  git config --local user.email "circleci@example.com"
  git config --local user.name "CircleCI"
}

newb() {
  if (( $# != 1 )); then
    echo "${FUNCNAME[0]} needs 1 parameter, $# was given"
    exit 100
  fi
  git checkout -b $1
}

cmt() {
  if (( $# < 1 )); then
    echo "${FUNCNAME[0]} needs at least 1 parameter, $# was given"
    exit 100
  fi
  local b=$(git symbolic-ref --short HEAD)
  local f=${b/\//-}-${1}-${2-}.txt
  touch $f
  git add $f
  git commit -m "$*"
}

push() {
  local b=$(git symbolic-ref --short HEAD)
  git push -u ${1-origin} $b
}
