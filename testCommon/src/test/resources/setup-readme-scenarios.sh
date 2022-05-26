#!/usr/bin/env bash

set -e -o pipefail -u

self_dir=$(cd "$(dirname "$0")" &>/dev/null; pwd -P)
source "$self_dir"/common.sh

sandboxDir=machete-sandbox
mkdir $sandboxDir
cd $sandboxDir

# Review
create_repo scenario-1-remote --bare
create_repo scenario-1
cd scenario-1
git remote add origin ../scenario-1-remote

create_branch master
  commit 'Init'
  push
git checkout master
create_branch sticky-header
  commit 'Add sticky-header'
  push
create_branch fancy-footer
  commit 'Add fancy footer'
  push
git checkout master
create_branch common-scripts
  commit 'Add common-scripts'
  push
git checkout fancy-footer
git branch -d common-scripts

cat >.git/machete <<EOF
master
  sticky-header PR #2
    fancy-footer PR #3
EOF

# Branch update
cd ..
create_repo scenario-2-remote --bare
create_repo scenario-2
cd scenario-2
git remote add origin ../scenario-2-remote

create_branch master
  commit 'Init'
  push
create_branch common-scripts
  commit 'Add common-scripts'
  push
git checkout master
  git merge --ff-only -
  git branch -d common-scripts
  git reset --hard HEAD~1
  git rev-parse HEAD > ../scenario-2/.git/refs/remotes/origin/master
  git rev-parse HEAD
  git rev-parse origin/common-scripts > ../scenario-2-remote/refs/heads/master
create_branch sticky-header
  commit 'Add sticky-header'
  push
create_branch fancy-footer
  commit 'Add fancy footer'
  push
cat >.git/machete <<EOF
master
  sticky-header PR #2
    fancy-footer PR #3
EOF

# Stacker PRs
cd ..
create_repo scenario-3-remote --bare
create_repo scenario-3
cd scenario-3
git remote add origin ../scenario-3-remote

create_branch master
  commit 'Init'
  commit 'Add common-scripts'
  push
create_branch sticky-header
  commit 'Add sticky-header'
  push
create_branch fancy-footer
  commit 'Add fancy footer'
  push
git checkout sticky-header
  commit 'Fix typos'

cat >.git/machete <<'EOF'
master
  sticky-header PR #2
    fancy-footer PR #3
EOF

# Merge (ff)
cd ..
create_repo scenario-4-remote --bare
create_repo scenario-4
cd scenario-4
git remote add origin ../scenario-4-remote

create_branch master
  commit 'Init'
  commit 'Add common-scripts'
 push
create_branch sticky-header
  commit 'Add sticky-header'
  commit 'Fix typos'
  push
create_branch fancy-footer
  commit 'Add fancy footer'
  push

cat >.git/machete <<'EOF'
master
  sticky-header PR #2
    fancy-footer PR #3
EOF
