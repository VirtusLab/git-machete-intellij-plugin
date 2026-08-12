#!/usr/bin/env bash

# Prevent env-var pollution from leaking author/committer identity into commits.
# `git config --local user.name/email` (set in `create_repo` below) is shadowed
# by these env vars whenever they're present, which silently breaks fixture
# determinism (commits get authored by whoever leaked the vars instead of by
# `CircleCI <circleci@example.com>`, producing different SHAs).
# `GIT_*_DATE` are re-exported per commit by `set_fake_git_date`, so it's safe
# (and equally important) to clear them here too.
unset GIT_AUTHOR_NAME GIT_AUTHOR_EMAIL GIT_AUTHOR_DATE \
      GIT_COMMITTER_NAME GIT_COMMITTER_EMAIL GIT_COMMITTER_DATE

status_branch_hook=$(cat <<'EOF'
#!/usr/bin/env bash
branch=$1
# For machete-status-branch hook, let's just use a command whose output would differ between the branches.
file_count=$(git ls-tree $branch | wc -l | sed 's/^ *//')
if [[ $branch = master ]]; then
  # To test handling of failures (both stdout and stderr should be ignored)
  echo Error | tee /dev/stderr
  exit 1
fi
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
  export GIT_COMMITTER_DATE="$date 12:34:56 +0000"
  export GIT_AUTHOR_DATE="$GIT_COMMITTER_DATE"
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
  cd $dir || exit 1
  shift
  git init "$@"
  mkdir -p .git/hooks/
  local hook_path=.git/hooks/machete-status-branch
  echo "$status_branch_hook" > $hook_path
  chmod +x $hook_path
  # `--local` (per-repository) is the default when writing git config... let's put it here for clarity anyway.
  git config --local user.email "circleci@example.com"
  git config --local user.name "CircleCI"
  # There might be a rare case when, on a developer machine,
  # `git commit` by default (as per `git config --global`) requires signing,
  # and signing in turn requires an action from user (like touching a YubiKey).
  # To make sure the tests can run automatically in such scenario,
  # let's disable automatic commit signing on a per-repository level.
  git config --local commit.gpgSign false
  # Override any global core.hooksPath setting to ensure hooks are looked up in .git/hooks
  # (where we create them in tests) rather than a global hooks directory.
  git config --local core.hooksPath .git/hooks
  cd - || exit 1
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

  local b f
  b=$(git symbolic-ref --short HEAD)
  f=${b//\//-}-$(sed 's/[ /]/-/g' <<< "$@").txt
  touch $f
  git add $f
  git commit -m "$*"
  set_fake_git_date 2020-01-$((++commit_day_of_month))
}

function push() {
  local b
  b=$(git symbolic-ref --short HEAD)
  git push -u ${1-origin} $b
}
