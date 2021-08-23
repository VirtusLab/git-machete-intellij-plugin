#!/usr/bin/env bash

status_branch_hook=$(cat <<'EOF'
#!/usr/bin/env bash
branch=$1
file_count=$(git ls-tree $branch | wc -l)
echo "<$file_count files>"
EOF
)

# All functions defined here are guaranteed to preserve the original current working directory.

function set_fake_git_date() {
  if (( $# != 1 )); then
    echo "${FUNCNAME[0]} <date> needs 1 parameter, $# was given"
    exit 100
  fi

  local date=$1
  # Note that GIT_COMMITTER_DATE is recorded not only into the commits but also into reflog entries.
  export GIT_COMMITTER_DATE="$date 12:34:56"
}

commit_day_of_month=1

function create_repo() {
  if (( $# < 1 )); then
    echo "${FUNCNAME[0]} <dir> [<git-init-options>...] needs at least 1 parameter, $# was given"
    exit 100
  fi

  set_fake_git_date 2020-01-$commit_day_of_month

  local dir=$1
  mkdir -p $dir
  cd $dir
  shift
  git init $@
 # mkdir -p .git/hooks/
 # local hook_path=.git/hooks/machete-status-branch
 # echo "$status_branch_hook" > $hook_path
 # chmod +x $hook_path
  git config --local user.email "circleci@example.com"
  git config --local user.name "CircleCI"
  cd -
}

function create_branch() {
  if (( $# != 1 )); then
    echo "${FUNCNAME[0]} <branch-name> needs 1 parameter, $# was given"
    exit 100
  fi

  git checkout -b $1
}

function commit() {
  if (( $# < 1 )); then
    echo "${FUNCNAME[0]} <commit-message-words...> needs at least 1 parameter, $# was given"
    exit 100
  fi

  local b=$(git symbolic-ref --short HEAD)
  local f=${b//\//-}-$(sed 's/[ /]/-/g' <<< "$@").txt
  touch $f
  git add $f
  git commit -m "$*"
  set_fake_git_date 2020-01-$((++commit_day_of_month))
}

function push() {
  local b=$(git symbolic-ref --short HEAD)
  git push -u ${1-origin} $b
}
