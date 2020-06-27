#!/usr/bin/env bash

status_branch_hook=$(cat <<'EOF'
#!/usr/bin/env bash
branch=$1
file_count=$(git ls-tree $branch | wc -l)
echo "<$file_count files>"
EOF
)

# All functions defined here are guaranteed to preserve the original working directory.

newrepo() {
  if (( $# < 1 )); then
    echo "${FUNCNAME[0]} <dir> [<git-init-options>...] needs at least 1 parameter, $# was given"
    exit 100
  fi

  local dir=$1
  mkdir -p $dir
  cd $dir
  shift
  git init $@
  mkdir -p .git/hooks/
  local hook_path=.git/hooks/machete-status-branch
  echo "$status_branch_hook" > $hook_path
  chmod +x $hook_path
  git config --local user.email "circleci@example.com"
  git config --local user.name "CircleCI"
  cd -
}

newb() {
  if (( $# != 1 )); then
    echo "${FUNCNAME[0]} <branch-name> needs 1 parameter, $# was given"
    exit 100
  fi

  git checkout -b $1
}

cmt() {
  if (( $# < 1 )); then
    echo "${FUNCNAME[0]} <commit-message-words...> needs at least 1 parameter, $# was given"
    exit 100
  fi

  local b=$(git symbolic-ref --short HEAD)
  local f=${b/\//-}-$(sed 's/ /-/g' <<< "$@").txt
  touch $f
  git add $f
  git commit -m "$*"
}

push() {
  local b=$(git symbolic-ref --short HEAD)
  git push -u ${1-origin} $b
}
